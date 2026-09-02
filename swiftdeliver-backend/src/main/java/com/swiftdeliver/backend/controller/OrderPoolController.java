package com.swiftdeliver.backend.controller;

import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.repository.DeliveryCompanyRepository;
import com.swiftdeliver.backend.service.OrderPoolService;
import com.swiftdeliver.backend.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pool")
@RequiredArgsConstructor
@Slf4j
public class OrderPoolController {

    private final OrderPoolService orderPoolService;
    private final SecurityService securityService;
    private final DeliveryCompanyRepository deliveryCompanyRepository;

    @GetMapping("/orders")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Page<Order>> getAvailableOrders(
            @RequestParam(required = false) Long deliveryCompanyId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long companyId = resolveDeliveryCompanyId(deliveryCompanyId);
        Page<Order> orders = orderPoolService.getAvailableOrders(companyId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/filtered")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Order>> getFilteredOrders(
            @RequestParam(required = false) Long deliveryCompanyId,
            @RequestParam(required = false) java.math.BigDecimal minValue,
            @RequestParam(required = false) java.math.BigDecimal maxValue,
            @RequestParam(required = false) Order.OrderPriority priority,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "50") int limit) {
        Long companyId = resolveDeliveryCompanyId(deliveryCompanyId);
        OrderPoolService.OrderPoolFilter filter = new OrderPoolService.OrderPoolFilter();
        filter.setMinValue(minValue);
        filter.setMaxValue(maxValue);
        filter.setPriority(priority);
        filter.setSortBy(sortBy != null ? sortBy : "created");
        filter.setSortDirection(sortDirection != null ? sortDirection : "desc");
        filter.setLimit(limit);
        List<Order> orders = orderPoolService.getFilteredOrders(companyId, filter);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/recommended")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Order>> getRecommendedOrders(
            @RequestParam(required = false) Long deliveryCompanyId,
            @RequestParam(defaultValue = "20") int limit) {
        Long companyId = resolveDeliveryCompanyId(deliveryCompanyId);
        List<Order> orders = orderPoolService.getRecommendedOrders(companyId, limit);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<OrderPoolService.OrderPoolStatistics> getPoolStats(
            @RequestParam(required = false) Long deliveryCompanyId) {
        Long companyId = resolveDeliveryCompanyId(deliveryCompanyId);
        return ResponseEntity.ok(orderPoolService.getPoolStatistics(companyId));
    }

    @PostMapping("/orders/{orderId}/viewed")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Void> markOrderViewed(@RequestParam(required = false) Long deliveryCompanyId,
                                                @PathVariable Long orderId) {
        Long companyId = resolveDeliveryCompanyId(deliveryCompanyId);
        orderPoolService.markOrderAsViewed(companyId, orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPoolHealth() {
        return ResponseEntity.ok(orderPoolService.getPoolHealthMetrics());
    }

    private Long resolveDeliveryCompanyId(Long requested) {
        if (securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.SUPER_ADMIN) {
            if (requested == null) {
                throw new IllegalArgumentException("deliveryCompanyId is required for SUPER_ADMIN");
            }
            return requested;
        }
        return deliveryCompanyRepository.findByOwnerId(securityService.getCurrentDeliveryOwner().getId())
                .stream()
                .map(DeliveryCompany::getId)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Current user owns no delivery company"));
    }
}
