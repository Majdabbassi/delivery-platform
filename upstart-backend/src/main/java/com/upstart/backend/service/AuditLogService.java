package com.upstart.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuditLogService {

    private final ConcurrentLinkedQueue<AuditEvent> auditQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public AuditLogService() {
        // Process audit events every 30 seconds
        scheduler.scheduleAtFixedRate(this::processAuditEvents, 30, 30, TimeUnit.SECONDS);
    }
    
    public void logSecurityEvent(SecurityEventType eventType, String username, String ipAddress, String details) {
        AuditEvent event = new AuditEvent(
            eventType,
            username,
            ipAddress,
            details,
            LocalDateTime.now()
        );
        
        auditQueue.offer(event);
        
        // Log immediately for critical events
        if (eventType.isCritical()) {
            logEventImmediately(event);
        }
    }
    
    public void logLoginAttempt(String username, String ipAddress, boolean successful) {
        SecurityEventType eventType = successful ? SecurityEventType.LOGIN_SUCCESS : SecurityEventType.LOGIN_FAILURE;
        String details = successful ? "Successful login" : "Failed login attempt";
        logSecurityEvent(eventType, username, ipAddress, details);
    }
    
    public void logLogout(String username, String ipAddress) {
        logSecurityEvent(SecurityEventType.LOGOUT, username, ipAddress, "User logout");
    }
    
    public void logTokenRefresh(String username, String ipAddress) {
        logSecurityEvent(SecurityEventType.TOKEN_REFRESH, username, ipAddress, "Token refreshed");
    }
    
    public void logPasswordChange(String username, String ipAddress) {
        logSecurityEvent(SecurityEventType.PASSWORD_CHANGE, username, ipAddress, "Password changed");
    }
    
    public void logAccountLockout(String username, String ipAddress) {
        logSecurityEvent(SecurityEventType.ACCOUNT_LOCKOUT, username, ipAddress, "Account locked due to multiple failed attempts");
    }
    
    public void logSuspiciousActivity(String username, String ipAddress, String details) {
        logSecurityEvent(SecurityEventType.SUSPICIOUS_ACTIVITY, username, ipAddress, details);
    }
    
    public void logPrivilegeEscalation(String username, String ipAddress, String details) {
        logSecurityEvent(SecurityEventType.PRIVILEGE_ESCALATION, username, ipAddress, details);
    }
    
    public void logDataAccess(String username, String ipAddress, String resource) {
        logSecurityEvent(SecurityEventType.DATA_ACCESS, username, ipAddress, "Accessed: " + resource);
    }
    
    public void logAuthenticationAttempt(String username, boolean successful, String ipAddress) {
        SecurityEventType eventType = successful ? SecurityEventType.LOGIN_SUCCESS : SecurityEventType.LOGIN_FAILURE;
        String details = successful ? "Successful authentication" : "Failed authentication attempt";
        logSecurityEvent(eventType, username, ipAddress, details);
    }
    
    public void logUserRegistration(String username, String ipAddress) {
        logSecurityEvent(SecurityEventType.DATA_ACCESS, username, ipAddress, "User registration: " + username);
    }
    
    private void processAuditEvents() {
        try {
            while (!auditQueue.isEmpty()) {
                AuditEvent event = auditQueue.poll();
                if (event != null) {
                    logEventImmediately(event);
                }
            }
        } catch (Exception e) {
            log.error("Error processing audit events: {}", e.getMessage());
        }
    }
    
    private void logEventImmediately(AuditEvent event) {
        // In a production environment, this would typically write to:
        // - A dedicated audit database
        // - SIEM system
        // - Security monitoring platform
        // - Centralized logging system (ELK stack, Splunk, etc.)
        
        String logMessage = String.format(
            "AUDIT_EVENT: [%s] User: %s, IP: %s, Event: %s, Details: %s, Timestamp: %s",
            event.getEventType().name(),
            event.getUsername() != null ? event.getUsername() : "UNKNOWN",
            event.getIpAddress() != null ? event.getIpAddress() : "UNKNOWN",
            event.getEventType().getDescription(),
            event.getDetails() != null ? event.getDetails() : "No details",
            event.getTimestamp()
        );
        
        if (event.getEventType().isCritical()) {
            log.warn(logMessage);
        } else {
            log.info(logMessage);
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public enum SecurityEventType {
        LOGIN_SUCCESS("Successful login", false),
        LOGIN_FAILURE("Failed login attempt", true),
        LOGOUT("User logout", false),
        TOKEN_REFRESH("Token refresh", false),
        PASSWORD_CHANGE("Password change", false),
        ACCOUNT_LOCKOUT("Account lockout", true),
        SUSPICIOUS_ACTIVITY("Suspicious activity detected", true),
        PRIVILEGE_ESCALATION("Privilege escalation attempt", true),
        DATA_ACCESS("Data access", false),
        UNAUTHORIZED_ACCESS("Unauthorized access attempt", true),
        SECURITY_VIOLATION("Security policy violation", true);
        
        private final String description;
        private final boolean critical;
        
        SecurityEventType(String description, boolean critical) {
            this.description = description;
            this.critical = critical;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isCritical() {
            return critical;
        }
    }
    
    private static class AuditEvent {
        private final SecurityEventType eventType;
        private final String username;
        private final String ipAddress;
        private final String details;
        private final LocalDateTime timestamp;
        
        public AuditEvent(SecurityEventType eventType, String username, String ipAddress, String details, LocalDateTime timestamp) {
            this.eventType = eventType;
            this.username = username;
            this.ipAddress = ipAddress;
            this.details = details;
            this.timestamp = timestamp;
        }
        
        public SecurityEventType getEventType() {
            return eventType;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getIpAddress() {
            return ipAddress;
        }
        
        public String getDetails() {
            return details;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}