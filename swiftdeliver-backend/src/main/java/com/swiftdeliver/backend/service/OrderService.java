package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.*;
import com.swiftdeliver.backend.repository.OrderRepository;
import com.swiftdeliver.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final RealtimeTrackingService realtimeTrackingService;
    private final SecurityService securityService;
    private final OrderAssignmentService orderAssignmentService;
    private final OrderPoolService orderPoolService;

    @Value("${swiftdeliver.assignment.general.auto-assign-on-create:true}")
    private boolean autoAssignOnCreate;

    @Value("${swiftdeliver.orders.rate-limit.per-minute:20}")
    private int orderCreateLimitPerMinute;

    private final ConcurrentHashMap<String, Deque<Long>> orderCreateTimestamps = new ConcurrentHashMap<>();

    // Create operations
    public Order createOrder(Order order) {
        User currentUser = securityService.getCurrentUser();
        assertOrderCreateRateLimit(currentUser.getUsername());
        order.setCreatedByUserId(currentUser.getId());

        // Defaults for the new order-type / routing fields.
        if (order.getOrderType() == null) {
            order.setOrderType(Order.OrderType.MARKETPLACE);
        }
        if (order.getRoutingMode() == null) {
            order.setRoutingMode(Order.RoutingMode.OPEN_BID);
        }
        if (order.getPricingMode() == null) {
            order.setPricingMode(Order.PricingMode.FIXED);
        }

        // The JPA entity constraints cannot run via @Valid on the raw request body
        // (orderNumber is always null on create), so enforce the business rules
        // explicitly here.
        if (order.getPickupAddress() == null || order.getPickupAddress().isBlank()) {
            throw new IllegalArgumentException("Pickup address is required");
        }
        if (order.getDeliveryAddress() == null || order.getDeliveryAddress().isBlank()) {
            throw new IllegalArgumentException("Delivery address is required");
        }
        if (order.getPricingMode() == Order.PricingMode.FIXED
                && (order.getOrderAmount() == null
                    || order.getOrderAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Order amount must be greater than zero for fixed pricing");
        }

        boolean isGeneralDelivery = order.getOrderType() == Order.OrderType.GENERAL_DELIVERY;
        log.info("Creating new {} order for {}",
                order.getOrderType(),
                isGeneralDelivery ? "general delivery" :
                        (order.getVendorCompany() != null ? order.getVendorCompany().getId() : "unknown vendor"));

        // Tenant binding: a CLIENT may only create orders for themselves,
        // and a VENDOR_OWNER may only create orders for their own company.
        if (currentUser.getRole() == User.Role.CLIENT) {
            order.setCustomerUser(securityService.getCurrentCustomerUser());
        } else if (currentUser.getRole() == User.Role.VENDOR_OWNER && !isGeneralDelivery) {
            if (order.getVendorCompany() == null) {
                throw new IllegalArgumentException("Vendor company is required for marketplace orders");
            }
            securityService.getOwnedVendorCompanyOrThrow(order.getVendorCompany().getId());
        }
        
        // Generate order number if not provided
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            order.setOrderNumber(generateOrderNumber());
        }
        
        // Generate tracking number
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isEmpty()) {
            order.setTrackingNumber(generateTrackingNumber());
        }
        
        // Set default status if not provided
        if (order.getStatus() == null) {
            order.setStatus(Order.OrderStatus.PENDING);
        }
        // Pooled orders (open bid) start OPEN_FOR_BID so they are visible to bidders.
        if (order.getRoutingMode() == Order.RoutingMode.OPEN_BID
                && order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.OPEN_FOR_BID);
        }
        
        // Set default priority if not provided
        if (order.getPriority() == null) {
            order.setPriority(Order.OrderPriority.NORMAL);
        }
        
        // Adopt proposed pricing: when the sender proposes a range, apply it as
        // the order amount floor (min) and cap (max) for reference by bidders.
        if (order.getPricingMode() == Order.PricingMode.MIN_MAX) {
            if (order.getProposedMinAmount() == null || order.getProposedMaxAmount() == null) {
                throw new IllegalArgumentException(
                        "Proposed min and max amounts are required when pricing mode is MIN_MAX");
            }
            if (order.getProposedMinAmount().compareTo(order.getProposedMaxAmount()) > 0) {
                throw new IllegalArgumentException("Proposed min amount cannot exceed proposed max amount");
            }
            order.setOrderAmount(order.getProposedMinAmount());
        }
        
        // Calculate total amount if not provided
        if (order.getTotalAmount() == null && order.getOrderAmount() != null) {
            BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
            order.setTotalAmount(order.getOrderAmount().add(deliveryFee));
        }
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {} and order number: {}", savedOrder.getId(), savedOrder.getOrderNumber());
        
        // Pooled (open-bid) orders are pushed into company pools and remain
        // OPEN_FOR_BID so both delivery companies and independent drivers can bid.
        if (savedOrder.getRoutingMode() == Order.RoutingMode.OPEN_BID) {
            try {
                orderPoolService.addOrderToPool(savedOrder);
            } catch (Exception e) {
                log.warn("Failed to add order {} to company pools: {}", savedOrder.getId(), e.getMessage());
            }
        }
        
        realtimeTrackingService.broadcastOrderCreated(savedOrder);
        
        // Auto-assign only for direct-routed orders that are still pending; pooled
        // orders are left OPEN_FOR_BID so carriers/independent drivers can bid.
        if (autoAssignOnCreate
                && savedOrder.getRoutingMode() != Order.RoutingMode.OPEN_BID
                && savedOrder.getStatus() == Order.OrderStatus.PENDING) {
            OrderAssignmentService.OrderAssignmentResult result =
                    orderAssignmentService.assignOrder(savedOrder.getId());
            if (result.isSuccessful()) {
                realtimeTrackingService.broadcastDriverAssigned(result.getOrder());
                return result.getOrder();
            }
            log.warn("Auto-assignment for order {} did not succeed: {}", savedOrder.getId(), result.getMessage());
        }
        
        return savedOrder;
    }

    /**
     * Fixed-window per-user rate limit on order creation. Throws 429 when the
     * user exceeds the configured number of orders within a minute.
     */
    private void assertOrderCreateRateLimit(String username) {
        if (orderCreateLimitPerMinute <= 0) {
            return; // disabled
        }
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        Deque<Long> timestamps = orderCreateTimestamps.computeIfAbsent(username, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= orderCreateLimitPerMinute) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Order creation rate limit exceeded. Please try again later.");
            }
            timestamps.addLast(now);
        }
    }

    /**
     * Explicitly runs the assignment engine for an order. The order must be
     * PENDING. Returns the (possibly re-assigned) order.
     */
    public Order autoAssignOrder(Long orderId) {
        Order order = getOrderById(orderId);
        assertCanEditOrder(order);
        OrderAssignmentService.OrderAssignmentResult result =
                orderAssignmentService.assignOrder(orderId);
        if (result.isSuccessful()) {
            realtimeTrackingService.broadcastDriverAssigned(result.getOrder());
            return result.getOrder();
        }
        throw new IllegalStateException("Order could not be assigned: " + result.getMessage());
    }

    /**
     * Reassigns an already-assigned order. Requires ownership of the order.
     */
    public Order reassignOrder(Long orderId, String reason) {
        Order order = getOrderById(orderId);
        assertCanEditOrder(order);
        OrderAssignmentService.OrderAssignmentResult result =
                orderAssignmentService.reassignOrder(orderId, reason);
        if (result.isSuccessful()) {
            realtimeTrackingService.broadcastDriverAssigned(result.getOrder());
            return result.getOrder();
        }
        throw new IllegalStateException("Order could not be reassigned: " + result.getMessage());
    }

    /**
     * Returns global assignment statistics (super admin scope).
     */
    public OrderAssignmentService.AssignmentStatistics getAssignmentStatistics() {
        return orderAssignmentService.getAssignmentStatistics();
    }
    
    // Read operations
    public Order getOrderById(Long id) {
        log.debug("Fetching order with ID: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
    }
    
    public Order getOrderByOrderNumber(String orderNumber) {
        log.debug("Fetching order with order number: {}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));
    }
    
    public Order getOrderByTrackingNumber(String trackingNumber) {
        log.debug("Fetching order with tracking number: {}", trackingNumber);
        return orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with tracking number: " + trackingNumber));
    }
    
    public Page<Order> getAllOrders(Pageable pageable) {
        log.debug("Fetching all orders with pagination");
        return orderRepository.findAll(pageable);
    }
    
    public Page<Order> getOrdersWithSpecification(Specification<Order> specification, Pageable pageable) {
        log.debug("Fetching orders with custom specification");
        return orderRepository.findAll(specification, pageable);
    }
    
    // Update operations
    public Order updateOrder(Long id, Order orderDetails) {
        log.info("Updating order with ID: {}", id);
        
        Order existingOrder = getOrderById(id);
        assertCanEditOrder(existingOrder);
        
        // Update fields
        if (orderDetails.getPickupAddress() != null) {
            existingOrder.setPickupAddress(orderDetails.getPickupAddress());
        }
        if (orderDetails.getDeliveryAddress() != null) {
            existingOrder.setDeliveryAddress(orderDetails.getDeliveryAddress());
        }
        if (orderDetails.getPickupLatitude() != null) {
            existingOrder.setPickupLatitude(orderDetails.getPickupLatitude());
        }
        if (orderDetails.getPickupLongitude() != null) {
            existingOrder.setPickupLongitude(orderDetails.getPickupLongitude());
        }
        if (orderDetails.getDeliveryLatitude() != null) {
            existingOrder.setDeliveryLatitude(orderDetails.getDeliveryLatitude());
        }
        if (orderDetails.getDeliveryLongitude() != null) {
            existingOrder.setDeliveryLongitude(orderDetails.getDeliveryLongitude());
        }
        if (orderDetails.getOrderAmount() != null) {
            if (orderDetails.getOrderAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Order amount must be greater than zero");
            }
            existingOrder.setOrderAmount(orderDetails.getOrderAmount());
        }
        if (orderDetails.getDeliveryFee() != null) {
            existingOrder.setDeliveryFee(orderDetails.getDeliveryFee());
        }
        if (orderDetails.getDescription() != null) {
            existingOrder.setDescription(orderDetails.getDescription());
        }
        if (orderDetails.getSpecialInstructions() != null) {
            existingOrder.setSpecialInstructions(orderDetails.getSpecialInstructions());
        }
        if (orderDetails.getNotes() != null) {
            existingOrder.setNotes(orderDetails.getNotes());
        }
        if (orderDetails.getPriority() != null) {
            existingOrder.setPriority(orderDetails.getPriority());
        }
        if (orderDetails.getScheduledPickupTime() != null) {
            existingOrder.setScheduledPickupTime(orderDetails.getScheduledPickupTime());
        }
        if (orderDetails.getScheduledDeliveryTime() != null) {
            existingOrder.setScheduledDeliveryTime(orderDetails.getScheduledDeliveryTime());
        }
        if (orderDetails.getEstimatedDeliveryTime() != null) {
            existingOrder.setEstimatedDeliveryTime(orderDetails.getEstimatedDeliveryTime());
        }
        if (orderDetails.getWeightKg() != null) {
            existingOrder.setWeightKg(orderDetails.getWeightKg());
        }
        if (orderDetails.getPackageDimensions() != null) {
            existingOrder.setPackageDimensions(orderDetails.getPackageDimensions());
        }
        if (orderDetails.getIsFragile() != null) {
            existingOrder.setIsFragile(orderDetails.getIsFragile());
        }
        if (orderDetails.getRequiresSignature() != null) {
            existingOrder.setRequiresSignature(orderDetails.getRequiresSignature());
        }
        if (orderDetails.getDistanceKm() != null) {
            existingOrder.setDistanceKm(orderDetails.getDistanceKm());
        }
        if (orderDetails.getActualPickupTime() != null) {
            existingOrder.setActualPickupTime(orderDetails.getActualPickupTime());
        }
        if (orderDetails.getActualDeliveryTime() != null) {
            existingOrder.setActualDeliveryTime(orderDetails.getActualDeliveryTime());
        }
        if (orderDetails.getStatus() != null && orderDetails.getStatus() != existingOrder.getStatus()) {
            assertValidStatusTransition(existingOrder.getStatus(), orderDetails.getStatus());
            existingOrder.setStatus(orderDetails.getStatus());
        }
        
        // Recalculate total amount if order amount or delivery fee changed
        if (orderDetails.getOrderAmount() != null || orderDetails.getDeliveryFee() != null) {
            BigDecimal orderAmount = existingOrder.getOrderAmount();
            BigDecimal deliveryFee = existingOrder.getDeliveryFee() != null ? existingOrder.getDeliveryFee() : BigDecimal.ZERO;
            if (orderAmount != null) {
                existingOrder.setTotalAmount(orderAmount.add(deliveryFee));
            } else {
                existingOrder.setTotalAmount(deliveryFee);
            }
        }
        
        Order updatedOrder = orderRepository.save(existingOrder);
        log.info("Order updated successfully with ID: {}", updatedOrder.getId());
        
        return updatedOrder;
    }
    
    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        log.info("Updating order status for ID: {} to: {}", id, status);
        
        Order order = getOrderById(id);
        assertCanUpdateStatus(order);
        if (status != order.getStatus()) {
            assertValidStatusTransition(order.getStatus(), status);
        }
        order.setStatus(status);
        
        // Set timestamps based on status
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case PENDING:
            case OPEN_FOR_BID:
            case ASSIGNED:
            case CONFIRMED:
            case IN_PROGRESS:
                // No specific timestamp updates needed for these statuses
                break;
            case PICKED_UP:
                if (order.getActualPickupTime() == null) {
                    order.setActualPickupTime(now);
                }
                break;
            case IN_TRANSIT:
                // Order is in transit, no specific timestamp update
                break;
            case DELIVERED:
                if (order.getActualDeliveryTime() == null) {
                    order.setActualDeliveryTime(now);
                }
                break;
            case CANCELLED:
            case FAILED:
                // No timestamp updates needed for terminal statuses
                break;
        }
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Order status updated successfully for ID: {}", updatedOrder.getId());
        
        realtimeTrackingService.broadcastOrderStatusChanged(updatedOrder);
        
        return updatedOrder;
    }

    /**
     * Allowed order-status transitions. DELIVERED is the terminal "done" state
     * (COMPLETED was removed). CANCELLED/FAILED are reachable from any
     * non-terminal status.
     */
    private static final Map<Order.OrderStatus, Set<Order.OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        Order.OrderStatus.PENDING,      EnumSet.of(Order.OrderStatus.ASSIGNED, Order.OrderStatus.CANCELLED, Order.OrderStatus.FAILED),
        Order.OrderStatus.OPEN_FOR_BID, EnumSet.of(Order.OrderStatus.ASSIGNED, Order.OrderStatus.CANCELLED, Order.OrderStatus.FAILED),
        Order.OrderStatus.ASSIGNED,     EnumSet.of(Order.OrderStatus.CONFIRMED, Order.OrderStatus.IN_PROGRESS, Order.OrderStatus.CANCELLED, Order.OrderStatus.FAILED),
        Order.OrderStatus.CONFIRMED,    EnumSet.of(Order.OrderStatus.IN_PROGRESS, Order.OrderStatus.PICKED_UP, Order.OrderStatus.CANCELLED, Order.OrderStatus.FAILED),
        Order.OrderStatus.IN_PROGRESS,  EnumSet.of(Order.OrderStatus.PICKED_UP, Order.OrderStatus.CANCELLED, Order.OrderStatus.FAILED),
        Order.OrderStatus.PICKED_UP,    EnumSet.of(Order.OrderStatus.IN_TRANSIT, Order.OrderStatus.FAILED),
        Order.OrderStatus.IN_TRANSIT,   EnumSet.of(Order.OrderStatus.DELIVERED, Order.OrderStatus.FAILED),
        // DELIVERED, CANCELLED, FAILED are terminal.
        Order.OrderStatus.DELIVERED,    EnumSet.noneOf(Order.OrderStatus.class),
        Order.OrderStatus.CANCELLED,    EnumSet.noneOf(Order.OrderStatus.class),
        Order.OrderStatus.FAILED,       EnumSet.noneOf(Order.OrderStatus.class)
    );

    private void assertValidStatusTransition(Order.OrderStatus from, Order.OrderStatus to) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalArgumentException("Invalid status transition: " + from + " -> " + to);
        }
    }
    
    public Order assignDeliveryCompany(Long orderId, DeliveryCompany deliveryCompany) {
        log.info("Assigning delivery company {} to order {}", deliveryCompany.getId(), orderId);
        
        Order order = getOrderById(orderId);
        assertCanEditOrder(order);
        order.setDeliveryCompany(deliveryCompany);
        
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.ASSIGNED);
        }
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Delivery company assigned successfully to order {}", updatedOrder.getId());
        
        realtimeTrackingService.broadcastOrderStatusChanged(updatedOrder);
        
        return updatedOrder;
    }
    
    public Order assignDriver(Long orderId, DriverPerson driver) {
        log.info("Assigning driver {} to order {}", driver.getId(), orderId);
        
        Order order = getOrderById(orderId);
        order.setDriverPerson(driver);
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Driver assigned successfully to order {}", updatedOrder.getId());
        
        realtimeTrackingService.broadcastDriverAssigned(updatedOrder);
        
        return updatedOrder;
    }
    
    public Order assignPartnership(Long orderId, Partnership partnership) {
        log.info("Assigning partnership {} to order {}", partnership.getId(), orderId);
        
        Order order = getOrderById(orderId);
        assertCanEditOrder(order);
        order.setPartnership(partnership);
        order.setDeliveryCompany(partnership.getDeliveryCompany());
        
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.ASSIGNED);
        }
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Partnership assigned successfully to order {}", updatedOrder.getId());
        
        realtimeTrackingService.broadcastOrderStatusChanged(updatedOrder);
        
        return updatedOrder;
    }
    
    public Order addRatingAndReview(Long orderId, Integer rating, String review) {
        log.info("Adding rating {} and review to order {}", rating, orderId);
        
        Order order = getOrderById(orderId);
        assertCanEditOrder(order);
        
        if (!order.isCompleted()) {
            throw new IllegalStateException("Cannot rate an order that is not completed");
        }
        
        order.setRating(rating);
        order.setReview(review);
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Rating and review added successfully to order {}", updatedOrder.getId());
        
        return updatedOrder;
    }
    
    public Order cancelOrder(Long orderId, String cancellationReason) {
        log.info("Cancelling order {} with reason: {}", orderId, cancellationReason);
        
        Order order = getOrderById(orderId);
        assertCanEditOrder(order);
        
        if (order.isCompleted() || order.isCancelled()) {
            throw new IllegalStateException("Cannot cancel an order that is already completed or cancelled");
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancellationReason(cancellationReason);
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Order cancelled successfully with ID: {}", updatedOrder.getId());
        
        realtimeTrackingService.broadcastOrderStatusChanged(updatedOrder);
        
        return updatedOrder;
    }
    
    // Delete operations
    public void deleteOrder(Long id) {
        log.info("Deleting order with ID: {}", id);
        
        Order order = getOrderById(id);
        orderRepository.delete(order);
        
        log.info("Order deleted successfully with ID: {}", id);
    }
    
    // Query operations
    public List<Order> getOrdersByVendorCompany(VendorCompany vendorCompany) {
        log.debug("Fetching orders for vendor company: {}", vendorCompany.getId());
        return orderRepository.findByVendorCompany(vendorCompany);
    }
    
    public List<Order> getOrdersByDeliveryCompany(DeliveryCompany deliveryCompany) {
        log.debug("Fetching orders for delivery company: {}", deliveryCompany.getId());
        return orderRepository.findByDeliveryCompany(deliveryCompany);
    }

    public List<Order> getOrdersByVendorOwner(VendorOwner owner) {
        log.debug("Fetching orders for vendor owner: {}", owner.getId());
        return orderRepository.findByVendorCompanyOwner(owner);
    }

    public List<Order> getOrdersByDeliveryOwner(DeliveryOwner owner) {
        log.debug("Fetching orders for delivery owner: {}", owner.getId());
        return orderRepository.findByDeliveryCompanyOwner(owner);
    }
    
    public List<Order> getOrdersByCustomer(CustomerUser customer) {
        log.debug("Fetching orders for customer: {}", customer.getId());
        return orderRepository.findByCustomerUser(customer);
    }
    
    public List<Order> getOrdersByDriver(DriverPerson driver) {
        log.debug("Fetching orders for driver: {}", driver.getId());
        return orderRepository.findByDriverPerson(driver);
    }

    public List<Order> getOrdersCreatedBy(Long userId) {
        log.debug("Fetching orders created by user: {}", userId);
        return orderRepository.findByCreatedByUserId(userId);
    }
    
    public List<Order> getOrdersByPartnership(Partnership partnership) {
        log.debug("Fetching orders for partnership: {}", partnership.getId());
        return orderRepository.findByPartnership(partnership);
    }
    
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        log.debug("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status);
    }

    /**
     * Orders currently open for bidding. Visible to delivery owners and
     * independent drivers so they can place bids on the pool.
     */
    public List<Order> getOpenForBidOrders() {
        log.debug("Fetching open-for-bid orders");
        return orderRepository.findByStatus(Order.OrderStatus.OPEN_FOR_BID);
    }
    
    public List<Order> getPendingUnassignedOrders() {
        log.debug("Fetching pending unassigned orders");
        return orderRepository.findPendingUnassignedOrders();
    }
    
    public List<Order> getOverdueOrders() {
        log.debug("Fetching overdue orders");
        return orderRepository.findOverdueOrders(LocalDateTime.now());
    }
    
    public List<Order> getActiveUrgentOrders() {
        log.debug("Fetching active urgent orders");
        return orderRepository.findActiveUrgentOrders();
    }

    public List<Order> getOrdersByStatusForOwner(Order.OrderStatus status) {
        User currentUser = securityService.getCurrentUser();
        switch (currentUser.getRole()) {
            case VENDOR_OWNER:
                return orderRepository.findByVendorCompanyOwnerAndStatus(securityService.getCurrentVendorOwner(), status);
            case DELIVERY_OWNER:
                return orderRepository.findByDeliveryCompanyOwnerAndStatus(securityService.getCurrentDeliveryOwner(), status);
            default:
                return orderRepository.findByStatus(status);
        }
    }

    public List<Order> getOverdueOrdersForOwner() {
        return securityService.filterOrdersForUser(getOverdueOrders());
    }

    public List<Order> getActiveUrgentOrdersForOwner() {
        return securityService.filterOrdersForUser(getActiveUrgentOrders());
    }
    
    // Statistics operations
    public long countOrdersByVendorCompany(VendorCompany vendorCompany) {
        return orderRepository.countByVendorCompany(vendorCompany);
    }
    
    public long countOrdersByDeliveryCompany(DeliveryCompany deliveryCompany) {
        return orderRepository.countByDeliveryCompany(deliveryCompany);
    }
    
    public long countOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }
    
    public BigDecimal calculateTotalRevenueByVendorCompany(VendorCompany vendorCompany) {
        BigDecimal revenue = orderRepository.calculateTotalRevenueByVendorCompany(vendorCompany);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public BigDecimal calculateTotalDeliveryRevenueByDeliveryCompany(DeliveryCompany deliveryCompany) {
        BigDecimal revenue = orderRepository.calculateTotalDeliveryRevenueByDeliveryCompany(deliveryCompany);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public Double calculateAverageRatingByDeliveryCompany(DeliveryCompany deliveryCompany) {
        return orderRepository.calculateAverageRatingByDeliveryCompany(deliveryCompany);
    }
    
    // Utility methods
    public boolean existsByOrderNumber(String orderNumber) {
        return orderRepository.existsByOrderNumber(orderNumber);
    }
    
    public boolean existsByTrackingNumber(String trackingNumber) {
        return orderRepository.existsByTrackingNumber(trackingNumber);
    }

    /**
     * Grants write access to an order for:
     * <ul>
     *   <li>the SUPER_ADMIN</li>
     *   <li>the VENDOR_OWNER who owns the order's vendor company</li>
     *   <li>the CLIENT who placed the order</li>
     * </ul>
     */
    private void assertCanEditOrder(Order order) {        User currentUser = securityService.getCurrentUser();
        if (currentUser.getRole() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (order.getCreatedByUserId() != null && currentUser.getId().equals(order.getCreatedByUserId())) {
            return;
        }
        if (currentUser.getRole() == User.Role.CLIENT
                && order.getCustomerUser() != null
                && currentUser.getId().equals(order.getCustomerUser().getId())) {
            return;
        }
        if (currentUser.getRole() == User.Role.VENDOR_OWNER
                && order.getVendorCompany() != null
                && order.getVendorCompany().getOwner() != null
                && currentUser.getId().equals(order.getVendorCompany().getOwner().getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to modify this order");
    }

    /**
     * Grants read access to an order for the SUPER_ADMIN, the vendor company
     * owner, the delivery company owner, the customer who placed the order,
     * and the driver assigned to the order. Orders that are OPEN_FOR_BID are
     * readable by delivery owners and independent drivers so they can bid.
     */
    public void assertCanReadOrder(Order order) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser.getRole() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (order.getCreatedByUserId() != null && currentUser.getId().equals(order.getCreatedByUserId())) {
            return;
        }
        boolean isOpenForBid = order.getStatus() == Order.OrderStatus.OPEN_FOR_BID;
        Long userId = currentUser.getId();
        if (currentUser.getRole() == User.Role.CLIENT
                && order.getCustomerUser() != null
                && userId.equals(order.getCustomerUser().getId())) {
            return;
        }
        if (currentUser.getRole() == User.Role.DRIVER) {
            if (order.getDriverPerson() != null && userId.equals(order.getDriverPerson().getId())) {
                return;
            }
            if (isOpenForBid) {
                return; // any independent/company driver may view and bid on pooled orders
            }
        }
        if (currentUser.getRole() == User.Role.VENDOR_OWNER
                && order.getVendorCompany() != null
                && order.getVendorCompany().getOwner() != null
                && userId.equals(order.getVendorCompany().getOwner().getId())) {
            return;
        }
        if (currentUser.getRole() == User.Role.DELIVERY_OWNER) {
            if (order.getDeliveryCompany() != null
                    && order.getDeliveryCompany().getOwner() != null
                    && userId.equals(order.getDeliveryCompany().getOwner().getId())) {
                return;
            }
            if (isOpenForBid) {
                return; // delivery owners may view pooled orders they bid on
            }
        }
        throw new AccessDeniedException("You do not have permission to view this order");
    }

    /**
     * Grants status-update access to an order for the SUPER_ADMIN, the
     * DELIVERY_OWNER who owns the order's delivery company, and the DRIVER
     * assigned to the order.
     */
    private void assertCanUpdateStatus(Order order) {
        User currentUser = securityService.getCurrentUser();
        if (currentUser.getRole() == User.Role.SUPER_ADMIN) {
            return;
        }
        Long userId = currentUser.getId();
        if (currentUser.getRole() == User.Role.DRIVER
                && order.getDriverPerson() != null
                && userId.equals(order.getDriverPerson().getId())) {
            return;
        }
        if (currentUser.getRole() == User.Role.DELIVERY_OWNER
                && order.getDeliveryCompany() != null
                && order.getDeliveryCompany().getOwner() != null
                && userId.equals(order.getDeliveryCompany().getOwner().getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to update this order's status");
    }
    
    private String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + timestamp + "-" + randomSuffix;
    }
    
    private String generateTrackingNumber() {
        String prefix = "TRK";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + "-" + timestamp + "-" + randomSuffix;
    }
}