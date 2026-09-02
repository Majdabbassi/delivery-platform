package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.entity.Partnership;
import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.DriverPerson;
import com.swiftdeliver.backend.config.AssignmentConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for auditing and logging order assignment decisions
 * Tracks assignment performance, decisions, and provides analytics
 */
@Service
public class AssignmentAuditService {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentAuditService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("ASSIGNMENT_AUDIT");

    @Autowired
    private AssignmentConfigurationProperties config;

    // In-memory metrics (in production, consider using a proper metrics store)
    private final Map<String, AtomicLong> assignmentMetrics = new ConcurrentHashMap<>();
    private final Map<String, AssignmentDecision> recentDecisions = new ConcurrentHashMap<>();

    /**
     * Log successful partnership assignment
     */
    public void logPartnershipAssignment(Order order, Partnership partnership, 
                                        BigDecimal score, String reason) {
        if (!config.getGeneral().isAuditLoggingEnabled()) {
            return;
        }

        AssignmentDecision decision = new AssignmentDecision(
            order.getId(),
            AssignmentType.PARTNERSHIP,
            partnership.getId(),
            partnership.getVendorCompany().getId(),
            partnership.getDeliveryCompany().getId(),
            null, // No specific driver yet
            score,
            reason,
            AssignmentStatus.SUCCESS,
            LocalDateTime.now(),
            null
        );

        logDecision(decision);
        updateMetrics("partnership_assignments", 1);
        updateMetrics("successful_assignments", 1);

        recentDecisions.put(order.getId().toString(), decision);
    }

    /**
     * Log successful direct delivery company assignment
     */
    public void logDirectAssignment(Order order, DeliveryCompany deliveryCompany, 
                                  DriverPerson driver, BigDecimal score, String reason) {
        if (!config.getGeneral().isAuditLoggingEnabled()) {
            return;
        }

        AssignmentDecision decision = new AssignmentDecision(
            order.getId(),
            AssignmentType.DIRECT,
            null, // No partnership
            order.getVendorCompany().getId(),
            deliveryCompany.getId(),
            driver != null ? driver.getId() : null,
            score,
            reason,
            AssignmentStatus.SUCCESS,
            LocalDateTime.now(),
            null
        );

        logDecision(decision);
        updateMetrics("direct_assignments", 1);
        updateMetrics("successful_assignments", 1);

        recentDecisions.put(order.getId().toString(), decision);
    }

    /**
     * Log assignment failure
     */
    public void logAssignmentFailure(Order order, String reason, String errorDetails) {
        if (!config.getGeneral().isAuditLoggingEnabled()) {
            return;
        }

        AssignmentDecision decision = new AssignmentDecision(
            order.getId(),
            AssignmentType.FAILED,
            null,
            order.getVendorCompany().getId(),
            null,
            null,
            BigDecimal.ZERO,
            reason,
            AssignmentStatus.FAILED,
            LocalDateTime.now(),
            errorDetails
        );

        logDecision(decision);
        updateMetrics("failed_assignments", 1);

        recentDecisions.put(order.getId().toString(), decision);
    }

    /**
     * Log assignment retry attempt
     */
    public void logRetryAttempt(Order order, int attemptNumber, String reason) {
        if (!config.getGeneral().isAuditLoggingEnabled()) {
            return;
        }

        auditLogger.info("Assignment retry attempt {} for order {}: {}", 
                        attemptNumber, order.getId(), reason);
        
        updateMetrics("retry_attempts", 1);
        updateMetrics("retry_attempt_" + attemptNumber, 1);
    }

    /**
     * Log assignment performance metrics
     */
    public void logPerformanceMetrics(Order order, long assignmentTimeMs, 
                                    int partnershipsEvaluated, int companiesEvaluated) {
        if (!config.getGeneral().isAuditLoggingEnabled()) {
            return;
        }

        auditLogger.info("Assignment performance for order {}: {}ms, {} partnerships evaluated, {} companies evaluated",
                        order.getId(), assignmentTimeMs, partnershipsEvaluated, companiesEvaluated);
        
        updateMetrics("total_assignment_time_ms", assignmentTimeMs);
        updateMetrics("partnerships_evaluated", partnershipsEvaluated);
        updateMetrics("companies_evaluated", companiesEvaluated);
    }

    /**
     * Log scoring details for debugging
     */
    public void logScoringDetails(String entityType, Long entityId, 
                                Map<String, BigDecimal> scoreBreakdown, BigDecimal finalScore) {
        if (!config.getGeneral().isAuditLoggingEnabled()) {
            return;
        }

        logger.debug("Scoring details for {} {}: breakdown={}, final={}", 
                    entityType, entityId, scoreBreakdown, finalScore);
    }

    /**
     * Get assignment statistics
     */
    public AssignmentStatistics getAssignmentStatistics() {
        long totalAssignments = getMetricValue("successful_assignments");
        long partnershipAssignments = getMetricValue("partnership_assignments");
        long directAssignments = getMetricValue("direct_assignments");
        long failedAssignments = getMetricValue("failed_assignments");
        long retryAttempts = getMetricValue("retry_attempts");
        long totalTimeMs = getMetricValue("total_assignment_time_ms");
        long partnershipsEvaluated = getMetricValue("partnerships_evaluated");
        long companiesEvaluated = getMetricValue("companies_evaluated");

        double successRate = totalAssignments > 0 ? 
            (double) totalAssignments / (totalAssignments + failedAssignments) * 100 : 0;
        
        double partnershipRate = totalAssignments > 0 ? 
            (double) partnershipAssignments / totalAssignments * 100 : 0;
        
        double averageTimeMs = totalAssignments > 0 ? 
            (double) totalTimeMs / totalAssignments : 0;

        return new AssignmentStatistics(
            totalAssignments,
            partnershipAssignments,
            directAssignments,
            failedAssignments,
            retryAttempts,
            successRate,
            partnershipRate,
            averageTimeMs,
            partnershipsEvaluated,
            companiesEvaluated
        );
    }

    /**
     * Get recent assignment decisions
     */
    public List<AssignmentDecision> getRecentDecisions(int limit) {
        return recentDecisions.values().stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }

    /**
     * Get assignment decision for specific order
     */
    public AssignmentDecision getAssignmentDecision(Long orderId) {
        return recentDecisions.get(orderId.toString());
    }

    /**
     * Clear old audit data (for maintenance)
     */
    public void clearOldAuditData(LocalDateTime before) {
        recentDecisions.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(before));
        
        logger.info("Cleared audit data before {}", before);
    }

    /**
     * Reset metrics (for testing or maintenance)
     */
    public void resetMetrics() {
        assignmentMetrics.clear();
        recentDecisions.clear();
        logger.info("Assignment metrics and decisions cleared");
    }

    // Private helper methods

    private void logDecision(AssignmentDecision decision) {
        auditLogger.info("Assignment decision: {}", decision.toString());
    }

    private void updateMetrics(String metricName, long value) {
        assignmentMetrics.computeIfAbsent(metricName, k -> new AtomicLong(0))
                         .addAndGet(value);
    }

    private long getMetricValue(String metricName) {
        return assignmentMetrics.getOrDefault(metricName, new AtomicLong(0)).get();
    }

    // Inner classes for data structures

    public enum AssignmentType {
        PARTNERSHIP, DIRECT, FAILED
    }

    public enum AssignmentStatus {
        SUCCESS, FAILED, RETRY
    }

    public static class AssignmentDecision {
        private final Long orderId;
        private final AssignmentType type;
        private final Long partnershipId;
        private final Long vendorCompanyId;
        private final Long deliveryCompanyId;
        private final Long driverId;
        private final BigDecimal score;
        private final String reason;
        private final AssignmentStatus status;
        private final LocalDateTime timestamp;
        private final String errorDetails;

        public AssignmentDecision(Long orderId, AssignmentType type, Long partnershipId,
                                Long vendorCompanyId, Long deliveryCompanyId, Long driverId,
                                BigDecimal score, String reason, AssignmentStatus status,
                                LocalDateTime timestamp, String errorDetails) {
            this.orderId = orderId;
            this.type = type;
            this.partnershipId = partnershipId;
            this.vendorCompanyId = vendorCompanyId;
            this.deliveryCompanyId = deliveryCompanyId;
            this.driverId = driverId;
            this.score = score;
            this.reason = reason;
            this.status = status;
            this.timestamp = timestamp;
            this.errorDetails = errorDetails;
        }

        // Getters
        public Long getOrderId() { return orderId; }
        public AssignmentType getType() { return type; }
        public Long getPartnershipId() { return partnershipId; }
        public Long getVendorCompanyId() { return vendorCompanyId; }
        public Long getDeliveryCompanyId() { return deliveryCompanyId; }
        public Long getDriverId() { return driverId; }
        public BigDecimal getScore() { return score; }
        public String getReason() { return reason; }
        public AssignmentStatus getStatus() { return status; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getErrorDetails() { return errorDetails; }

        @Override
        public String toString() {
            return String.format(
                "AssignmentDecision{orderId=%d, type=%s, partnershipId=%s, vendorCompanyId=%d, " +
                "deliveryCompanyId=%s, driverId=%s, score=%s, reason='%s', status=%s, timestamp=%s}",
                orderId, type, partnershipId, vendorCompanyId, deliveryCompanyId, 
                driverId, score, reason, status, timestamp
            );
        }
    }

    public static class AssignmentStatistics {
        private final long totalAssignments;
        private final long partnershipAssignments;
        private final long directAssignments;
        private final long failedAssignments;
        private final long retryAttempts;
        private final double successRate;
        private final double partnershipRate;
        private final double averageAssignmentTimeMs;
        private final long totalPartnershipsEvaluated;
        private final long totalCompaniesEvaluated;

        public AssignmentStatistics(long totalAssignments, long partnershipAssignments,
                                  long directAssignments, long failedAssignments,
                                  long retryAttempts, double successRate, double partnershipRate,
                                  double averageAssignmentTimeMs, long totalPartnershipsEvaluated,
                                  long totalCompaniesEvaluated) {
            this.totalAssignments = totalAssignments;
            this.partnershipAssignments = partnershipAssignments;
            this.directAssignments = directAssignments;
            this.failedAssignments = failedAssignments;
            this.retryAttempts = retryAttempts;
            this.successRate = successRate;
            this.partnershipRate = partnershipRate;
            this.averageAssignmentTimeMs = averageAssignmentTimeMs;
            this.totalPartnershipsEvaluated = totalPartnershipsEvaluated;
            this.totalCompaniesEvaluated = totalCompaniesEvaluated;
        }

        // Getters
        public long getTotalAssignments() { return totalAssignments; }
        public long getPartnershipAssignments() { return partnershipAssignments; }
        public long getDirectAssignments() { return directAssignments; }
        public long getFailedAssignments() { return failedAssignments; }
        public long getRetryAttempts() { return retryAttempts; }
        public double getSuccessRate() { return successRate; }
        public double getPartnershipRate() { return partnershipRate; }
        public double getAverageAssignmentTimeMs() { return averageAssignmentTimeMs; }
        public long getTotalPartnershipsEvaluated() { return totalPartnershipsEvaluated; }
        public long getTotalCompaniesEvaluated() { return totalCompaniesEvaluated; }

        @Override
        public String toString() {
            return String.format(
                "AssignmentStatistics{total=%d, partnership=%d, direct=%d, failed=%d, " +
                "retries=%d, successRate=%.2f%%, partnershipRate=%.2f%%, avgTime=%.2fms}",
                totalAssignments, partnershipAssignments, directAssignments, failedAssignments,
                retryAttempts, successRate, partnershipRate, averageAssignmentTimeMs
            );
        }
    }
}