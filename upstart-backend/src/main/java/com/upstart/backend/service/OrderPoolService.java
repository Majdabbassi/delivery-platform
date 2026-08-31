package com.upstart.backend.service;

import com.upstart.backend.entity.Order;
import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.Partnership;
import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.repository.OrderRepository;
import com.upstart.backend.repository.DeliveryCompanyRepository;
import com.upstart.backend.repository.PartnershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for managing order pools and broadcasting orders to delivery companies
 * Handles order visibility, filtering, and pool management
 */
@Service
@Transactional
public class OrderPoolService {

    private static final Logger logger = LoggerFactory.getLogger(OrderPoolService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryCompanyRepository deliveryCompanyRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private DeliveryCompanyService deliveryCompanyService;

    // In-memory tracking of order pools and subscriptions
    private final Map<Long, Set<Long>> companyOrderPools = new ConcurrentHashMap<>();
    private final Map<Long, List<OrderPoolSubscription>> poolSubscriptions = new ConcurrentHashMap<>();
    private final Map<Long, OrderPoolMetrics> poolMetrics = new ConcurrentHashMap<>();

    /**
     * Add order to relevant delivery company pools
     */
    public void addOrderToPool(Order order) {
        logger.info("Adding order {} to delivery company pools", order.getId());

        List<DeliveryCompany> eligibleCompanies = findEligibleDeliveryCompanies(order);
        
        for (DeliveryCompany company : eligibleCompanies) {
            addOrderToCompanyPool(company.getId(), order.getId());
            updatePoolMetrics(company.getId(), "orders_added", 1);
        }

        logger.info("Order {} added to {} company pools", order.getId(), eligibleCompanies.size());
    }

    /**
     * Remove order from all pools
     */
    public void removeOrderFromPools(Long orderId) {
        logger.info("Removing order {} from all pools", orderId);

        int removedCount = 0;
        for (Map.Entry<Long, Set<Long>> entry : companyOrderPools.entrySet()) {
            if (entry.getValue().remove(orderId)) {
                removedCount++;
                updatePoolMetrics(entry.getKey(), "orders_removed", 1);
            }
        }

        logger.info("Order {} removed from {} company pools", orderId, removedCount);
    }

    /**
     * Get orders available to a specific delivery company
     */
    public Page<Order> getAvailableOrders(Long deliveryCompanyId, Pageable pageable) {
        Set<Long> orderIds = companyOrderPools.getOrDefault(deliveryCompanyId, new HashSet<>());
        
        if (orderIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return orderRepository.findByIdInAndStatus(orderIds, Order.OrderStatus.PENDING, pageable);
    }

    /**
     * Get filtered orders for delivery company based on criteria
     */
    public List<Order> getFilteredOrders(Long deliveryCompanyId, OrderPoolFilter filter) {
        Set<Long> orderIds = companyOrderPools.getOrDefault(deliveryCompanyId, new HashSet<>());
        
        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Order> orders = orderRepository.findByIdInAndStatus(orderIds, Order.OrderStatus.PENDING);
        
        return orders.stream()
            .filter(order -> matchesFilter(order, filter, deliveryCompanyId))
            .sorted(getOrderComparator(filter.getSortBy(), filter.getSortDirection()))
            .limit(filter.getLimit())
            .collect(Collectors.toList());
    }

    /**
     * Subscribe delivery company to order pool updates
     */
    public void subscribeToOrderPool(Long deliveryCompanyId, OrderPoolSubscription subscription) {
        poolSubscriptions.computeIfAbsent(deliveryCompanyId, k -> new CopyOnWriteArrayList<>())
                         .add(subscription);
        
        logger.info("Delivery company {} subscribed to order pool updates", deliveryCompanyId);
    }

    /**
     * Unsubscribe from order pool updates
     */
    public void unsubscribeFromOrderPool(Long deliveryCompanyId, String subscriptionId) {
        List<OrderPoolSubscription> subscriptions = poolSubscriptions.get(deliveryCompanyId);
        if (subscriptions != null) {
            subscriptions.removeIf(sub -> sub.getSubscriptionId().equals(subscriptionId));
        }
        
        logger.info("Delivery company {} unsubscribed from order pool updates", deliveryCompanyId);
    }

    /**
     * Broadcast order to subscribed delivery companies
     */
    public void broadcastOrderUpdate(Order order, OrderUpdateType updateType) {
        Set<Long> relevantCompanies = findRelevantCompanies(order);
        
        for (Long companyId : relevantCompanies) {
            List<OrderPoolSubscription> subscriptions = poolSubscriptions.get(companyId);
            if (subscriptions != null) {
                for (OrderPoolSubscription subscription : subscriptions) {
                    if (subscription.matchesUpdateType(updateType)) {
                        subscription.getCallback().accept(order, updateType);
                    }
                }
            }
        }
        
        logger.debug("Broadcasted order {} update to {} companies", order.getId(), relevantCompanies.size());
    }

    /**
     * Get pool statistics for delivery company
     */
    public OrderPoolStatistics getPoolStatistics(Long deliveryCompanyId) {
        Set<Long> orderIds = companyOrderPools.getOrDefault(deliveryCompanyId, new HashSet<>());
        OrderPoolMetrics metrics = poolMetrics.getOrDefault(deliveryCompanyId, new OrderPoolMetrics());
        
        List<Order> currentOrders = orderRepository.findByIdInAndStatus(orderIds, Order.OrderStatus.PENDING);
        
        BigDecimal totalValue = currentOrders.stream()
            .map(Order::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<Order.OrderPriority, Long> priorityDistribution = currentOrders.stream()
            .collect(Collectors.groupingBy(Order::getPriority, Collectors.counting()));
        
        return new OrderPoolStatistics(
            currentOrders.size(),
            totalValue,
            priorityDistribution,
            metrics.getOrdersAdded(),
            metrics.getOrdersRemoved(),
            metrics.getOrdersViewed(),
            metrics.getLastActivity()
        );
    }

    /**
     * Mark order as viewed by delivery company
     */
    public void markOrderAsViewed(Long deliveryCompanyId, Long orderId) {
        updatePoolMetrics(deliveryCompanyId, "orders_viewed", 1);
        updatePoolMetrics(deliveryCompanyId, "last_activity", System.currentTimeMillis());
    }

    /**
     * Get recommended orders for delivery company
     */
    public List<Order> getRecommendedOrders(Long deliveryCompanyId, int limit) {
        DeliveryCompany company = deliveryCompanyRepository.findById(deliveryCompanyId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery company not found"));
        
        Set<Long> orderIds = companyOrderPools.getOrDefault(deliveryCompanyId, new HashSet<>());
        
        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Order> orders = orderRepository.findByIdInAndStatus(orderIds, Order.OrderStatus.PENDING);
        
        return orders.stream()
            .filter(order -> isOrderRecommended(order, company))
            .sorted(this::compareOrdersByRecommendationScore)
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Clear expired orders from pools
     */
    public void clearExpiredOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // Orders older than 24 hours
        
        List<Long> expiredOrderIds = orderRepository.findExpiredPendingOrders(cutoff)
            .stream()
            .map(Order::getId)
            .collect(Collectors.toList());
        
        for (Long orderId : expiredOrderIds) {
            removeOrderFromPools(orderId);
        }
        
        logger.info("Cleared {} expired orders from pools", expiredOrderIds.size());
    }

    /**
     * Get pool health metrics
     */
    public Map<String, Object> getPoolHealthMetrics() {
        Map<String, Object> health = new HashMap<>();
        
        int totalPools = companyOrderPools.size();
        int totalOrders = companyOrderPools.values().stream()
            .mapToInt(Set::size)
            .sum();
        
        int activeSubscriptions = poolSubscriptions.values().stream()
            .mapToInt(List::size)
            .sum();
        
        health.put("total_pools", totalPools);
        health.put("total_orders_in_pools", totalOrders);
        health.put("active_subscriptions", activeSubscriptions);
        health.put("average_orders_per_pool", totalPools > 0 ? (double) totalOrders / totalPools : 0);
        
        return health;
    }

    // Private helper methods

    private List<DeliveryCompany> findEligibleDeliveryCompanies(Order order) {
        List<DeliveryCompany> allCompanies = deliveryCompanyRepository.findByIsActive(true);
        
        return allCompanies.stream()
            .filter(company -> isCompanyEligibleForOrder(company, order))
            .collect(Collectors.toList());
    }

    private boolean isCompanyEligibleForOrder(DeliveryCompany company, Order order) {
        // Check if company is active and licensed
        if (!company.getIsActive() || !company.getIsLicensed()) {
            return false;
        }
        
        // Check service area coverage
        if (!isOrderInServiceArea(order, company)) {
            return false;
        }
        
        // Check capacity
        if (!deliveryCompanyService.hasAvailableDrivers(company)) {
            return false;
        }
        
        // Check if there's an exclusive partnership that excludes this company
        return !hasExclusivePartnershipConflict(order, company);
    }

    private boolean isOrderInServiceArea(Order order, DeliveryCompany company) {
        if (company.getServiceRegion() == null || order.getDeliveryAddress() == null) {
            return true; // Default to eligible if no restrictions
        }
        
        // Simple service area check - in production, use proper geospatial queries
        return company.getServiceRegion().toLowerCase()
            .contains(order.getDeliveryAddress().toLowerCase());
    }

    private boolean hasExclusivePartnershipConflict(Order order, DeliveryCompany company) {
        // Check if vendor has exclusive partnership with another delivery company
        List<Partnership> exclusivePartnerships = partnershipRepository
            .findByVendorCompanyAndIsExclusiveAndStatus(
                order.getVendorCompany(), true, Partnership.PartnershipStatus.ACTIVE);
        
        return exclusivePartnerships.stream()
            .anyMatch(p -> !p.getDeliveryCompany().getId().equals(company.getId()));
    }

    private void addOrderToCompanyPool(Long companyId, Long orderId) {
        companyOrderPools.computeIfAbsent(companyId, k -> ConcurrentHashMap.newKeySet())
                         .add(orderId);
    }

    private Set<Long> findRelevantCompanies(Order order) {
        return companyOrderPools.entrySet().stream()
            .filter(entry -> entry.getValue().contains(order.getId()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    private boolean matchesFilter(Order order, OrderPoolFilter filter, Long deliveryCompanyId) {
        // Apply various filters
        if (filter.getMinValue() != null && 
            (order.getTotalAmount() == null || order.getTotalAmount().compareTo(filter.getMinValue()) < 0)) {
            return false;
        }
        
        if (filter.getMaxValue() != null && 
            (order.getTotalAmount() == null || order.getTotalAmount().compareTo(filter.getMaxValue()) > 0)) {
            return false;
        }
        
        if (filter.getPriority() != null && !filter.getPriority().equals(order.getPriority())) {
            return false;
        }
        
        if (filter.getMaxDistance() != null) {
            DeliveryCompany company = deliveryCompanyRepository.findById(deliveryCompanyId).orElse(null);
            if (company != null && order.getDeliveryLatitude() != null && order.getDeliveryLongitude() != null) {
                // For now, skip distance filtering since company coordinates are not available
                // In production, DeliveryCompany should have latitude/longitude fields
                // String companyCoordinates = company.getLatitude() + "," + company.getLongitude();
                // String orderCoordinates = order.getDeliveryLatitude() + "," + order.getDeliveryLongitude();
                // BigDecimal distance = distanceUtils.calculateDistance(companyCoordinates, orderCoordinates);
                // if (distance.doubleValue() > filter.getMaxDistance()) {
                //     return false;
                // }
            }
        }
        
        return true;
    }

    private Comparator<Order> getOrderComparator(String sortBy, String sortDirection) {
        Comparator<Order> comparator;
        
        switch (sortBy != null ? sortBy.toLowerCase() : "created") {
            case "value":
                comparator = Comparator.comparing(Order::getTotalAmount, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "priority":
                comparator = Comparator.comparing(Order::getPriority);
                break;
            case "deadline":
                comparator = Comparator.comparing(Order::getEstimatedDeliveryTime, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                comparator = Comparator.comparing(Order::getCreatedAt);
        }
        
        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }
        
        return comparator;
    }

    private boolean isOrderRecommended(Order order, DeliveryCompany company) {
        // Simple recommendation logic - can be enhanced
        if (order.getPriority() == Order.OrderPriority.HIGH) {
            return true;
        }
        
        if (order.getTotalAmount() != null && 
            order.getTotalAmount().compareTo(BigDecimal.valueOf(100)) > 0) {
            return true;
        }
        
        // Check if company has good history with this vendor
        return hasGoodVendorHistory(company, order.getVendorCompany());
    }

    private boolean hasGoodVendorHistory(DeliveryCompany company, VendorCompany vendor) {
        // Check for existing partnership or good delivery history
        return partnershipRepository.existsByVendorCompanyAndDeliveryCompanyAndStatus(
            vendor, company, Partnership.PartnershipStatus.ACTIVE);
    }

    private int compareOrdersByRecommendationScore(Order a, Order b) {
        // Simple scoring - can be enhanced with proper algorithm
        int scoreA = calculateRecommendationScore(a);
        int scoreB = calculateRecommendationScore(b);
        return Integer.compare(scoreB, scoreA); // Higher score first
    }

    private int calculateRecommendationScore(Order order) {
        int score = 0;
        
        if (order.getPriority() == Order.OrderPriority.HIGH) score += 50;
        else if (order.getPriority() == Order.OrderPriority.NORMAL) score += 25;
        
        if (order.getTotalAmount() != null) {
            score += order.getTotalAmount().intValue() / 10; // 1 point per $10
        }
        
        // Urgency bonus
        if (order.getEstimatedDeliveryTime() != null && 
            order.getEstimatedDeliveryTime().isBefore(LocalDateTime.now().plusHours(2))) {
            score += 30;
        }
        
        return score;
    }

    private void updatePoolMetrics(Long companyId, String metric, long value) {
        OrderPoolMetrics metrics = poolMetrics.computeIfAbsent(companyId, k -> new OrderPoolMetrics());
        
        switch (metric) {
            case "orders_added":
                metrics.incrementOrdersAdded(value);
                break;
            case "orders_removed":
                metrics.incrementOrdersRemoved(value);
                break;
            case "orders_viewed":
                metrics.incrementOrdersViewed(value);
                break;
            case "last_activity":
                metrics.setLastActivity(LocalDateTime.now());
                break;
        }
    }

    // Inner classes and enums

    public enum OrderUpdateType {
        NEW_ORDER, ORDER_UPDATED, ORDER_ASSIGNED, ORDER_CANCELLED
    }

    public static class OrderPoolFilter {
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private Order.OrderPriority priority;
        private Double maxDistance;
        private String sortBy = "created";
        private String sortDirection = "desc";
        private int limit = 50;

        // Getters and Setters
        public BigDecimal getMinValue() { return minValue; }
        public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }

        public BigDecimal getMaxValue() { return maxValue; }
        public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }

        public Order.OrderPriority getPriority() { return priority; }
        public void setPriority(Order.OrderPriority priority) { this.priority = priority; }

        public Double getMaxDistance() { return maxDistance; }
        public void setMaxDistance(Double maxDistance) { this.maxDistance = maxDistance; }

        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }

        public String getSortDirection() { return sortDirection; }
        public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }

    public static class OrderPoolSubscription {
        private final String subscriptionId;
        private final Set<OrderUpdateType> updateTypes;
        private final java.util.function.BiConsumer<Order, OrderUpdateType> callback;
        private final LocalDateTime createdAt;

        public OrderPoolSubscription(String subscriptionId, Set<OrderUpdateType> updateTypes,
                                   java.util.function.BiConsumer<Order, OrderUpdateType> callback) {
            this.subscriptionId = subscriptionId;
            this.updateTypes = updateTypes;
            this.callback = callback;
            this.createdAt = LocalDateTime.now();
        }

        public boolean matchesUpdateType(OrderUpdateType updateType) {
            return updateTypes.contains(updateType);
        }

        // Getters
        public String getSubscriptionId() { return subscriptionId; }
        public Set<OrderUpdateType> getUpdateTypes() { return updateTypes; }
        public java.util.function.BiConsumer<Order, OrderUpdateType> getCallback() { return callback; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class OrderPoolStatistics {
        private final int currentOrderCount;
        private final BigDecimal totalValue;
        private final Map<Order.OrderPriority, Long> priorityDistribution;
        private final long ordersAdded;
        private final long ordersRemoved;
        private final long ordersViewed;
        private final LocalDateTime lastActivity;

        public OrderPoolStatistics(int currentOrderCount, BigDecimal totalValue,
                                 Map<Order.OrderPriority, Long> priorityDistribution,
                                 long ordersAdded, long ordersRemoved, long ordersViewed,
                                 LocalDateTime lastActivity) {
            this.currentOrderCount = currentOrderCount;
            this.totalValue = totalValue;
            this.priorityDistribution = priorityDistribution;
            this.ordersAdded = ordersAdded;
            this.ordersRemoved = ordersRemoved;
            this.ordersViewed = ordersViewed;
            this.lastActivity = lastActivity;
        }

        // Getters
        public int getCurrentOrderCount() { return currentOrderCount; }
        public BigDecimal getTotalValue() { return totalValue; }
        public Map<Order.OrderPriority, Long> getPriorityDistribution() { return priorityDistribution; }
        public long getOrdersAdded() { return ordersAdded; }
        public long getOrdersRemoved() { return ordersRemoved; }
        public long getOrdersViewed() { return ordersViewed; }
        public LocalDateTime getLastActivity() { return lastActivity; }
    }

    private static class OrderPoolMetrics {
        private long ordersAdded = 0;
        private long ordersRemoved = 0;
        private long ordersViewed = 0;
        private LocalDateTime lastActivity = LocalDateTime.now();

        public void incrementOrdersAdded(long count) { this.ordersAdded += count; }
        public void incrementOrdersRemoved(long count) { this.ordersRemoved += count; }
        public void incrementOrdersViewed(long count) { this.ordersViewed += count; }
        
        // Getters and Setters
        public long getOrdersAdded() { return ordersAdded; }
        public long getOrdersRemoved() { return ordersRemoved; }
        public long getOrdersViewed() { return ordersViewed; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    }
}