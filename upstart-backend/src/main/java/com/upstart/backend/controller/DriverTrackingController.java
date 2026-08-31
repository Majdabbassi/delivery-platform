package com.upstart.backend.controller;

import com.upstart.backend.dto.OrderLocationUpdate;
import com.upstart.backend.dto.OrderRealtimeEvent;
import com.upstart.backend.entity.Order;
import com.upstart.backend.service.OrderService;
import com.upstart.backend.service.RealtimeTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Slf4j
public class DriverTrackingController {

    private final OrderService orderService;
    private final RealtimeTrackingService realtimeTrackingService;

    @PostMapping("/orders/{orderId}/location")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DRIVER')")
    @Operation(summary = "Update order location", description = "Publishes a live driver location update for an order and broadcasts it to subscribed clients")
    public ResponseEntity<Order> updateOrderLocation(@PathVariable Long orderId,
                                                     @Valid @RequestBody OrderLocationUpdate locationUpdate) {
        log.info("Updating location for order {}: lat={}, lng={}", orderId, locationUpdate.getLatitude(), locationUpdate.getLongitude());

        Order order = orderService.getOrderById(orderId);

        realtimeTrackingService.broadcastDriverLocation(
                orderId,
                order.getOrderNumber(),
                locationUpdate.getLatitude(),
                locationUpdate.getLongitude(),
                locationUpdate.getSpeedKmh(),
                locationUpdate.getDriverPersonId());

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