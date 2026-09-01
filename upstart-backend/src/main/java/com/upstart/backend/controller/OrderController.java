package com.upstart.backend.controller;

import com.upstart.backend.entity.*;
import com.upstart.backend.service.OrderService;
import com.upstart.backend.service.DeliveryCompanyService;
import com.upstart.backend.service.PartnershipService;
import com.upstart.backend.service.SecurityService;
import com.upstart.backend.service.OrderAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    private final DeliveryCompanyService deliveryCompanyService;
    private final PartnershipService partnershipService;
    private final SecurityService securityService;
    
    // Create operations
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('CLIENT')")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody Order order) {
        log.info("Creating new order for vendor company: {}", order.getVendorCompany().getId());
        Order createdOrder = orderService.createOrder(order);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }
    
    // Read operations
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER') or hasRole('CLIENT') or hasRole('DRIVER')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        log.debug("Fetching order with ID: {}", id);
        Order order = orderService.getOrderById(id);
        orderService.assertCanReadOrder(order);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/order-number/{orderNumber}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER') or hasRole('CLIENT') or hasRole('DRIVER')")
    public ResponseEntity<Order> getOrderByOrderNumber(@PathVariable String orderNumber) {
        log.debug("Fetching order with order number: {}", orderNumber);
        Order order = orderService.getOrderByOrderNumber(orderNumber);
        orderService.assertCanReadOrder(order);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<Order> getOrderByTrackingNumber(@PathVariable String trackingNumber) {
        log.debug("Fetching order with tracking number: {}", trackingNumber);
        Order order = orderService.getOrderByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<Order>> getAllOrders(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching all orders with pagination");
        Page<Order> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }
    
    // Update operations
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @Valid @RequestBody Order orderDetails) {
        log.info("Updating order with ID: {}", id);
        Order updatedOrder = orderService.updateOrder(id, orderDetails);
        return ResponseEntity.ok(updatedOrder);
    }
    
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER') or hasRole('DRIVER')")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam Order.OrderStatus status) {
        log.info("Updating order status for ID: {} to: {}", id, status);
        Order updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }
    
    @PatchMapping("/{id}/assign-delivery-company")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Order> assignDeliveryCompany(@PathVariable Long id, @RequestParam Long deliveryCompanyId) {
        log.info("Assigning delivery company {} to order {}", deliveryCompanyId, id);
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        Order updatedOrder = orderService.assignDeliveryCompany(id, deliveryCompany);
        return ResponseEntity.ok(updatedOrder);
    }
    
    @PatchMapping("/{id}/assign-partnership")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Order> assignPartnership(@PathVariable Long id, @RequestParam Long partnershipId) {
        log.info("Assigning partnership {} to order {}", partnershipId, id);
        Partnership partnership = partnershipService.getPartnershipById(partnershipId);
        Order updatedOrder = orderService.assignPartnership(id, partnership);
        return ResponseEntity.ok(updatedOrder);
    }
    
    @PatchMapping("/{id}/rating")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CLIENT')")
    public ResponseEntity<Order> addRatingAndReview(@PathVariable Long id, 
                                                   @RequestParam Integer rating, 
                                                   @RequestParam(required = false) String review) {
        log.info("Adding rating {} and review to order {}", rating, id);
        Order updatedOrder = orderService.addRatingAndReview(id, rating, review);
        return ResponseEntity.ok(updatedOrder);
    }
    
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('CLIENT')")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id, @RequestParam String cancellationReason) {
        log.info("Cancelling order {} with reason: {}", id, cancellationReason);
        Order updatedOrder = orderService.cancelOrder(id, cancellationReason);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{id}/auto-assign")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('CLIENT')")
    public ResponseEntity<Order> autoAssignOrder(@PathVariable Long id) {
        log.info("Auto-assigning order with ID: {}", id);
        Order assigned = orderService.autoAssignOrder(id);
        return ResponseEntity.ok(assigned);
    }

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Order> reassignOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        log.info("Reassigning order with ID: {}", id);
        Order reassigned = orderService.reassignOrder(id, reason != null ? reason : "Manual reassignment");
        return ResponseEntity.ok(reassigned);
    }

    @GetMapping("/assignment/statistics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<OrderAssignmentService.AssignmentStatistics> getAssignmentStatistics() {
        return ResponseEntity.ok(orderService.getAssignmentStatistics());
    }
    
    // Delete operations
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Deleting order with ID: {}", id);
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
    
    // Query operations by company
    @GetMapping("/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<List<Order>> getOrdersByVendorCompany(@PathVariable Long vendorCompanyId) {
        log.debug("Fetching orders for vendor company: {}", vendorCompanyId);
        VendorCompany vendorCompany = securityService.getOwnedVendorCompanyOrThrow(vendorCompanyId);
        List<Order> orders = orderService.getOrdersByVendorCompany(vendorCompany);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Order>> getOrdersByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        log.debug("Fetching orders for delivery company: {}", deliveryCompanyId);
        DeliveryCompany deliveryCompany = securityService.getOwnedDeliveryCompanyOrThrow(deliveryCompanyId);
        List<Order> orders = orderService.getOrdersByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/my")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER') or hasRole('DRIVER') or hasRole('CLIENT')")
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        log.debug("Fetching orders for current user: {}", authentication.getName());
        List<Order> orders;
        User currentUser = securityService.getCurrentUser();
        switch (currentUser.getRole()) {
            case DRIVER:
                orders = orderService.getOrdersByDriver(securityService.getCurrentDriverPerson());
                break;
            case CLIENT:
                orders = orderService.getOrdersByCustomer(securityService.getCurrentCustomerUser());
                break;
            case VENDOR_OWNER:
                orders = orderService.getOrdersByVendorOwner(securityService.getCurrentVendorOwner());
                break;
            case DELIVERY_OWNER:
                orders = orderService.getOrdersByDeliveryOwner(securityService.getCurrentDeliveryOwner());
                break;
            case SUPER_ADMIN:
            default:
                orders = orderService.getAllOrders(org.springframework.data.domain.Pageable.unpaged()).getContent();
                break;
        }
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/partnership/{partnershipId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Order>> getOrdersByPartnership(@PathVariable Long partnershipId) {
        log.debug("Fetching orders for partnership: {}", partnershipId);
        Partnership partnership = partnershipService.getPartnershipById(partnershipId);
        List<Order> orders = orderService.getOrdersByPartnership(partnership);
        orders = securityService.filterOrdersForUser(orders);
        return ResponseEntity.ok(orders);
    }
    
    // Query operations by status
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        log.debug("Fetching orders with status: {}", status);
        List<Order> orders = orderService.getOrdersByStatusForOwner(status);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/pending-unassigned")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Order>> getPendingUnassignedOrders() {
        log.debug("Fetching pending unassigned orders");
        List<Order> orders = orderService.getPendingUnassignedOrders();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Order>> getOverdueOrders() {
        log.debug("Fetching overdue orders");
        List<Order> orders = orderService.getOverdueOrdersForOwner();
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/urgent")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Order>> getActiveUrgentOrders() {
        log.debug("Fetching active urgent orders");
        List<Order> orders = orderService.getActiveUrgentOrdersForOwner();
        return ResponseEntity.ok(orders);
    }
    
    // Statistics operations
    @GetMapping("/stats/count/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Long> countOrdersByVendorCompany(@PathVariable Long vendorCompanyId) {
        VendorCompany vendorCompany = securityService.getOwnedVendorCompanyOrThrow(vendorCompanyId);
        long count = orderService.countOrdersByVendorCompany(vendorCompany);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/count/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Long> countOrdersByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        DeliveryCompany deliveryCompany = securityService.getOwnedDeliveryCompanyOrThrow(deliveryCompanyId);
        long count = orderService.countOrdersByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/count/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countOrdersByStatus(@PathVariable Order.OrderStatus status) {
        long count = orderService.countOrdersByStatus(status);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/revenue/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<BigDecimal> getTotalRevenueByVendorCompany(@PathVariable Long vendorCompanyId) {
        VendorCompany vendorCompany = securityService.getOwnedVendorCompanyOrThrow(vendorCompanyId);
        BigDecimal revenue = orderService.calculateTotalRevenueByVendorCompany(vendorCompany);
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/stats/revenue/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<BigDecimal> getTotalDeliveryRevenueByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        DeliveryCompany deliveryCompany = securityService.getOwnedDeliveryCompanyOrThrow(deliveryCompanyId);
        BigDecimal revenue = orderService.calculateTotalDeliveryRevenueByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/stats/rating/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Double> getAverageRatingByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        DeliveryCompany deliveryCompany = securityService.getOwnedDeliveryCompanyOrThrow(deliveryCompanyId);
        Double rating = orderService.calculateAverageRatingByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(rating);
    }
    
    // Utility operations
    @GetMapping("/exists/order-number/{orderNumber}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Boolean> existsByOrderNumber(@PathVariable String orderNumber) {
        boolean exists = orderService.existsByOrderNumber(orderNumber);
        return ResponseEntity.ok(exists);
    }
    
    @GetMapping("/exists/tracking-number/{trackingNumber}")
    public ResponseEntity<Boolean> existsByTrackingNumber(@PathVariable String trackingNumber) {
        boolean exists = orderService.existsByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(exists);
    }
}