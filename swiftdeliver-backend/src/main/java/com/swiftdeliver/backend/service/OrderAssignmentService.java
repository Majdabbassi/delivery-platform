package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.*;
import com.swiftdeliver.backend.repository.*;
import com.swiftdeliver.backend.specification.PartnershipSpecifications;
import com.swiftdeliver.backend.service.ScoringAlgorithmService.PartnershipScore;
import com.swiftdeliver.backend.service.ScoringAlgorithmService.DeliveryCompanyScore;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling order assignment logic
 * Manages the assignment of orders to delivery companies and partnerships
 */
@Service
@Transactional
public class OrderAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(OrderAssignmentService.class);

    private final OrderRepository orderRepository;
    private final PartnershipRepository partnershipRepository;
    private final DeliveryCompanyRepository deliveryCompanyRepository;
    private final DriverPersonRepository driverPersonRepository;
    private final ScoringAlgorithmService scoringAlgorithmService;
    private final DeliveryCompanyService deliveryCompanyService;

    public OrderAssignmentService(OrderRepository orderRepository,
                                  PartnershipRepository partnershipRepository,
                                  DeliveryCompanyRepository deliveryCompanyRepository,
                                  DriverPersonRepository driverPersonRepository,
                                  ScoringAlgorithmService scoringAlgorithmService,
                                  DeliveryCompanyService deliveryCompanyService) {
        this.orderRepository = orderRepository;
        this.partnershipRepository = partnershipRepository;
        this.deliveryCompanyRepository = deliveryCompanyRepository;
        this.driverPersonRepository = driverPersonRepository;
        this.scoringAlgorithmService = scoringAlgorithmService;
        this.deliveryCompanyService = deliveryCompanyService;
    }

    /**
     * Assign an order using the best available option
     */
    public OrderAssignmentResult assignOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in PENDING status: " + order.getStatus());
        }

        logger.info("Starting assignment process for order: {}", orderId);

        // Try partnership assignment first
        OrderAssignmentResult partnershipResult = tryPartnershipAssignment(order);
        if (partnershipResult.isSuccessful()) {
            logger.info("Order {} assigned via partnership: {}", orderId, partnershipResult.getPartnership().getId());
            return partnershipResult;
        }

        // Fallback to direct delivery company assignment
        OrderAssignmentResult directResult = tryDirectAssignment(order);
        if (directResult.isSuccessful()) {
            logger.info("Order {} assigned directly to delivery company: {}", orderId, directResult.getDeliveryCompany().getId());
            return directResult;
        }

        logger.warn("Failed to assign order: {}", orderId);
        return OrderAssignmentResult.failed("No suitable delivery option found");
    }

    /**
     * Try to assign order through partnerships
     */
    private OrderAssignmentResult tryPartnershipAssignment(Order order) {
        List<Partnership> eligiblePartnerships = findEligiblePartnerships(order);
        
        if (eligiblePartnerships.isEmpty()) {
            return OrderAssignmentResult.failed("No eligible partnerships found");
        }

        // Score and rank partnerships
        List<PartnershipScore> scoredPartnerships = scoringAlgorithmService
                .scorePartnerships(eligiblePartnerships, order);

        // Try to assign to the best partnership
        for (PartnershipScore partnershipScore : scoredPartnerships) {
            Partnership partnership = partnershipScore.getPartnership();
            
            if (canAssignToPartnership(order, partnership)) {
                return assignToPartnership(order, partnership);
            }
        }

        return OrderAssignmentResult.failed("No partnerships can accept the order");
    }

    /**
     * Try to assign order directly to delivery companies
     */
    private OrderAssignmentResult tryDirectAssignment(Order order) {
        List<DeliveryCompany> availableCompanies = findAvailableDeliveryCompanies(order);
        
        if (availableCompanies.isEmpty()) {
            return OrderAssignmentResult.failed("No available delivery companies found");
        }

        // Score and rank delivery companies
        List<DeliveryCompanyScore> scoredCompanies = scoringAlgorithmService
                .scoreDeliveryCompanies(availableCompanies, order);

        // Try to assign to the best delivery company
        for (DeliveryCompanyScore companyScore : scoredCompanies) {
            DeliveryCompany company = companyScore.getDeliveryCompany();
            
            if (canAssignToDeliveryCompany(order, company)) {
                return assignToDeliveryCompany(order, company);
            }
        }

        return OrderAssignmentResult.failed("No delivery companies can accept the order");
    }

    /**
     * Find eligible partnerships for an order
     */
    private List<Partnership> findEligiblePartnerships(Order order) {
        Specification<Partnership> spec = PartnershipSpecifications
                .findBestForOrder(
                    order.getVendorCompany().getId(),
                    order.getOrderAmount(),
                    order.getEstimatedDistance(),
                    extractServiceArea(order.getDeliveryAddress()),
                    true // Prefer exclusive partnerships
                );

        return partnershipRepository.findAll(spec, 
                Sort.by(Sort.Direction.DESC, "averageRating", "totalOrdersCompleted"));
    }

    /**
     * Find available delivery companies for an order
     */
    private List<DeliveryCompany> findAvailableDeliveryCompanies(Order order) {
        return deliveryCompanyRepository.findAll().stream()
                .filter(company -> deliveryCompanyService.isAvailableForOrders(company))
                .filter(company -> deliveryCompanyService.canAcceptOrder(company, order))
                .sorted((c1, c2) -> {
                    // Sort by rating and then by available drivers
                    int ratingCompare = c2.getRating().compareTo(c1.getRating());
                    if (ratingCompare != 0) return ratingCompare;
                    return Integer.compare(deliveryCompanyService.getAvailableDrivers(c2).size(),
                            deliveryCompanyService.getAvailableDrivers(c1).size());
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if order can be assigned to a partnership
     */
    private boolean canAssignToPartnership(Order order, Partnership partnership) {
        DeliveryCompany deliveryCompany = partnership.getDeliveryCompany();
        
        return partnership.getStatus() == Partnership.PartnershipStatus.ACTIVE &&
               deliveryCompanyService.isAvailableForOrders(deliveryCompany) &&
               deliveryCompanyService.hasAvailableDrivers(deliveryCompany) &&
               isWithinOrderConstraints(order, partnership) &&
               isWithinServiceArea(order, partnership);
    }

    /**
     * Check if order can be assigned to a delivery company
     */
    private boolean canAssignToDeliveryCompany(Order order, DeliveryCompany company) {
        return deliveryCompanyService.isAvailableForOrders(company) &&
               deliveryCompanyService.hasAvailableDrivers(company) &&
               deliveryCompanyService.canAcceptOrder(company, order);
    }

    /**
     * Assign order to a partnership
     */
    private OrderAssignmentResult assignToPartnership(Order order, Partnership partnership) {
        try {
            DeliveryCompany deliveryCompany = partnership.getDeliveryCompany();
            DriverPerson driver = selectBestDriver(deliveryCompany, order);

            order.setPartnership(partnership);
            order.setDeliveryCompany(deliveryCompany);
            order.setDriverPerson(driver);
            order.setStatus(Order.OrderStatus.ASSIGNED);
            order.setAssignedAt(LocalDateTime.now());

            // Calculate delivery fee based on partnership commission
            BigDecimal deliveryFee = calculatePartnershipDeliveryFee(order, partnership);
            order.setDeliveryFee(deliveryFee);

            orderRepository.save(order);

            // Update partnership metrics
            partnership.incrementOrderCount();
            partnershipRepository.save(partnership);

            // Update driver availability
            if (driver != null) {
                driver.setIsAvailable(false);
                driverPersonRepository.save(driver);
            }

            logger.info("Order {} successfully assigned to partnership {} with driver {}", 
                    order.getId(), partnership.getId(), driver != null ? driver.getId() : "none");

            return OrderAssignmentResult.success(order, partnership, deliveryCompany, driver);

        } catch (Exception e) {
            logger.error("Failed to assign order {} to partnership {}: {}", 
                    order.getId(), partnership.getId(), e.getMessage());
            return OrderAssignmentResult.failed("Assignment failed: " + e.getMessage());
        }
    }

    /**
     * Assign order to a delivery company
     */
    private OrderAssignmentResult assignToDeliveryCompany(Order order, DeliveryCompany company) {
        try {
            DriverPerson driver = selectBestDriver(company, order);

            order.setDeliveryCompany(company);
            order.setDriverPerson(driver);
            order.setStatus(Order.OrderStatus.ASSIGNED);
            order.setAssignedAt(LocalDateTime.now());

            // Calculate delivery fee based on company rates
            BigDecimal deliveryFee = calculateDirectDeliveryFee(order, company);
            order.setDeliveryFee(deliveryFee);

            orderRepository.save(order);

            // Update driver availability
            if (driver != null) {
                driver.setIsAvailable(false);
                driverPersonRepository.save(driver);
            }

            logger.info("Order {} successfully assigned to delivery company {} with driver {}", 
                    order.getId(), company.getId(), driver != null ? driver.getId() : "none");

            return OrderAssignmentResult.success(order, null, company, driver);

        } catch (Exception e) {
            logger.error("Failed to assign order {} to delivery company {}: {}", 
                    order.getId(), company.getId(), e.getMessage());
            return OrderAssignmentResult.failed("Assignment failed: " + e.getMessage());
        }
    }

    /**
     * Select the best available driver for an order
     */
    private DriverPerson selectBestDriver(DeliveryCompany company, Order order) {
        List<DriverPerson> availableDrivers = deliveryCompanyService.getAvailableDrivers(company);
        
        if (availableDrivers.isEmpty()) {
            return null;
        }

        // Simple selection based on rating and proximity (can be enhanced)
        return availableDrivers.stream()
                .filter(driver -> driver.getIsAvailable())
                .max(Comparator.comparing(DriverPerson::getRating)
                        .thenComparing(driver -> -driver.getTotalDeliveries()))
                .orElse(null);
    }

    /**
     * Calculate delivery fee for partnership orders
     */
    private BigDecimal calculatePartnershipDeliveryFee(Order order, Partnership partnership) {
        BigDecimal baseFee = order.getBaseFee() != null ? order.getBaseFee() : BigDecimal.valueOf(5.00);
        BigDecimal distanceFee = distanceOrZero(order).multiply(BigDecimal.valueOf(0.50));
        BigDecimal partnershipDiscount = baseFee.multiply(partnership.getCommissionRate());
        
        return baseFee.add(distanceFee).subtract(partnershipDiscount);
    }

    /**
     * Calculates the direct delivery fee, falling back to zero distance when the
     * order has no distance recorded. Null-safe against missing distanceKm.
     */
    private BigDecimal calculateDirectDeliveryFee(Order order, DeliveryCompany company) {
        BigDecimal baseFee = order.getBaseFee() != null ? order.getBaseFee() : BigDecimal.valueOf(5.00);
        BigDecimal distanceFee = distanceOrZero(order).multiply(BigDecimal.valueOf(0.50));
        BigDecimal companyCommission = baseFee.multiply(company.getCommissionRate());
        
        return baseFee.add(distanceFee).add(companyCommission);
    }

    private BigDecimal distanceOrZero(Order order) {
        BigDecimal distance = order.getEstimatedDistance();
        return distance != null ? distance : BigDecimal.ZERO;
    }

    /**
     * Check if order is within partnership constraints
     */
    private boolean isWithinOrderConstraints(Order order, Partnership partnership) {
        BigDecimal orderValue = order.getOrderAmount();
        BigDecimal distance = distanceOrZero(order);
        double distanceDouble = distance.doubleValue();

        // Check order value constraints
        if (partnership.getMinOrderValue() != null && 
            orderValue.compareTo(partnership.getMinOrderValue()) < 0) {
            return false;
        }
        
        if (partnership.getMaxOrderValue() != null && 
            orderValue.compareTo(partnership.getMaxOrderValue()) > 0) {
            return false;
        }

        // Check distance constraints
        if (partnership.getMinDeliveryDistance() != null && 
            distanceDouble < partnership.getMinDeliveryDistance()) {
            return false;
        }
        
        if (partnership.getMaxDeliveryDistance() != null && 
            distanceDouble > partnership.getMaxDeliveryDistance()) {
            return false;
        }

        return true;
    }

    /**
     * Check if order is within partnership service area
     */
    private boolean isWithinServiceArea(Order order, Partnership partnership) {
        List<String> serviceAreas = partnership.getServiceAreas();
        if (serviceAreas == null || serviceAreas.isEmpty()) {
            return true; // No restrictions
        }

        String orderServiceArea = extractServiceArea(order.getDeliveryAddress());
        return serviceAreas.stream()
                .anyMatch(area -> area.toLowerCase().contains(orderServiceArea.toLowerCase()));
    }

    /**
     * Extract service area from address (simplified implementation)
     */
    private String extractServiceArea(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        
        // Simple implementation - extract city/area from address
        // In real implementation, this would use geocoding or address parsing
        String[] parts = address.split(",");
        return parts.length > 1 ? parts[parts.length - 2].trim() : "";
    }

    /**
     * Reassign an order to a different delivery option
     */
    public OrderAssignmentResult reassignOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getStatus() != Order.OrderStatus.ASSIGNED) {
            throw new RuntimeException("Order is not in ASSIGNED status: " + order.getStatus());
        }

        logger.info("Reassigning order: {} due to: {}", orderId, reason);

        // Release current assignment
        releaseCurrentAssignment(order);

        // Reset order status
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPartnership(null);
        order.setDeliveryCompany(null);
        order.setDriverPerson(null);
        order.setAssignedAt(null);
        
        orderRepository.save(order);

        // Try to assign again
        return assignOrder(orderId);
    }

    /**
     * Release current assignment resources
     */
    private void releaseCurrentAssignment(Order order) {
        if (order.getDriverPerson() != null) {
            DriverPerson driver = order.getDriverPerson();
            driver.setIsAvailable(true);
            driverPersonRepository.save(driver);
        }
    }

    /**
     * Get assignment statistics
     */
    public AssignmentStatistics getAssignmentStatistics() {
        long totalOrders = orderRepository.count();
        long assignedOrders = orderRepository.countByStatus(Order.OrderStatus.ASSIGNED);
        long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PENDING);
        long partnershipOrders = orderRepository.countByPartnershipIsNotNull();
        long directOrders = orderRepository.countByPartnershipIsNullAndDeliveryCompanyIsNotNull();

        return new AssignmentStatistics(
                totalOrders,
                assignedOrders,
                pendingOrders,
                partnershipOrders,
                directOrders
        );
    }

    /**
     * Result class for order assignment operations
     */
    public static class OrderAssignmentResult {
        private boolean successful;
        private String message;
        private Order order;
        private Partnership partnership;
        private DeliveryCompany deliveryCompany;
        private DriverPerson driver;

        private OrderAssignmentResult(boolean successful, String message) {
            this.successful = successful;
            this.message = message;
        }

        public static OrderAssignmentResult success(Order order, Partnership partnership, 
                                                  DeliveryCompany deliveryCompany, DriverPerson driver) {
            OrderAssignmentResult result = new OrderAssignmentResult(true, "Assignment successful");
            result.order = order;
            result.partnership = partnership;
            result.deliveryCompany = deliveryCompany;
            result.driver = driver;
            return result;
        }

        public static OrderAssignmentResult failed(String message) {
            return new OrderAssignmentResult(false, message);
        }

        // Getters
        public boolean isSuccessful() { return successful; }
        public String getMessage() { return message; }
        public Order getOrder() { return order; }
        public Partnership getPartnership() { return partnership; }
        public DeliveryCompany getDeliveryCompany() { return deliveryCompany; }
        public DriverPerson getDriver() { return driver; }
    }

    /**
     * Statistics class for assignment metrics
     */
    public static class AssignmentStatistics {
        private long totalOrders;
        private long assignedOrders;
        private long pendingOrders;
        private long partnershipOrders;
        private long directOrders;

        public AssignmentStatistics(long totalOrders, long assignedOrders, long pendingOrders, 
                                  long partnershipOrders, long directOrders) {
            this.totalOrders = totalOrders;
            this.assignedOrders = assignedOrders;
            this.pendingOrders = pendingOrders;
            this.partnershipOrders = partnershipOrders;
            this.directOrders = directOrders;
        }

        // Getters
        public long getTotalOrders() { return totalOrders; }
        public long getAssignedOrders() { return assignedOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public long getPartnershipOrders() { return partnershipOrders; }
        public long getDirectOrders() { return directOrders; }
        
        public double getAssignmentRate() {
            return totalOrders > 0 ? (double) assignedOrders / totalOrders * 100 : 0;
        }
        
        public double getPartnershipRate() {
            return assignedOrders > 0 ? (double) partnershipOrders / assignedOrders * 100 : 0;
        }
    }
}