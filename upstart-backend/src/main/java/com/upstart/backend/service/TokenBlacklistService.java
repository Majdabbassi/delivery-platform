package com.upstart.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class TokenBlacklistService {

    // Store token ID with its expiration time
    private final Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Metrics
    private final AtomicLong totalBlacklistedTokens = new AtomicLong(0);
    private final AtomicLong cleanupOperations = new AtomicLong(0);
    
    @Value("${jwt.blacklist.cleanup.interval:1}")
    private long cleanupIntervalHours;
    
    @Value("${jwt.blacklist.max.size:10000}")
    private int maxBlacklistSize;
    
    /**
     * Token blacklist entry to store expiration information
     */
    public static class BlacklistEntry {
        private final String tokenId;
        private final LocalDateTime expirationTime;
        private final LocalDateTime blacklistedAt;
        
        public BlacklistEntry(String tokenId, LocalDateTime expirationTime) {
            this.tokenId = tokenId;
            this.expirationTime = expirationTime;
            this.blacklistedAt = LocalDateTime.now();
        }
        
        public String getTokenId() { return tokenId; }
        public LocalDateTime getExpirationTime() { return expirationTime; }
        public LocalDateTime getBlacklistedAt() { return blacklistedAt; }
        public boolean isExpired() { return LocalDateTime.now().isAfter(expirationTime); }
    }

    public TokenBlacklistService() {
        // Constructor - Spring will inject @Value fields after this
        log.debug("TokenBlacklistService constructor called");
    }

    /**
     * Initialize the service after Spring has injected all dependencies
     */
    @PostConstruct
    public void init() {
        // Clean up expired tokens based on configured interval
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 
            cleanupIntervalHours, cleanupIntervalHours, TimeUnit.HOURS);
        log.info("TokenBlacklistService initialized with cleanup interval: {} hours, max size: {}", 
            cleanupIntervalHours, maxBlacklistSize);
    }

    /**
     * Add a token to the blacklist with expiration time
     * @param tokenId the unique token ID (jti claim)
     * @param expirationTime when the token expires
     */
    public void blacklistToken(String tokenId, LocalDateTime expirationTime) {
        if (tokenId != null && !tokenId.trim().isEmpty() && expirationTime != null) {
            // Check if blacklist is approaching max size
            if (blacklistedTokens.size() >= maxBlacklistSize) {
                log.warn("Blacklist approaching max size ({}), triggering cleanup", maxBlacklistSize);
                cleanupExpiredTokens();
            }
            
            blacklistedTokens.put(tokenId, expirationTime);
            totalBlacklistedTokens.incrementAndGet();
            log.info("Token blacklisted: {} (expires: {})", tokenId, expirationTime);
        }
    }

    /**
     * Add a token to the blacklist (legacy method for backward compatibility)
     * @param tokenId the unique token ID (jti claim)
     */
    public void blacklistToken(String tokenId) {
        // Default to 24 hours from now if no expiration provided
        blacklistToken(tokenId, LocalDateTime.now().plusHours(24));
    }
    
    /**
     * Add a token to the blacklist with Date expiration time (for backward compatibility)
     * @param tokenId the unique token ID (jti claim)
     * @param expirationTime when the token expires (Date)
     */
    public void blacklistToken(String tokenId, java.util.Date expirationTime) {
        if (expirationTime != null) {
            LocalDateTime localDateTime = expirationTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
            blacklistToken(tokenId, localDateTime);
        } else {
            blacklistToken(tokenId);
        }
    }

    /**
     * Check if a token is blacklisted and not expired
     * @param tokenId the unique token ID (jti claim)
     * @return true if the token is blacklisted and not expired
     */
    public boolean isTokenBlacklisted(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        
        LocalDateTime expirationTime = blacklistedTokens.get(tokenId);
        if (expirationTime == null) {
            return false;
        }
        
        // If token has expired, remove it and return false
        if (LocalDateTime.now().isAfter(expirationTime)) {
            blacklistedTokens.remove(tokenId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Alias method for backward compatibility
     * @param tokenId the unique token ID (jti claim)
     * @return true if the token is blacklisted and not expired
     */
    public boolean isBlacklisted(String tokenId) {
        return isTokenBlacklisted(tokenId);
    }

    /**
     * Remove a token from the blacklist (for cleanup purposes)
     * @param tokenId the unique token ID (jti claim)
     */
    public void removeFromBlacklist(String tokenId) {
        if (tokenId != null) {
            blacklistedTokens.remove(tokenId);
            log.debug("Token removed from blacklist: {}", tokenId);
        }
    }

    /**
     * Get the current size of the blacklist
     * @return the number of blacklisted tokens
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }

    /**
     * Get total number of tokens that have been blacklisted (metric)
     * @return total blacklisted tokens count
     */
    public long getTotalBlacklistedTokens() {
        return totalBlacklistedTokens.get();
    }

    /**
     * Get number of cleanup operations performed (metric)
     * @return cleanup operations count
     */
    public long getCleanupOperations() {
        return cleanupOperations.get();
    }

    /**
     * Clear all blacklisted tokens (use with caution)
     */
    public void clearBlacklist() {
        int size = blacklistedTokens.size();
        blacklistedTokens.clear();
        log.warn("Token blacklist cleared - {} tokens removed", size);
    }

    /**
     * Cleanup method to remove expired tokens from blacklist
     * This method removes all tokens that have passed their expiration time
     */
    public void cleanupExpiredTokens() {
        long startTime = System.currentTimeMillis();
        int initialSize = blacklistedTokens.size();
        
        log.debug("Starting cleanup of expired tokens. Current blacklist size: {}", initialSize);
        
        LocalDateTime now = LocalDateTime.now();
        int removedCount = 0;
        
        // Remove expired tokens
        var iterator = blacklistedTokens.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now.isAfter(entry.getValue())) {
                iterator.remove();
                removedCount++;
            }
        }
        
        cleanupOperations.incrementAndGet();
        long duration = System.currentTimeMillis() - startTime;
        
        if (removedCount > 0) {
            log.info("Cleanup completed: removed {} expired tokens in {}ms. Blacklist size: {} -> {}", 
                removedCount, duration, initialSize, blacklistedTokens.size());
        } else {
            log.debug("Cleanup completed: no expired tokens found in {}ms. Current size: {}", 
                duration, blacklistedTokens.size());
        }
    }

    /**
     * Force cleanup of expired tokens (can be called manually)
     * @return number of tokens removed
     */
    public int forceCleanup() {
        int sizeBefore = blacklistedTokens.size();
        cleanupExpiredTokens();
        return sizeBefore - blacklistedTokens.size();
    }

    /**
     * Get blacklist statistics for monitoring
     * @return map containing various metrics
     */
    public Map<String, Object> getBlacklistStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("currentSize", blacklistedTokens.size());
        stats.put("maxSize", maxBlacklistSize);
        stats.put("totalBlacklisted", totalBlacklistedTokens.get());
        stats.put("cleanupOperations", cleanupOperations.get());
        stats.put("cleanupIntervalHours", cleanupIntervalHours);
        
        // Calculate utilization percentage
        double utilization = (double) blacklistedTokens.size() / maxBlacklistSize * 100;
        stats.put("utilizationPercent", Math.round(utilization * 100.0) / 100.0);
        
        return stats;
    }

    /**
     * Shutdown the cleanup scheduler
     * This method is automatically called when the Spring context is destroyed
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TokenBlacklistService scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Scheduler did not terminate gracefully, forcing shutdown");
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Scheduler did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for scheduler shutdown");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("TokenBlacklistService shutdown completed");
    }
    
    /**
     * Alias for shutdown method for backward compatibility
     */
    public void destroy() {
        shutdown();
    }
}