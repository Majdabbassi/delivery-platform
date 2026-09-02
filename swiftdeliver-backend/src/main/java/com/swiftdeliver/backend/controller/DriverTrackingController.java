package com.swiftdeliver.backend.controller;

import com.swiftdeliver.backend.dto.OrderLocationUpdate;
import com.swiftdeliver.backend.dto.OrderRealtimeEvent;
import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.entity.User;
import com.swiftdeliver.backend.service.OrderService;
import com.swiftdeliver.backend.service.RealtimeTrackingService;
import com.swiftdeliver.backend.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Slf4j
public class DriverTrackingController {

    private final OrderService orderService;
    private final RealtimeTrackingService realtimeTrackingService;
    private final SecurityService securityService;

    @PostMapping("/orders/{orderId}/location")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DRIVER')")
    @Operation(summary = "Update order location", description = "Publishes a live driver location update for an order and broadcasts it to subscribed clients")
    public ResponseEntity<Order> updateOrderLocation(@PathVariable Long orderId,
                                                     @Valid @RequestBody OrderLocationUpdate locationUpdate) {
        log.info("Updating location for order {}: lat={}, lng={}", orderId, locationUpdate.getLatitude(), locationUpdate.getLongitude());

        Order order = orderService.getOrderById(orderId);

        // Lock: a DRIVER may only post location for an order that is assigned to them.
        if (securityService.getCurrentUser().getRole() != User.Role.SUPER_ADMIN) {
            Long driverId = securityService.getCurrentUser().getId();
            boolean assigned = order.getDriverPerson() != null
                    && driverId.equals(order.getDriverPerson().getId());
            if (!assigned) {
                throw new AccessDeniedException("You are not the assigned driver for this order");
            }
        }

        realtimeTrackingService.broadcastDriverLocation(
                orderId,
                order.getOrderNumber(),
                locationUpdate.getLatitude(),
                locationUpdate.getLongitude(),
                locationUpdate.getSpeedKmh(),
                order.getDriverPerson() != null ? order.getDriverPerson().getId() : null);

        return ResponseEntity.ok(order);
    }

    @GetMapping("/orders/{orderId}/location")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DRIVER') or hasRole('CLIENT')")
    @Operation(summary = "Get last known order location", description = "Returns the most recent driver location broadcast for an order, if any")
    public ResponseEntity<OrderRealtimeEvent> getOrderLocation(@PathVariable Long orderId) {
        OrderRealtimeEvent location = realtimeTrackingService.getLastLocation(orderId);
        if (location == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(location);
    }
}