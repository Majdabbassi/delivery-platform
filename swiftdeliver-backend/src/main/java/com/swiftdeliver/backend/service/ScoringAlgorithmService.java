package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for scoring and ranking partnerships and delivery companies
 * Implements various algorithms for optimal order assignment
 */
@Service
public class ScoringAlgorithmService {

    private static final Logger logger = LoggerFactory.getLogger(ScoringAlgorithmService.class);

    private final DeliveryCompanyService deliveryCompanyService;

    // Scoring weights for partnerships
    @Value("${swiftdeliver.assignment.scoring.partnership-weights.rating:0.25}")
    private double partnershipRatingWeight;
    @Value("${swiftdeliver.assignment.scoring.partnership-weights.reliability:0.20}")
    private double partnershipReliabilityWeight;
    @Value("${swiftdeliver.assignment.scoring.partnership-weights.cost:0.20}")
    private double partnershipCostWeight;
    @Value("${swiftdeliver.assignment.scoring.partnership-weights.speed:0.15}")
    private double partnershipSpeedWeight;
    @Value("${swiftdeliver.assignment.scoring.partnership-weights.capacity:0.10}")
    private double partnershipCapacityWeight;
    @Value("${swiftdeliver.assignment.scoring.partnership-weights.history:0.10}")
    private double partnershipHistoryWeight;

    // Scoring weights for delivery companies
    @Value("${swiftdeliver.assignment.scoring.delivery-company-weights.rating:0.30}")
    private double companyRatingWeight;
    @Value("${swiftdeliver.assignment.scoring.delivery-company-weights.availability:0.25}")
    private double companyAvailabilityWeight;
    @Value("${swiftdeliver.assignment.scoring.delivery-company-weights.cost:0.20}")
    private double companyCostWeight;
    @Value("${swiftdeliver.assignment.scoring.delivery-company-weights.distance:0.15}")
    private double companyDistanceWeight;
    @Value("${swiftdeliver.assignment.scoring.delivery-company-weights.capacity:0.10}")
    private double companyCapacityWeight;

    public ScoringAlgorithmService(DeliveryCompanyService deliveryCompanyService) {
        this.deliveryCompanyService = deliveryCompanyService;
    }

    /**
     * Score and rank partnerships for an order
     */
    public List<PartnershipScore> scorePartnerships(List<Partnership> partnerships, Order order) {
        logger.debug("Scoring {} partnerships for order {}", partnerships.size(), order.getId());

        return partnerships.stream()
                .map(partnership -> calculatePartnershipScore(partnership, order))
                .sorted((s1, s2) -> Double.compare(s2.getTotalScore(), s1.getTotalScore()))
                .collect(Collectors.toList());
    }

    /**
     * Score and rank delivery companies for an order
     */
    public List<DeliveryCompanyScore> scoreDeliveryCompanies(List<DeliveryCompany> companies, Order order) {
        logger.debug("Scoring {} delivery companies for order {}", companies.size(), order.getId());

        return companies.stream()
                .map(company -> calculateDeliveryCompanyScore(company, order))
                .sorted((s1, s2) -> Double.compare(s2.getTotalScore(), s1.getTotalScore()))
                .collect(Collectors.toList());
    }

    /**
     * Calculate comprehensive score for a partnership
     */
    private PartnershipScore calculatePartnershipScore(Partnership partnership, Order order) {
        Map<String, Double> scores = new HashMap<>();
        
        // Rating score (0-1)
        double ratingScore = calculateRatingScore(partnership.getAverageRating());
        scores.put("rating", ratingScore);

        // Reliability score (0-1)
        double reliabilityScore = calculateReliabilityScore(partnership);
        scores.put("reliability", reliabilityScore);

        // Cost efficiency score (0-1)
        double costScore = calculateCostScore(partnership, order);
        scores.put("cost", costScore);

        // Speed/Performance score (0-1)
        double speedScore = calculateSpeedScore(partnership);
        scores.put("speed", speedScore);

        // Capacity score (0-1)
        double capacityScore = calculateCapacityScore(partnership.getDeliveryCompany());
        scores.put("capacity", capacityScore);

        // Historical performance score (0-1)
        double historyScore = calculateHistoryScore(partnership, order.getVendorCompany());
        scores.put("history", historyScore);

        // Calculate weighted total score
        double totalScore = 
                ratingScore * partnershipRatingWeight +
                reliabilityScore * partnershipReliabilityWeight +
                costScore * partnershipCostWeight +
                speedScore * partnershipSpeedWeight +
                capacityScore * partnershipCapacityWeight +
                historyScore * partnershipHistoryWeight;

        // Apply bonuses and penalties
        totalScore = applyPartnershipBonuses(totalScore, partnership, order);

        return new PartnershipScore(partnership, totalScore, scores);
    }

    /**
     * Calculate comprehensive score for a delivery company
     */
    private DeliveryCompanyScore calculateDeliveryCompanyScore(DeliveryCompany company, Order order) {
        Map<String, Double> scores = new HashMap<>();
        
        // Rating score (0-1)
        double ratingScore = calculateRatingScore(company.getRating());
        scores.put("rating", ratingScore);

        // Availability score (0-1)
        double availabilityScore = calculateAvailabilityScore(company);
        scores.put("availability", availabilityScore);

        // Cost score (0-1)
        double costScore = calculateDirectCostScore(company, order);
        scores.put("cost", costScore);

        // Distance/Location score (0-1)
        double distanceScore = calculateDistanceScore(company, order);
        scores.put("distance", distanceScore);

        // Capacity score (0-1)
        double capacityScore = calculateCapacityScore(company);
        scores.put("capacity", capacityScore);

        // Calculate weighted total score
        double totalScore = 
                ratingScore * companyRatingWeight +
                availabilityScore * companyAvailabilityWeight +
                costScore * companyCostWeight +
                distanceScore * companyDistanceWeight +
                capacityScore * companyCapacityWeight;

        // Apply bonuses and penalties
        totalScore = applyCompanyBonuses(totalScore, company, order);

        return new DeliveryCompanyScore(company, totalScore, scores);
    }

    /**
     * Calculate rating score (0-1)
     */
    private double calculateRatingScore(BigDecimal rating) {
        if (rating == null) {
            return 0.5; // Neutral score for no rating
        }
        return Math.min(rating.doubleValue() / 5.0, 1.0);
    }

    /**
     * Calculate reliability score based on partnership performance
     */
    private double calculateReliabilityScore(Partnership partnership) {
        double score = 0.5; // Base score
        
        // Success rate factor
        if (partnership.getTotalOrdersCompleted() != null && partnership.getTotalOrdersCompleted() > 0) {
            double successRate = 0.9;
            score = successRate;
        }
        
        // Consistency factor (based on regular activity)
        if (partnership.getContractEndDate() != null) {
            long daysSinceLastOrder = ChronoUnit.DAYS.between(
                    partnership.getContractEndDate(), LocalDateTime.now());
            
            if (daysSinceLastOrder <= 7) {
                score += 0.1; // Recent activity bonus
            } else if (daysSinceLastOrder > 30) {
                score -= 0.2; // Inactivity penalty
            }
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Calculate cost efficiency score for partnership
     */
    private double calculateCostScore(Partnership partnership, Order order) {
        BigDecimal commissionRate = partnership.getCommissionRate();
        if (commissionRate == null) {
            return 0.5;
        }
        
        // Lower commission rate = higher score
        double rate = commissionRate.doubleValue();
        double normalizedRate = Math.min(rate / 0.10, 1.0);
        
        return 1.0 - normalizedRate;
    }

    /**
     * Calculate direct cost score for delivery company
     */
    private double calculateDirectCostScore(DeliveryCompany company, Order order) {
        BigDecimal commissionRate = company.getCommissionRate();
        if (commissionRate == null) {
            return 0.5;
        }
        
        // Lower commission rate = higher score
        double rate = commissionRate.doubleValue();
        double normalizedRate = Math.min(rate / 0.15, 1.0);
        
        return 1.0 - normalizedRate;
    }

    /**
     * Calculate speed/performance score
     */
    private double calculateSpeedScore(Partnership partnership) {
        double score = 0.5; // Base score

        DeliveryCompany company = partnership.getDeliveryCompany();
        
        if (company.getTotalDeliveriesManaged() > 0) {
            double avgDeliveryHours = 24.0;
            score = Math.max(0.1, 1.0 - (avgDeliveryHours / 48.0));
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Calculate capacity score
     */
    private double calculateCapacityScore(DeliveryCompany company) {
        int availableDrivers = deliveryCompanyService.getAvailableDrivers(company).size();
        int totalDrivers = deliveryCompanyService.getDrivers(company).size();
        
        if (totalDrivers == 0) {
            return 0.0;
        }
        
        double availabilityRatio = (double) availableDrivers / totalDrivers;
        
        // Bonus for having more available drivers
        double capacityBonus = Math.min(availableDrivers / 10.0, 0.3); // Max 30% bonus for 10+ drivers
        
        return Math.min(1.0, availabilityRatio + capacityBonus);
    }

    /**
     * Calculate availability score for delivery company
     */
    private double calculateAvailabilityScore(DeliveryCompany company) {
        if (!company.getIsActive() || !company.getIsLicensed()) {
            return 0.0;
        }
        
        double score = 0.7; // Base score for active and licensed
        
        // Available drivers factor
        int availableDrivers = deliveryCompanyService.getAvailableDrivers(company).size();
        if (availableDrivers > 0) {
            score += 0.3;
        }
        
        // Current load factor
        long activeOrders = deliveryCompanyService.getActiveOrdersCount(company);
        long capacity = company.getMaxDrivers();
        
        if (capacity > 0) {
            double loadRatio = (double) activeOrders / capacity;
            if (loadRatio < 0.5) {
                score += 0.1; // Low load bonus
            } else if (loadRatio > 0.8) {
                score -= 0.2; // High load penalty
            }
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Calculate distance score based on region matching
     */
    private double calculateDistanceScore(DeliveryCompany company, Order order) {
        String companyRegion = company.getServiceRegion();
        String orderArea = extractServiceArea(order.getDeliveryAddress());
        
        if (companyRegion != null && orderArea != null && 
            companyRegion.toLowerCase().contains(orderArea.toLowerCase())) {
            return 1.0; // Perfect match
        }
        
        return 0.7; // Default score for serviceable area
    }

    /**
     * Calculate historical performance score
     */
    private double calculateHistoryScore(Partnership partnership, VendorCompany vendor) {
        double score = 0.5; // Base score
        
        // Order volume factor
        Long totalOrders = partnership.getTotalOrdersCompleted();
        if (totalOrders != null && totalOrders > 0) {
            double volumeScore = Math.min(totalOrders / 100.0, 0.3); // Max 30% for 100+ orders
            score += volumeScore;
        }
        
        // Revenue factor
        BigDecimal totalRevenue = partnership.getTotalRevenueGenerated();
        if (totalRevenue != null && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            double revenueScore = Math.min(totalRevenue.doubleValue() / 10000.0, 0.2); // Max 20% for $10k+
            score += revenueScore;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Apply bonuses and penalties for partnerships
     */
    private double applyPartnershipBonuses(double baseScore, Partnership partnership, Order order) {
        double adjustedScore = baseScore;
        
        // Exclusive partnership bonus
        if (partnership.getIsExclusive()) {
            adjustedScore += 0.1;
        }
        
        // Long-term partnership bonus
        if (partnership.getContractStartDate() != null) {
            long monthsActive = ChronoUnit.MONTHS.between(
                    partnership.getContractStartDate(), LocalDateTime.now());
            if (monthsActive >= 12) {
                adjustedScore += 0.05; // Long-term bonus
            }
        }
        
        // High-value order handling bonus
        if (order.getOrderAmount().compareTo(BigDecimal.valueOf(500)) > 0) {
            if (partnership.getMinimumOrderValue() == null || 
                partnership.getMinimumOrderValue().compareTo(order.getOrderAmount()) <= 0) {
                adjustedScore += 0.05;
            }
        }
        
        // Service area perfect match bonus
        if (isInServiceArea(order, partnership)) {
            adjustedScore += 0.05;
        }
        
        return Math.max(0.0, Math.min(1.0, adjustedScore));
    }

    /**
     * Apply bonuses and penalties for delivery companies
     */
    private double applyCompanyBonuses(double baseScore, DeliveryCompany company, Order order) {
        double adjustedScore = baseScore;
        
        // High rating bonus
        if (company.getRating() != null && company.getRating().compareTo(BigDecimal.valueOf(4.5)) >= 0) {
            adjustedScore += 0.05;
        }
        
        // Large fleet bonus
        if (deliveryCompanyService.getDrivers(company).size() >= 20) {
            adjustedScore += 0.03;
        }
        
        // Recent activity bonus
        if (company.getLastDeliveryDate() != null) {
            long daysSinceLastDelivery = ChronoUnit.DAYS.between(
                    company.getLastDeliveryDate(), LocalDateTime.now());
            if (daysSinceLastDelivery <= 1) {
                adjustedScore += 0.05;
            }
        }
        
        // Emergency contact availability bonus
        if (company.getEmergencyContact() != null && !company.getEmergencyContact().isEmpty()) {
            adjustedScore += 0.02;
        }
        
        return Math.max(0.0, Math.min(1.0, adjustedScore));
    }

    /**
     * Check if order is in partnership service area
     */
    private boolean isInServiceArea(Order order, Partnership partnership) {
        List<String> serviceAreas = partnership.getServiceAreas();
        if (serviceAreas == null || serviceAreas.isEmpty()) {
            return true; // No restrictions
        }
        
        String orderArea = extractServiceArea(order.getDeliveryAddress());
        return serviceAreas.stream()
                .anyMatch(area -> area.toLowerCase().contains(orderArea.toLowerCase()));
    }

    /**
     * Extract service area from address
     */
    private String extractServiceArea(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        
        String[] parts = address.split(",");
        return parts.length > 1 ? parts[parts.length - 2].trim() : "";
    }

    /**
     * Partnership scoring result
     */
    public static class PartnershipScore {
        private Partnership partnership;
        private double totalScore;
        private Map<String, Double> componentScores;

        public PartnershipScore(Partnership partnership, double totalScore, Map<String, Double> componentScores) {
            this.partnership = partnership;
            this.totalScore = BigDecimal.valueOf(totalScore).setScale(3, RoundingMode.HALF_UP).doubleValue();
            this.componentScores = componentScores;
        }

        // Getters
        public Partnership getPartnership() { return partnership; }
        public double getTotalScore() { return totalScore; }
        public Map<String, Double> getComponentScores() { return componentScores; }
        
        public String getScoreBreakdown() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Total: %.3f (", totalScore));
            componentScores.forEach((key, value) -> 
                sb.append(String.format("%s: %.3f, ", key, value)));
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2); // Remove last comma
            }
            sb.append(")");
            return sb.toString();
        }
    }

    /**
     * Delivery company scoring result
     */
    public static class DeliveryCompanyScore {
        private DeliveryCompany deliveryCompany;
        private double totalScore;
        private Map<String, Double> componentScores;

        public DeliveryCompanyScore(DeliveryCompany deliveryCompany, double totalScore, Map<String, Double> componentScores) {
            this.deliveryCompany = deliveryCompany;
            this.totalScore = BigDecimal.valueOf(totalScore).setScale(3, RoundingMode.HALF_UP).doubleValue();
            this.componentScores = componentScores;
        }

        // Getters
        public DeliveryCompany getDeliveryCompany() { return deliveryCompany; }
        public double getTotalScore() { return totalScore; }
        public Map<String, Double> getComponentScores() { return componentScores; }
        
        public String getScoreBreakdown() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Total: %.3f (", totalScore));
            componentScores.forEach((key, value) -> 
                sb.append(String.format("%s: %.3f, ", key, value)));
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2); // Remove last comma
            }
            sb.append(")");
            return sb.toString();
        }
    }
}