package com.upstart.backend.service;

import com.upstart.backend.dto.OrderRealtimeEvent;
import com.upstart.backend.entity.CustomerUser;
import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.DeliveryOwner;
import com.upstart.backend.entity.DriverPerson;
import com.upstart.backend.entity.Order;
import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.entity.VendorOwner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeTrackingServiceTest {

    private SimpMessagingTemplate messagingTemplate;

    private Order buildOrder() {
        CustomerUser customer = new CustomerUser();
        customer.setId(1L);
        DriverPerson driver = new DriverPerson();
        driver.setId(2L);
        VendorOwner vendorOwner = new VendorOwner();
        vendorOwner.setId(3L);
        vendorOwner.setUsername("vo");
        vendorOwner.setRole(com.upstart.backend.entity.User.Role.VENDOR_OWNER);
        VendorCompany vendor = new VendorCompany();
        vendor.setId(10L);
        vendor.setOwner(vendorOwner);
        DeliveryOwner deliveryOwner = new DeliveryOwner();
        deliveryOwner.setId(4L);
        deliveryOwner.setUsername("do");
        deliveryOwner.setRole(com.upstart.backend.entity.User.Role.DELIVERY_OWNER);
        DeliveryCompany delivery = new DeliveryCompany();
        delivery.setId(20L);
        delivery.setOwner(deliveryOwner);

        Order order = new Order();
        order.setId(100L);
        order.setOrderNumber("ORD-100");
        order.setTrackingNumber("TRK-100");
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCustomerUser(customer);
        order.setDriverPerson(driver);
        order.setVendorCompany(vendor);
        order.setDeliveryCompany(delivery);
        return order;
    }

    @Test
    void broadcastOrderCreated_fansOutToGlobalOrderAndUserTopics() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        RealtimeTrackingService service = new RealtimeTrackingService(messagingTemplate);

        Order order = buildOrder();
        service.broadcastOrderCreated(order);

        verify(messagingTemplate).convertAndSend(eq("/topic/orders"), anyEvent());
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/100"), anyEvent());

        ArgumentCaptor<OrderRealtimeEvent> captor = ArgumentCaptor.forClass(OrderRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/users/1"), captor.capture());
        Set<Long> involved = captor.getValue().getInvolvedUserIds();
        assertEquals(Set.of(1L, 2L, 3L, 4L), involved);
    }

    @Test
    void broadcastOrderCreated_routesToEveryInvolvedUserTopic() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        RealtimeTrackingService service = new RealtimeTrackingService(messagingTemplate);

        service.broadcastOrderCreated(buildOrder());

        verify(messagingTemplate).convertAndSend(eq("/topic/users/1"), anyEvent());
        verify(messagingTemplate).convertAndSend(eq("/topic/users/2"), anyEvent());
        verify(messagingTemplate).convertAndSend(eq("/topic/users/3"), anyEvent());
        verify(messagingTemplate).convertAndSend(eq("/topic/users/4"), anyEvent());
    }

    @Test
    void broadcastOrderStatusChanged_includesInvolvedUsers() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        RealtimeTrackingService service = new RealtimeTrackingService(messagingTemplate);

        service.broadcastOrderStatusChanged(buildOrder());

        ArgumentCaptor<OrderRealtimeEvent> captor = ArgumentCaptor.forClass(OrderRealtimeEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/users/1"), captor.capture());
        assertEquals("ORDER_STATUS_CHANGED", captor.getValue().getType());
        assertEquals(Set.of(1L, 2L, 3L, 4L), captor.getValue().getInvolvedUserIds());
    }

    private OrderRealtimeEvent anyEvent() {
        return org.mockito.ArgumentMatchers.any(OrderRealtimeEvent.class);
    }
}
