package com.upstart.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RateLimitingService {

    private final Map<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Configuration
    @Value("${security.rate-limiting.max-attempts:5}")
    private int maxAttempts = 5;
    
    @Value("${security.rate-limiting.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes = 15;
    
    @Value("${security.rate-limiting.cleanup-interval-minutes:30}")
    private int cleanupIntervalMinutes = 30;

    public RateLimitingService() {
        // Clean up expired entries periodically
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 
                cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES);
    }

    /**
     * Check if an IP address is currently rate limited
     * @param ipAddress the client IP address
     * @return true if the IP is rate limited
     */
    public boolean isRateLimited(String ipAddress) {
        AttemptInfo attemptInfo = loginAttempts.get(ipAddress);
        
        if (attemptInfo == null) {
            return false;
        }
        
        // Check if lockout period has expired
        if (attemptInfo.isLocked() && attemptInfo.getLockoutExpiry().isBefore(LocalDateTime.now())) {
            // Lockout expired, reset attempts
            loginAttempts.remove(ipAddress);
            log.info("Rate limit lockout expired for IP: {}", ipAddress);
            return false;
        }
        
        return attemptInfo.isLocked();
    }

    /**
     * Record a failed login attempt
     * @param ipAddress the client IP address
     */
    public void recordFailedAttempt(String ipAddress) {
        AttemptInfo attemptInfo = loginAttempts.computeIfAbsent(ipAddress, k -> new AttemptInfo());
        
        attemptInfo.incrementAttempts();
        
        if (attemptInfo.getAttempts() >= maxAttempts) {
            attemptInfo.setLocked(true);
            attemptInfo.setLockoutExpiry(LocalDateTime.now().plus(lockoutDurationMinutes, ChronoUnit.MINUTES));
            log.warn("IP address {} has been rate limited due to {} failed login attempts", 
                    ipAddress, attemptInfo.getAttempts());
        } else {
            log.info("Failed login attempt {} of {} for IP: {}", 
                    attemptInfo.getAttempts(), maxAttempts, ipAddress);
        }
    }

    /**
     * Record a successful login (resets failed attempts)
     * @param ipAddress the client IP address
     */
    public void recordSuccessfulAttempt(String ipAddress) {
        AttemptInfo removed = loginAttempts.remove(ipAddress);
        if (removed != null && removed.getAttempts() > 0) {
            log.info("Successful login for IP: {} - cleared {} failed attempts", 
                    ipAddress, removed.getAttempts());
        }
    }

    /**
     * Get the number of failed attempts for an IP
     * @param ipAddress the client IP address
     * @return the number of failed attempts
     */
    public int getFailedAttempts(String ipAddress) {
        AttemptInfo attemptInfo = loginAttempts.get(ipAddress);
        return attemptInfo != null ? attemptInfo.getAttempts() : 0;
    }

    /**
     * Get the remaining lockout time in minutes
     * @param ipAddress the client IP address
     * @return remaining lockout time in minutes, or 0 if not locked
     */
    public long getRemainingLockoutMinutes(String ipAddress) {
        AttemptInfo attemptInfo = loginAttempts.get(ipAddress);
        
        if (attemptInfo == null || !attemptInfo.isLocked()) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (attemptInfo.getLockoutExpiry().isBefore(now)) {
            return 0;
        }
        
        return ChronoUnit.MINUTES.between(now, attemptInfo.getLockoutExpiry());
    }

    /**
     * Manually unlock an IP address (admin function)
     * @param ipAddress the client IP address
     */
    public void unlockIpAddress(String ipAddress) {
        AttemptInfo removed = loginAttempts.remove(ipAddress);
        if (removed != null) {
            log.info("Manually unlocked IP address: {}", ipAddress);
        }
    }

    /**
     * Get current rate limiting statistics
     * @return map of IP addresses and their attempt info
     */
    public Map<String, AttemptInfo> getRateLimitingStats() {
        return new ConcurrentHashMap<>(loginAttempts);
    }

    /**
     * Clean up expired entries from the rate limiting cache
     */
    private void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        
        // Use AtomicInteger to avoid lambda variable capture issues
        java.util.concurrent.atomic.AtomicInteger removedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        loginAttempts.entrySet().removeIf(entry -> {
            AttemptInfo info = entry.getValue();
            boolean shouldRemove = info.isLocked() && info.getLockoutExpiry().isBefore(now);
            if (shouldRemove) {
                removedCount.incrementAndGet();
            }
            return shouldRemove;
        });
        
        int finalRemovedCount = removedCount.get();
        if (finalRemovedCount > 0) {
            log.debug("Cleaned up {} expired rate limiting entries", finalRemovedCount);
        }
    }

    /**
     * Shutdown the cleanup scheduler when the bean is destroyed
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RateLimitingService scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Scheduler did not terminate gracefully, forcing shutdown");
                scheduler.shutdownNow();
            } else {
                log.info("RateLimitingService scheduler shut down successfully");
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for scheduler shutdown");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Inner class to track attempt information
     */
    public static class AttemptInfo {
        private int attempts = 0;
        private boolean locked = false;
        private LocalDateTime lockoutExpiry;
        private final LocalDateTime firstAttempt = LocalDateTime.now();

        public void incrementAttempts() {
            this.attempts++;
        }

        public int getAttempts() {
            return attempts;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public LocalDateTime getLockoutExpiry() {
            return lockoutExpiry;
        }

        public void setLockoutExpiry(LocalDateTime lockoutExpiry) {
            this.lockoutExpiry = lockoutExpiry;
        }

        public LocalDateTime getFirstAttempt() {
            return firstAttempt;
        }
    }
}