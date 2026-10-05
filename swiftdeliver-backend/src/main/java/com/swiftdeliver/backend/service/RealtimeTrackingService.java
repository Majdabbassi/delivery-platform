package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.dto.OrderRealtimeEvent;
import com.swiftdeliver.backend.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeTrackingService {

    private static final String TOPIC_ORDER = "/topic/orders";
    private static final String TOPIC_ORDER_LOCATION = "/topic/orders/location";
    private static final String TOPIC_USER = "/topic/users";

    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<Long, OrderRealtimeEvent> lastLocations = new ConcurrentHashMap<>();

    public void broadcastOrderCreated(Order order) {
        OrderRealtimeEvent event = baseEvent(order, "ORDER_CREATED");
        send(event);
    }

    public void broadcastOrderStatusChanged(Order order) {
        OrderRealtimeEvent event = baseEvent(order, "ORDER_STATUS_CHANGED");
        send(event);
    }

    public void broadcastDriverAssigned(Order order) {
        OrderRealtimeEvent event = baseEvent(order, "DRIVER_ASSIGNED");
        send(event);
    }

    public void broadcastDriverLocation(Order order, Double latitude, Double longitude, Double speedKmh) {
        Long orderId = order.getId();
        Long driverPersonId = order.getDriverPerson() != null ? order.getDriverPerson().getId() : null;
        OrderRealtimeEvent event = OrderRealtimeEvent.builder()
                .type("DRIVER_LOCATION_UPDATE")
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .latitude(latitude)
                .longitude(longitude)
                .speedKmh(speedKmh)
                .driverPersonId(driverPersonId)
                .timestamp(LocalDateTime.now())
                .involvedUserIds(involvedUserIds(order))
                .build();

        lastLocations.put(orderId, event);
        messagingTemplate.convertAndSend(TOPIC_ORDER_LOCATION, event);   // admins only
        messagingTemplate.convertAndSend(orderTopic(orderId), event);    // anyone allowed to read the order
        // The people involved in the order get it on their private topic, which is all the
        // non-admin frontend subscribes to.
        for (Long userId : event.getInvolvedUserIds()) {
            messagingTemplate.convertAndSend(userTopic(userId), event);
        }
    }

    public OrderRealtimeEvent getLastLocation(Long orderId) {
        return lastLocations.get(orderId);
    }

    private OrderRealtimeEvent baseEvent(Order order, String type) {
        return OrderRealtimeEvent.builder()
                .type(type)
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .trackingNumber(order.getTrackingNumber())
                .status(order.getStatus())
                .driverPersonId(order.getDriverPerson() != null ? order.getDriverPerson().getId() : null)
                .timestamp(LocalDateTime.now())
                .involvedUserIds(involvedUserIds(order))
                .build();
    }

    /**
     * Collects the principal ids (User subclass ids) involved in an order so the
     * event can be routed to their private per-user topics.
     */
    private Set<Long> involvedUserIds(Order order) {
        Set<Long> ids = new HashSet<>();
        if (order.getCustomerUser() != null) {
            ids.add(order.getCustomerUser().getId());
        }
        if (order.getDriverPerson() != null) {
            ids.add(order.getDriverPerson().getId());
        }
        if (order.getVendorCompany() != null && order.getVendorCompany().getOwner() != null) {
            ids.add(order.getVendorCompany().getOwner().getId());
        }
        if (order.getDeliveryCompany() != null && order.getDeliveryCompany().getOwner() != null) {
            ids.add(order.getDeliveryCompany().getOwner().getId());
        }
        return ids;
    }

    private void send(OrderRealtimeEvent event) {
        messagingTemplate.convertAndSend(TOPIC_ORDER, event);
        if (event.getOrderId() != null) {
            messagingTemplate.convertAndSend(orderTopic(event.getOrderId()), event);
        }
        // Fan out to per-user private topics.
        if (event.getInvolvedUserIds() != null) {
            for (Long userId : event.getInvolvedUserIds()) {
                messagingTemplate.convertAndSend(userTopic(userId), event);
            }
        }
    }

    private String orderTopic(Long orderId) {
        return TOPIC_ORDER + "/" + orderId;
    }

    private String userTopic(Long userId) {
        return TOPIC_USER + "/" + userId;
    }
}
