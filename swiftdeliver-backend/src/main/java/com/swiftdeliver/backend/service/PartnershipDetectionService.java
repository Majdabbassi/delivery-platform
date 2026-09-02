package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.*;
import com.swiftdeliver.backend.repository.*;
import com.swiftdeliver.backend.specification.PartnershipSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting and managing partnerships
 * Handles partnership discovery, validation, and recommendation logic
 */
@Service
@Transactional
public class PartnershipDetectionService {

    // Logger removed as it was not being used

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private VendorCompanyRepository vendorCompanyRepository;

    @Autowired
    private DeliveryCompanyRepository deliveryCompanyRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryCompanyService deliveryCompanyService;

    /**
     * Detect existing partnerships for a vendor company
     */
    public List<Partnership> detectPartnershipsForVendor(Long vendorCompanyId) {
        VendorCompany vendor = vendorCompanyRepository.findById(vendorCompanyId)
                .orElseThrow(() -> new RuntimeException("Vendor company not found: " + vendorCompanyId));

        return partnershipRepository.findByVendorCompany(vendor);
    }

    /**
     * Detect existing partnerships for a delivery company
     */
    public List<Partnership> detectPartnershipsForDelivery(Long deliveryCompanyId) {
        DeliveryCompany delivery = deliveryCompanyRepository.findById(deliveryCompanyId)
                .orElseThrow(() -> new RuntimeException("Delivery company not found: " + deliveryCompanyId));

        return partnershipRepository.findByDeliveryCompany(delivery);
    }

    /**
     * Detect partnerships for a specific order
     */
    public List<Partnership> detectPartnershipsForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        return detectPartnershipsForOrder(order);
    }

    /**
     * Detect partnerships for a specific order entity
     */
    public List<Partnership> detectPartnershipsForOrder(Order order) {
        Specification<Partnership> spec = PartnershipSpecifications
                .isEligibleForOrder(
                    order.getOrderAmount(),
                    order.getEstimatedDistance(),
                    extractServiceArea(order.getDeliveryAddress())
                )
                .and(PartnershipSpecifications.hasVendorCompany(order.getVendorCompany()));

        return partnershipRepository.findAll(spec);
    }

    /**
     * Detect potential partnerships based on order history and patterns
     */
    public List<PartnershipRecommendation> detectPotentialPartnerships(Long vendorCompanyId) {
        VendorCompany vendor = vendorCompanyRepository.findById(vendorCompanyId)
                .orElseThrow(() -> new RuntimeException("Vendor company not found: " + vendorCompanyId));

        // Analyze order patterns
        List<Order> recentOrders = orderRepository.findRecentOrdersByVendorCompany(
                vendor, LocalDateTime.now().minusMonths(3));

        // Group orders by delivery company to find frequent collaborations
        Map<DeliveryCompany, List<Order>> ordersByDelivery = recentOrders.stream()
                .filter(order -> order.getDeliveryCompany() != null)
                .collect(Collectors.groupingBy(Order::getDeliveryCompany));

        List<PartnershipRecommendation> recommendations = new ArrayList<>();

        for (Map.Entry<DeliveryCompany, List<Order>> entry : ordersByDelivery.entrySet()) {
            DeliveryCompany deliveryCompany = entry.getKey();
            List<Order> orders = entry.getValue();

            // Check if partnership already exists
            if (hasExistingPartnership(vendor, deliveryCompany)) {
                continue;
            }

            // Calculate recommendation score
            PartnershipRecommendation recommendation = calculateRecommendation(
                    vendor, deliveryCompany, orders);

            if (recommendation.getScore() >= 0.6) { // Minimum threshold
                recommendations.add(recommendation);
            }
        }

        // Sort by score descending
        recommendations.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));

        return recommendations;
    }

    /**
     * Detect conflicting partnerships (exclusivity violations)
     */
    public List<PartnershipConflict> detectPartnershipConflicts() {
        List<PartnershipConflict> conflicts = new ArrayList<>();

        // Find all active exclusive partnerships
        List<Partnership> exclusivePartnerships = partnershipRepository.findAll(
                PartnershipSpecifications.isCurrentlyActive()
                        .and(PartnershipSpecifications.isExclusive(true))
        );

        // Check for conflicts
        for (Partnership exclusive : exclusivePartnerships) {
            // Find other active partnerships with same vendor
            List<Partnership> vendorPartnerships = partnershipRepository.findAll(
                    PartnershipSpecifications.hasVendorCompany(exclusive.getVendorCompany())
                            .and(PartnershipSpecifications.isCurrentlyActive())
            );

            for (Partnership other : vendorPartnerships) {
                if (!other.getId().equals(exclusive.getId()) && 
                    hasServiceAreaOverlap(exclusive, other)) {
                    
                    conflicts.add(new PartnershipConflict(
                            exclusive, other, "Exclusive partnership conflict in service area"));
                }
            }

            // Find other active partnerships with same delivery company
            List<Partnership> deliveryPartnerships = partnershipRepository.findAll(
                    PartnershipSpecifications.hasDeliveryCompany(exclusive.getDeliveryCompany())
                            .and(PartnershipSpecifications.isCurrentlyActive())
            );

            for (Partnership other : deliveryPartnerships) {
                if (!other.getId().equals(exclusive.getId()) && 
                    hasServiceAreaOverlap(exclusive, other)) {
                    
                    conflicts.add(new PartnershipConflict(
                            exclusive, other, "Exclusive partnership conflict in delivery company"));
                }
            }
        }

        return conflicts;
    }

    /**
     * Detect partnerships that are expiring soon
     */
    public List<Partnership> detectExpiringPartnerships(int daysAhead) {
        return partnershipRepository.findAll(
                PartnershipSpecifications.isExpiringSoon(daysAhead)
        );
    }

    /**
     * Detect underperforming partnerships
     */
    public List<Partnership> detectUnderperformingPartnerships() {
        List<Partnership> allActive = partnershipRepository.findAll(
                PartnershipSpecifications.isCurrentlyActive()
        );

        return allActive.stream()
                .filter(this::isUnderperforming)
                .collect(Collectors.toList());
    }

    /**
     * Detect partnerships with high performance
     */
    public List<Partnership> detectHighPerformingPartnerships() {
        return partnershipRepository.findAll(
                PartnershipSpecifications.hasGoodPerformance(
                        50L, // Minimum 50 orders
                        BigDecimal.valueOf(1000), // Minimum $1000 revenue
                        BigDecimal.valueOf(4.0) // Minimum 4.0 rating
                )
        );
    }

    /**
     * Validate partnership eligibility
     */
    public PartnershipValidationResult validatePartnership(
            Long vendorCompanyId, Long deliveryCompanyId) {
        
        VendorCompany vendor = vendorCompanyRepository.findById(vendorCompanyId)
                .orElseThrow(() -> new RuntimeException("Vendor company not found: " + vendorCompanyId));
        
        DeliveryCompany delivery = deliveryCompanyRepository.findById(deliveryCompanyId)
                .orElseThrow(() -> new RuntimeException("Delivery company not found: " + deliveryCompanyId));

        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check if partnership already exists
        if (hasExistingPartnership(vendor, delivery)) {
            issues.add("Partnership already exists between these companies");
        }

        // Check company status
        if (!vendor.getIsActive()) {
            issues.add("Vendor company is not active");
        }
        
        if (!delivery.getIsActive()) {
            issues.add("Delivery company is not active");
        }

        // Check licensing
        if (!vendor.getIsVerified()) {
            warnings.add("Vendor company is not verified");
        }
        
        if (!delivery.getIsLicensed()) {
            issues.add("Delivery company is not licensed");
        }

        // Check capacity
        if (!deliveryCompanyService.hasAvailableDrivers(delivery)) {
            warnings.add("Delivery company has no available drivers");
        }

        // Check for exclusive conflicts
        List<Partnership> vendorExclusive = partnershipRepository.findByVendorCompany(vendor).stream()
                .filter(p -> p.getIsExclusive() && p.getStatus() == Partnership.PartnershipStatus.ACTIVE)
                .collect(Collectors.toList());
        
        if (!vendorExclusive.isEmpty()) {
            warnings.add("Vendor has existing exclusive partnerships");
        }

        List<Partnership> deliveryExclusive = partnershipRepository.findByDeliveryCompany(delivery).stream()
                .filter(p -> p.getIsExclusive() && p.getStatus() == Partnership.PartnershipStatus.ACTIVE)
                .collect(Collectors.toList());
        
        if (!deliveryExclusive.isEmpty()) {
            warnings.add("Delivery company has existing exclusive partnerships");
        }

        boolean isValid = issues.isEmpty();
        return new PartnershipValidationResult(isValid, issues, warnings);
    }

    /**
     * Calculate partnership recommendation score
     */
    private PartnershipRecommendation calculateRecommendation(
            VendorCompany vendor, DeliveryCompany delivery, List<Order> orders) {
        
        double score = 0.0;
        Map<String, Object> factors = new HashMap<>();

        // Order frequency factor (30%)
        double orderFrequency = orders.size() / 90.0; // Orders per day over 3 months
        score += Math.min(orderFrequency * 0.3, 0.3);
        factors.put("orderFrequency", orderFrequency);

        // Success rate factor (25%)
        long successfulOrders = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED)
                .count();
        double successRate = orders.isEmpty() ? 0 : (double) successfulOrders / orders.size();
        score += successRate * 0.25;
        factors.put("successRate", successRate);

        // Average rating factor (20%)
        double avgRating = orders.stream()
                .filter(o -> o.getCustomerRating() != null)
                .mapToDouble(o -> o.getCustomerRating().doubleValue())
                .average()
                .orElse(0.0);
        score += (avgRating / 5.0) * 0.2;
        factors.put("averageRating", avgRating);

        // Revenue potential factor (15%)
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getOrderAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double revenueScore = Math.min(totalRevenue.doubleValue() / 10000.0, 1.0); // Normalize to $10k
        score += revenueScore * 0.15;
        factors.put("totalRevenue", totalRevenue);

        // Delivery company rating factor (10%)
        double companyRating = delivery.getRating().doubleValue();
        score += (companyRating / 5.0) * 0.1;
        factors.put("companyRating", companyRating);

        return new PartnershipRecommendation(
                vendor, delivery, score, factors, 
                "Based on " + orders.size() + " recent orders");
    }

    /**
     * Check if partnership already exists
     */
    private boolean hasExistingPartnership(VendorCompany vendor, DeliveryCompany delivery) {
        return partnershipRepository.existsByVendorCompanyAndDeliveryCompany(vendor, delivery);
    }

    /**
     * Check if two partnerships have overlapping service areas
     */
    private boolean hasServiceAreaOverlap(Partnership p1, Partnership p2) {
        List<String> areas1 = p1.getServiceAreas();
        List<String> areas2 = p2.getServiceAreas();
        
        if (areas1 == null || areas2 == null || areas1.isEmpty() || areas2.isEmpty()) {
            return true; // No restrictions means overlap
        }
        
        // Simple overlap check - in real implementation, use proper geographic analysis
        for (String area1 : areas1) {
            for (String area2 : areas2) {
                if (area1.trim().toLowerCase().equals(area2.trim().toLowerCase())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if partnership is underperforming
     */
    private boolean isUnderperforming(Partnership partnership) {
        // Define underperformance criteria
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        
        // Low order count
        if (partnership.getTotalOrdersCompleted() < 10) {
            return true;
        }
        
        // Low rating
        if (partnership.getAverageRating() != null && 
            partnership.getAverageRating().compareTo(BigDecimal.valueOf(3.0)) < 0) {
            return true;
        }
        
        // No recent activity - check if contract end date is approaching or passed
        if (partnership.getContractEndDate() != null && 
            partnership.getContractEndDate().isBefore(threeMonthsAgo)) {
            return true;
        }
        
        return false;
    }

    /**
     * Extract service area from address
     */
    private String extractServiceArea(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        
        // Simple implementation - extract city/area from address
        String[] parts = address.split(",");
        return parts.length > 1 ? parts[parts.length - 2].trim() : "";
    }

    /**
     * Partnership recommendation result
     */
    public static class PartnershipRecommendation {
        private VendorCompany vendorCompany;
        private DeliveryCompany deliveryCompany;
        private double score;
        private Map<String, Object> factors;
        private String reason;

        public PartnershipRecommendation(VendorCompany vendorCompany, DeliveryCompany deliveryCompany,
                                       double score, Map<String, Object> factors, String reason) {
            this.vendorCompany = vendorCompany;
            this.deliveryCompany = deliveryCompany;
            this.score = score;
            this.factors = factors;
            this.reason = reason;
        }

        // Getters
        public VendorCompany getVendorCompany() { return vendorCompany; }
        public DeliveryCompany getDeliveryCompany() { return deliveryCompany; }
        public double getScore() { return score; }
        public Map<String, Object> getFactors() { return factors; }
        public String getReason() { return reason; }
    }

    /**
     * Partnership conflict result
     */
    public static class PartnershipConflict {
        private Partnership partnership1;
        private Partnership partnership2;
        private String conflictReason;

        public PartnershipConflict(Partnership partnership1, Partnership partnership2, String conflictReason) {
            this.partnership1 = partnership1;
            this.partnership2 = partnership2;
            this.conflictReason = conflictReason;
        }

        // Getters
        public Partnership getPartnership1() { return partnership1; }
        public Partnership getPartnership2() { return partnership2; }
        public String getConflictReason() { return conflictReason; }
    }

    /**
     * Partnership validation result
     */
    public static class PartnershipValidationResult {
        private boolean valid;
        private List<String> issues;
        private List<String> warnings;

        public PartnershipValidationResult(boolean valid, List<String> issues, List<String> warnings) {
            this.valid = valid;
            this.issues = issues;
            this.warnings = warnings;
        }

        // Getters
        public boolean isValid() { return valid; }
        public List<String> getIssues() { return issues; }
        public List<String> getWarnings() { return warnings; }
    }
}