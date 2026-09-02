package com.swiftdeliver.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Configuration properties for order assignment algorithms
 * Manages settings for partnership detection, scoring, and assignment logic
 */
@Component
@ConfigurationProperties(prefix = "swiftdeliver.assignment")
@Validated
public class AssignmentConfigurationProperties {

    /**
     * General assignment settings
     */
    private General general = new General();

    /**
     * Partnership-specific settings
     */
    private Partnership partnership = new Partnership();

    /**
     * Scoring algorithm settings
     */
    private Scoring scoring = new Scoring();

    /**
     * Distance calculation settings
     */
    private Distance distance = new Distance();

    /**
     * Retry and fallback settings
     */
    private Retry retry = new Retry();

    /**
     * Performance and optimization settings
     */
    private Performance performance = new Performance();

    // Getters and Setters
    public General getGeneral() { return general; }
    public void setGeneral(General general) { this.general = general; }

    public Partnership getPartnership() { return partnership; }
    public void setPartnership(Partnership partnership) { this.partnership = partnership; }

    public Scoring getScoring() { return scoring; }
    public void setScoring(Scoring scoring) { this.scoring = scoring; }

    public Distance getDistance() { return distance; }
    public void setDistance(Distance distance) { this.distance = distance; }

    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }

    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }

    /**
     * General assignment configuration
     */
    public static class General {
        /**
         * Enable automatic order assignment
         */
        private boolean autoAssignmentEnabled = true;

        /**
         * Prefer partnerships over direct assignment
         */
        private boolean preferPartnerships = true;

        /**
         * Maximum assignment attempts per order
         */
        @Min(1)
        @Max(10)
        private int maxAssignmentAttempts = 3;

        /**
         * Assignment timeout in seconds
         */
        @Min(5)
        @Max(300)
        private int assignmentTimeoutSeconds = 30;

        /**
         * Enable assignment audit logging
         */
        private boolean auditLoggingEnabled = true;

        /**
         * Minimum score threshold for assignment
         */
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal minScoreThreshold = BigDecimal.valueOf(0.3);

        // Getters and Setters
        public boolean isAutoAssignmentEnabled() { return autoAssignmentEnabled; }
        public void setAutoAssignmentEnabled(boolean autoAssignmentEnabled) { this.autoAssignmentEnabled = autoAssignmentEnabled; }

        public boolean isPreferPartnerships() { return preferPartnerships; }
        public void setPreferPartnerships(boolean preferPartnerships) { this.preferPartnerships = preferPartnerships; }

        public int getMaxAssignmentAttempts() { return maxAssignmentAttempts; }
        public void setMaxAssignmentAttempts(int maxAssignmentAttempts) { this.maxAssignmentAttempts = maxAssignmentAttempts; }

        public int getAssignmentTimeoutSeconds() { return assignmentTimeoutSeconds; }
        public void setAssignmentTimeoutSeconds(int assignmentTimeoutSeconds) { this.assignmentTimeoutSeconds = assignmentTimeoutSeconds; }

        public boolean isAuditLoggingEnabled() { return auditLoggingEnabled; }
        public void setAuditLoggingEnabled(boolean auditLoggingEnabled) { this.auditLoggingEnabled = auditLoggingEnabled; }

        public BigDecimal getMinScoreThreshold() { return minScoreThreshold; }
        public void setMinScoreThreshold(BigDecimal minScoreThreshold) { this.minScoreThreshold = minScoreThreshold; }
    }

    /**
     * Partnership-specific configuration
     */
    public static class Partnership {
        /**
         * Enable exclusive partnership priority
         */
        private boolean exclusivePriorityEnabled = true;

        /**
         * Minimum partnership recommendation score
         */
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal minRecommendationScore = BigDecimal.valueOf(0.6);

        /**
         * Maximum partnerships to evaluate per order
         */
        @Min(1)
        @Max(50)
        private int maxPartnershipsToEvaluate = 10;

        /**
         * Partnership conflict detection enabled
         */
        private boolean conflictDetectionEnabled = true;

        /**
         * Days ahead to check for expiring partnerships
         */
        @Min(1)
        @Max(365)
        private int expirationCheckDays = 30;

        /**
         * Minimum orders for partnership performance evaluation
         */
        @Min(1)
        private int minOrdersForPerformanceEval = 10;

        /**
         * Partnership performance evaluation period in days
         */
        @Min(1)
        @Max(365)
        private int performanceEvaluationDays = 90;

        // Getters and Setters
        public boolean isExclusivePriorityEnabled() { return exclusivePriorityEnabled; }
        public void setExclusivePriorityEnabled(boolean exclusivePriorityEnabled) { this.exclusivePriorityEnabled = exclusivePriorityEnabled; }

        public BigDecimal getMinRecommendationScore() { return minRecommendationScore; }
        public void setMinRecommendationScore(BigDecimal minRecommendationScore) { this.minRecommendationScore = minRecommendationScore; }

        public int getMaxPartnershipsToEvaluate() { return maxPartnershipsToEvaluate; }
        public void setMaxPartnershipsToEvaluate(int maxPartnershipsToEvaluate) { this.maxPartnershipsToEvaluate = maxPartnershipsToEvaluate; }

        public boolean isConflictDetectionEnabled() { return conflictDetectionEnabled; }
        public void setConflictDetectionEnabled(boolean conflictDetectionEnabled) { this.conflictDetectionEnabled = conflictDetectionEnabled; }

        public int getExpirationCheckDays() { return expirationCheckDays; }
        public void setExpirationCheckDays(int expirationCheckDays) { this.expirationCheckDays = expirationCheckDays; }

        public int getMinOrdersForPerformanceEval() { return minOrdersForPerformanceEval; }
        public void setMinOrdersForPerformanceEval(int minOrdersForPerformanceEval) { this.minOrdersForPerformanceEval = minOrdersForPerformanceEval; }

        public int getPerformanceEvaluationDays() { return performanceEvaluationDays; }
        public void setPerformanceEvaluationDays(int performanceEvaluationDays) { this.performanceEvaluationDays = performanceEvaluationDays; }
    }

    /**
     * Scoring algorithm configuration
     */
    public static class Scoring {
        /**
         * Partnership scoring weights
         */
        private PartnershipWeights partnershipWeights = new PartnershipWeights();

        /**
         * Delivery company scoring weights
         */
        private DeliveryCompanyWeights deliveryCompanyWeights = new DeliveryCompanyWeights();

        /**
         * Bonus and penalty settings
         */
        private BonusPenalty bonusPenalty = new BonusPenalty();

        // Getters and Setters
        public PartnershipWeights getPartnershipWeights() { return partnershipWeights; }
        public void setPartnershipWeights(PartnershipWeights partnershipWeights) { this.partnershipWeights = partnershipWeights; }

        public DeliveryCompanyWeights getDeliveryCompanyWeights() { return deliveryCompanyWeights; }
        public void setDeliveryCompanyWeights(DeliveryCompanyWeights deliveryCompanyWeights) { this.deliveryCompanyWeights = deliveryCompanyWeights; }

        public BonusPenalty getBonusPenalty() { return bonusPenalty; }
        public void setBonusPenalty(BonusPenalty bonusPenalty) { this.bonusPenalty = bonusPenalty; }

        public static class PartnershipWeights {
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal rating = BigDecimal.valueOf(0.25);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal reliability = BigDecimal.valueOf(0.20);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal cost = BigDecimal.valueOf(0.20);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal speed = BigDecimal.valueOf(0.15);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal capacity = BigDecimal.valueOf(0.10);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal history = BigDecimal.valueOf(0.10);

            // Getters and Setters
            public BigDecimal getRating() { return rating; }
            public void setRating(BigDecimal rating) { this.rating = rating; }

            public BigDecimal getReliability() { return reliability; }
            public void setReliability(BigDecimal reliability) { this.reliability = reliability; }

            public BigDecimal getCost() { return cost; }
            public void setCost(BigDecimal cost) { this.cost = cost; }

            public BigDecimal getSpeed() { return speed; }
            public void setSpeed(BigDecimal speed) { this.speed = speed; }

            public BigDecimal getCapacity() { return capacity; }
            public void setCapacity(BigDecimal capacity) { this.capacity = capacity; }

            public BigDecimal getHistory() { return history; }
            public void setHistory(BigDecimal history) { this.history = history; }
        }

        public static class DeliveryCompanyWeights {
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal rating = BigDecimal.valueOf(0.30);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal availability = BigDecimal.valueOf(0.25);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal cost = BigDecimal.valueOf(0.20);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal distance = BigDecimal.valueOf(0.15);
            
            @DecimalMin("0.0") @DecimalMax("1.0")
            private BigDecimal capacity = BigDecimal.valueOf(0.10);

            // Getters and Setters
            public BigDecimal getRating() { return rating; }
            public void setRating(BigDecimal rating) { this.rating = rating; }

            public BigDecimal getAvailability() { return availability; }
            public void setAvailability(BigDecimal availability) { this.availability = availability; }

            public BigDecimal getCost() { return cost; }
            public void setCost(BigDecimal cost) { this.cost = cost; }

            public BigDecimal getDistance() { return distance; }
            public void setDistance(BigDecimal distance) { this.distance = distance; }

            public BigDecimal getCapacity() { return capacity; }
            public void setCapacity(BigDecimal capacity) { this.capacity = capacity; }
        }

        public static class BonusPenalty {
            @DecimalMin("0.0") @DecimalMax("0.5")
            private BigDecimal exclusivePartnershipBonus = BigDecimal.valueOf(0.1);
            
            @DecimalMin("0.0") @DecimalMax("0.5")
            private BigDecimal longTermPartnershipBonus = BigDecimal.valueOf(0.05);
            
            @DecimalMin("0.0") @DecimalMax("0.5")
            private BigDecimal highValueOrderBonus = BigDecimal.valueOf(0.05);
            
            @DecimalMin("0.0") @DecimalMax("0.5")
            private BigDecimal serviceAreaMatchBonus = BigDecimal.valueOf(0.05);
            
            @DecimalMin("0.0") @DecimalMax("0.5")
            private BigDecimal highRatingBonus = BigDecimal.valueOf(0.05);
            
            @DecimalMin("0.0") @DecimalMax("0.5")
            private BigDecimal inactivityPenalty = BigDecimal.valueOf(0.2);

            // Getters and Setters
            public BigDecimal getExclusivePartnershipBonus() { return exclusivePartnershipBonus; }
            public void setExclusivePartnershipBonus(BigDecimal exclusivePartnershipBonus) { this.exclusivePartnershipBonus = exclusivePartnershipBonus; }

            public BigDecimal getLongTermPartnershipBonus() { return longTermPartnershipBonus; }
            public void setLongTermPartnershipBonus(BigDecimal longTermPartnershipBonus) { this.longTermPartnershipBonus = longTermPartnershipBonus; }

            public BigDecimal getHighValueOrderBonus() { return highValueOrderBonus; }
            public void setHighValueOrderBonus(BigDecimal highValueOrderBonus) { this.highValueOrderBonus = highValueOrderBonus; }

            public BigDecimal getServiceAreaMatchBonus() { return serviceAreaMatchBonus; }
            public void setServiceAreaMatchBonus(BigDecimal serviceAreaMatchBonus) { this.serviceAreaMatchBonus = serviceAreaMatchBonus; }

            public BigDecimal getHighRatingBonus() { return highRatingBonus; }
            public void setHighRatingBonus(BigDecimal highRatingBonus) { this.highRatingBonus = highRatingBonus; }

            public BigDecimal getInactivityPenalty() { return inactivityPenalty; }
            public void setInactivityPenalty(BigDecimal inactivityPenalty) { this.inactivityPenalty = inactivityPenalty; }
        }
    }

    /**
     * Distance calculation configuration
     */
    public static class Distance {
        /**
         * Maximum delivery distance in kilometers
         */
        @Min(1)
        @Max(1000)
        private int maxDeliveryDistanceKm = 50;

        /**
         * Default area type for speed calculations
         */
        private String defaultAreaType = "suburban";

        /**
         * Speed settings for different area types (km/h)
         */
        private Map<String, Integer> areaTypeSpeeds = Map.of(
            "urban", 25,
            "suburban", 40,
            "highway", 60
        );

        /**
         * Base delivery time in minutes
         */
        @Min(5)
        @Max(120)
        private int baseDeliveryTimeMinutes = 15;

        /**
         * Distance calculation multiplier for driving vs straight-line
         */
        @DecimalMin("1.0")
        @DecimalMax("3.0")
        private BigDecimal drivingDistanceMultiplier = BigDecimal.valueOf(1.3);

        // Getters and Setters
        public int getMaxDeliveryDistanceKm() { return maxDeliveryDistanceKm; }
        public void setMaxDeliveryDistanceKm(int maxDeliveryDistanceKm) { this.maxDeliveryDistanceKm = maxDeliveryDistanceKm; }

        public String getDefaultAreaType() { return defaultAreaType; }
        public void setDefaultAreaType(String defaultAreaType) { this.defaultAreaType = defaultAreaType; }

        public Map<String, Integer> getAreaTypeSpeeds() { return areaTypeSpeeds; }
        public void setAreaTypeSpeeds(Map<String, Integer> areaTypeSpeeds) { this.areaTypeSpeeds = areaTypeSpeeds; }

        public int getBaseDeliveryTimeMinutes() { return baseDeliveryTimeMinutes; }
        public void setBaseDeliveryTimeMinutes(int baseDeliveryTimeMinutes) { this.baseDeliveryTimeMinutes = baseDeliveryTimeMinutes; }

        public BigDecimal getDrivingDistanceMultiplier() { return drivingDistanceMultiplier; }
        public void setDrivingDistanceMultiplier(BigDecimal drivingDistanceMultiplier) { this.drivingDistanceMultiplier = drivingDistanceMultiplier; }
    }

    /**
     * Retry and fallback configuration
     */
    public static class Retry {
        /**
         * Enable automatic retry on assignment failure
         */
        private boolean autoRetryEnabled = true;

        /**
         * Maximum retry attempts
         */
        @Min(0)
        @Max(5)
        private int maxRetryAttempts = 2;

        /**
         * Retry delay in seconds
         */
        @Min(1)
        @Max(300)
        private int retryDelaySeconds = 5;

        /**
         * Enable fallback to lower-scored options
         */
        private boolean fallbackEnabled = true;

        /**
         * Fallback score reduction factor
         */
        @DecimalMin("0.1")
        @DecimalMax("0.9")
        private BigDecimal fallbackScoreReduction = BigDecimal.valueOf(0.2);

        // Getters and Setters
        public boolean isAutoRetryEnabled() { return autoRetryEnabled; }
        public void setAutoRetryEnabled(boolean autoRetryEnabled) { this.autoRetryEnabled = autoRetryEnabled; }

        public int getMaxRetryAttempts() { return maxRetryAttempts; }
        public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }

        public int getRetryDelaySeconds() { return retryDelaySeconds; }
        public void setRetryDelaySeconds(int retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }

        public boolean isFallbackEnabled() { return fallbackEnabled; }
        public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }

        public BigDecimal getFallbackScoreReduction() { return fallbackScoreReduction; }
        public void setFallbackScoreReduction(BigDecimal fallbackScoreReduction) { this.fallbackScoreReduction = fallbackScoreReduction; }
    }

    /**
     * Performance and optimization configuration
     */
    public static class Performance {
        /**
         * Enable caching for assignment calculations
         */
        private boolean cachingEnabled = true;

        /**
         * Cache TTL in minutes
         */
        @Min(1)
        @Max(1440)
        private int cacheTtlMinutes = 30;

        /**
         * Enable parallel processing for scoring
         */
        private boolean parallelProcessingEnabled = true;

        /**
         * Maximum thread pool size for parallel processing
         */
        @Min(1)
        @Max(20)
        private int maxThreadPoolSize = 5;

        /**
         * Enable assignment metrics collection
         */
        private boolean metricsEnabled = true;

        /**
         * Batch size for bulk assignment operations
         */
        @Min(1)
        @Max(1000)
        private int batchSize = 50;

        // Getters and Setters
        public boolean isCachingEnabled() { return cachingEnabled; }
        public void setCachingEnabled(boolean cachingEnabled) { this.cachingEnabled = cachingEnabled; }

        public int getCacheTtlMinutes() { return cacheTtlMinutes; }
        public void setCacheTtlMinutes(int cacheTtlMinutes) { this.cacheTtlMinutes = cacheTtlMinutes; }

        public boolean isParallelProcessingEnabled() { return parallelProcessingEnabled; }
        public void setParallelProcessingEnabled(boolean parallelProcessingEnabled) { this.parallelProcessingEnabled = parallelProcessingEnabled; }

        public int getMaxThreadPoolSize() { return maxThreadPoolSize; }
        public void setMaxThreadPoolSize(int maxThreadPoolSize) { this.maxThreadPoolSize = maxThreadPoolSize; }

        public boolean isMetricsEnabled() { return metricsEnabled; }
        public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }
}