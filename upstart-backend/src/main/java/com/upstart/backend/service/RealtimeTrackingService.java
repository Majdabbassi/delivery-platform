package com.upstart.backend.service;

import com.upstart.backend.dto.OrderRealtimeEvent;
import com.upstart.backend.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeTrackingService {

    private static final String TOPIC_ORDER = "/topic/orders";
    private static final String TOPIC_ORDER_LOCATION = "/topic/orders/location";

    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<Long, OrderRealtimeEvent> lastLocations = new ConcurrentHashMap<>();

    public void broadcastOrderCreated(Order order) {
        OrderRealtimeEvent event = OrderRealtimeEvent.builder()
                .type("ORDER_CREATED")
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .trackingNumber(order.getTrackingNumber())
                .status(order.getStatus())
                .driverPersonId(order.getDriverPerson() != null ? order.getDriverPerson().getId() : null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        send(event);
    }

    public void broadcastOrderStatusChanged(Order order) {
        OrderRealtimeEvent event = OrderRealtimeEvent.builder()
                .type("ORDER_STATUS_CHANGED")
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .trackingNumber(order.getTrackingNumber())
                .status(order.getStatus())
                .driverPersonId(order.getDriverPerson() != null ? order.getDriverPerson().getId() : null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        send(event);
    }

    public void broadcastDriverAssigned(Order order) {
        OrderRealtimeEvent event = OrderRealtimeEvent.builder()
                .type("DRIVER_ASSIGNED")
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .trackingNumber(order.getTrackingNumber())
                .status(order.getStatus())
                .driverPersonId(order.getDriverPerson() != null ? order.getDriverPerson().getId() : null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        send(event);
    }

    public void broadcastDriverLocation(Long orderId, String orderNumber, Double latitude,
                                        Double longitude, Double speedKmh, Long driverPersonId) {
        OrderRealtimeEvent event = OrderRealtimeEvent.builder()
                .type("DRIVER_LOCATION_UPDATE")
                .orderId(orderId)
                .orderNumber(orderNumber)
                .latitude(latitude)
                .longitude(longitude)
                .speedKmh(speedKmh)
                .driverPersonId(driverPersonId)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        lastLocations.put(orderId, event);
        messagingTemplate.convertAndSend(TOPIC_ORDER_LOCATION, event);
        messagingTemplate.convertAndSend(orderTopic(orderId), event);
    }

    public OrderRealtimeEvent getLastLocation(Long orderId) {
        return lastLocations.get(orderId);
    }

    private void send(OrderRealtimeEvent event) {
        messagingTemplate.convertAndSend(TOPIC_ORDER, event);
        if (event.getOrderId() != null) {
            messagingTemplate.convertAndSend(orderTopic(event.getOrderId()), event);
        }
    }

    private String orderTopic(Long orderId) {
        return TOPIC_ORDER + "/" + orderId;
    }
}