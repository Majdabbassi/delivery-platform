package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.CustomerUser;
import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.DeliveryOwner;
import com.swiftdeliver.backend.entity.DriverPerson;
import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.entity.User;
import com.swiftdeliver.backend.entity.VendorCompany;
import com.swiftdeliver.backend.entity.VendorOwner;
import com.swiftdeliver.backend.repository.OrderRepository;
import com.swiftdeliver.backend.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderServiceAuthorizationTest {

    @Mock private OrderRepository orderRepository;
    @Mock private RealtimeTrackingService realtimeTrackingService;
    @Mock private SecurityService securityService;
    @Mock private OrderAssignmentService orderAssignmentService;
    @Mock private OrderPoolService orderPoolService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, realtimeTrackingService, securityService,
                orderAssignmentService, orderPoolService);
        ReflectionTestUtils.setField(orderService, "autoAssignOnCreate", false);
        ReflectionTestUtils.setField(orderService, "orderCreateLimitPerMinute", 1000);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuth(User mockUser) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser, null, List.of()));
        lenient().when(securityService.getCurrentUser()).thenReturn(mockUser);
    }

    private User mockUser(Long id, User.Role role) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getRole()).thenReturn(role);
        lenient().when(user.getUsername()).thenReturn("user-" + id);
        return user;
    }

    private Order buildOrder(Long id, Long createdByUserId) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber("ORD-TEST-" + id);
        order.setCreatedByUserId(createdByUserId);
        order.setPickupAddress("A");
        order.setDeliveryAddress("B");
        return order;
    }

    // ─── assertCanReadOrder ───────────────────────────────────────────────

    @Test
    @DisplayName("assertCanReadOrder: SUPER_ADMIN can read any order")
    void readOrder_superAdmin_allowed() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);

        assertThatCode(() -> orderService.assertCanReadOrder(order)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrder: creator can read their own order")
    void readOrder_creator_allowed() {
        User creator = mockUser(5L, User.Role.CLIENT);
        setAuth(creator);
        Order order = buildOrder(10L, 5L);

        assertThatCode(() -> orderService.assertCanReadOrder(order)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrder: assigned driver can read the order")
    void readOrder_assignedDriver_allowed() {
        User driver = mockUser(3L, User.Role.DRIVER);
        setAuth(driver);
        DriverPerson dp = mock(DriverPerson.class);
        when(dp.getId()).thenReturn(3L);
        Order order = buildOrder(10L, 99L);
        order.setDriverPerson(dp);

        assertThatCode(() -> orderService.assertCanReadOrder(order)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrder: customer user can read their own order")
    void readOrder_customer_allowed() {
        User client = mockUser(2L, User.Role.CLIENT);
        setAuth(client);
        CustomerUser cu = mock(CustomerUser.class);
        when(cu.getId()).thenReturn(2L);
        Order order = buildOrder(10L, 99L);
        order.setCustomerUser(cu);

        assertThatCode(() -> orderService.assertCanReadOrder(order)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrder: vendor company owner can read their order")
    void readOrder_vendorOwner_allowed() {
        VendorOwner vo = mock(VendorOwner.class);
        when(vo.getId()).thenReturn(7L);
        VendorCompany vc = new VendorCompany();
        vc.setOwner(vo);

        User vendorUser = mockUser(7L, User.Role.VENDOR_OWNER);
        setAuth(vendorUser);
        Order order = buildOrder(10L, 99L);
        order.setVendorCompany(vc);

        assertThatCode(() -> orderService.assertCanReadOrder(order)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrder: delivery company owner can read their order")
    void readOrder_deliveryOwner_allowed() {
        DeliveryOwner do_ = mock(DeliveryOwner.class);
        when(do_.getId()).thenReturn(8L);
        DeliveryCompany dc = new DeliveryCompany();
        dc.setOwner(do_);

        User deliveryUser = mockUser(8L, User.Role.DELIVERY_OWNER);
        setAuth(deliveryUser);
        Order order = buildOrder(10L, 99L);
        order.setDeliveryCompany(dc);

        assertThatCode(() -> orderService.assertCanReadOrder(order)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrder: unrelated user is denied")
    void readOrder_unrelated_denied() {
        User stranger = mockUser(100L, User.Role.CLIENT);
        setAuth(stranger);
        Order order = buildOrder(10L, 99L);

        assertThatThrownBy(() -> orderService.assertCanReadOrder(order))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertCanReadOrder: unauthenticated (null auth) throws")
    void readOrder_nullAuth_throws() {
        lenient().when(securityService.getCurrentUser())
                .thenThrow(new AccessDeniedException("No authenticated user in context"));

        Order order = buildOrder(10L, 99L);
        assertThatThrownBy(() -> orderService.assertCanReadOrder(order))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── assertCanReadOrderLocation (no OPEN_FOR_BID exception) ───────────

    @Test
    @DisplayName("assertCanReadOrderLocation: SUPER_ADMIN allowed")
    void location_superAdmin_allowed() {
        setAuth(mockUser(1L, User.Role.SUPER_ADMIN));
        assertThatCode(() -> orderService.assertCanReadOrderLocation(buildOrder(10L, 99L)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: creator allowed")
    void location_creator_allowed() {
        setAuth(mockUser(5L, User.Role.CLIENT));
        assertThatCode(() -> orderService.assertCanReadOrderLocation(buildOrder(10L, 5L)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: assigned driver allowed")
    void location_driver_allowed() {
        User driver = mockUser(3L, User.Role.DRIVER);
        setAuth(driver);
        DriverPerson dp = mock(DriverPerson.class);
        when(dp.getId()).thenReturn(3L);
        Order order = buildOrder(10L, 99L);
        order.setDriverPerson(dp);

        assertThatCode(() -> orderService.assertCanReadOrderLocation(order))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: customer user allowed")
    void location_customer_allowed() {
        User client = mockUser(2L, User.Role.CLIENT);
        setAuth(client);
        CustomerUser cu = mock(CustomerUser.class);
        when(cu.getId()).thenReturn(2L);
        Order order = buildOrder(10L, 99L);
        order.setCustomerUser(cu);

        assertThatCode(() -> orderService.assertCanReadOrderLocation(order))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: vendor owner of the order's vendor allowed")
    void location_vendorOwner_allowed() {
        VendorOwner vo = mock(VendorOwner.class);
        when(vo.getId()).thenReturn(7L);
        VendorCompany vc = new VendorCompany();
        vc.setOwner(vo);
        setAuth(mockUser(7L, User.Role.VENDOR_OWNER));
        Order order = buildOrder(10L, 99L);
        order.setVendorCompany(vc);

        assertThatCode(() -> orderService.assertCanReadOrderLocation(order))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: delivery owner of the order's delivery allowed")
    void location_deliveryOwner_allowed() {
        DeliveryOwner do_ = mock(DeliveryOwner.class);
        when(do_.getId()).thenReturn(8L);
        DeliveryCompany dc = new DeliveryCompany();
        dc.setOwner(do_);
        setAuth(mockUser(8L, User.Role.DELIVERY_OWNER));
        Order order = buildOrder(10L, 99L);
        order.setDeliveryCompany(dc);

        assertThatCode(() -> orderService.assertCanReadOrderLocation(order))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: unrelated user is denied")
    void location_unrelated_denied() {
        setAuth(mockUser(100L, User.Role.CLIENT));
        assertThatThrownBy(() -> orderService.assertCanReadOrderLocation(buildOrder(10L, 99L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: driver NOT assigned to this order is denied")
    void location_unassignedDriver_denied() {
        User driver = mockUser(99L, User.Role.DRIVER);
        setAuth(driver);
        DriverPerson dp = mock(DriverPerson.class);
        when(dp.getId()).thenReturn(3L);  // assigned to someone else
        Order order = buildOrder(10L, 5L);
        order.setDriverPerson(dp);

        assertThatThrownBy(() -> orderService.assertCanReadOrderLocation(order))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: OPEN_FOR_BID does NOT grant access to drivers")
    void location_openForBid_driverDenied() {
        User driver = mockUser(99L, User.Role.DRIVER);
        setAuth(driver);
        Order order = buildOrder(10L, 5L);
        order.setStatus(Order.OrderStatus.OPEN_FOR_BID);

        assertThatThrownBy(() -> orderService.assertCanReadOrderLocation(order))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertCanReadOrderLocation: OPEN_FOR_BID does NOT grant access to delivery owners")
    void location_openForBid_deliveryOwnerDenied() {
        setAuth(mockUser(88L, User.Role.DELIVERY_OWNER));
        Order order = buildOrder(10L, 5L);
        order.setStatus(Order.OrderStatus.OPEN_FOR_BID);

        assertThatThrownBy(() -> orderService.assertCanReadOrderLocation(order))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── assertCanReadOrder: OPEN_FOR_BID grants access to drivers/owners ─

    @Test
    @DisplayName("assertCanReadOrder: OPEN_FOR_BID allows any driver to view the order")
    void readOrder_openForBid_driverAllowed() {
        User driver = mockUser(99L, User.Role.DRIVER);
        setAuth(driver);
        Order order = buildOrder(10L, 5L);
        order.setStatus(Order.OrderStatus.OPEN_FOR_BID);

        assertThatCode(() -> orderService.assertCanReadOrder(order)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertCanReadOrder: OPEN_FOR_BID allows any delivery owner to view the order")
    void readOrder_openForBid_deliveryOwnerAllowed() {
        setAuth(mockUser(88L, User.Role.DELIVERY_OWNER));
        Order order = buildOrder(10L, 5L);
        order.setStatus(Order.OrderStatus.OPEN_FOR_BID);

        assertThatCode(() -> orderService.assertCanReadOrder(order)).doesNotThrowAnyException();
    }

    // ─── assertCanUpdateStatus (via updateOrderStatus) ────────────────────

    @Test
    @DisplayName("updateOrderStatus: SUPER_ADMIN can transition status")
    void updateStatus_superAdmin_allowed() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.ASSIGNED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.CONFIRMED);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        verify(realtimeTrackingService).broadcastOrderStatusChanged(updated);
    }

    @Test
    @DisplayName("updateOrderStatus: assigned driver can transition status")
    void updateStatus_assignedDriver_allowed() {
        User driver = mockUser(3L, User.Role.DRIVER);
        setAuth(driver);
        DriverPerson dp = mock(DriverPerson.class);
        when(dp.getId()).thenReturn(3L);
        Order order = buildOrder(10L, 99L);
        order.setDriverPerson(dp);
        order.setStatus(Order.OrderStatus.ASSIGNED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.IN_PROGRESS);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("updateOrderStatus: delivery owner of the order's company can transition")
    void updateStatus_deliveryOwner_allowed() {
        DeliveryOwner do_ = mock(DeliveryOwner.class);
        when(do_.getId()).thenReturn(8L);
        DeliveryCompany dc = new DeliveryCompany();
        dc.setOwner(do_);
        setAuth(mockUser(8L, User.Role.DELIVERY_OWNER));
        Order order = buildOrder(10L, 99L);
        order.setDeliveryCompany(dc);
        order.setStatus(Order.OrderStatus.ASSIGNED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.CONFIRMED);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("updateOrderStatus: unrelated user is denied")
    void updateStatus_unrelated_denied() {
        setAuth(mockUser(100L, User.Role.CLIENT));
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.ASSIGNED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(10L, Order.OrderStatus.CONFIRMED))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateOrderStatus: driver not assigned to this order is denied")
    void updateStatus_unassignedDriver_denied() {
        User driver = mockUser(99L, User.Role.DRIVER);
        setAuth(driver);
        DriverPerson dp = mock(DriverPerson.class);
        when(dp.getId()).thenReturn(3L);  // assigned to someone else
        Order order = buildOrder(10L, 99L);
        order.setDriverPerson(dp);
        order.setStatus(Order.OrderStatus.ASSIGNED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(10L, Order.OrderStatus.IN_PROGRESS))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── invalid status transitions ───────────────────────────────────────

    @Test
    @DisplayName("updateOrderStatus: DELIVERED -> IN_PROGRESS is invalid")
    void updateStatus_terminalToNonTerminal_invalid() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.DELIVERED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(10L, Order.OrderStatus.IN_PROGRESS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("updateOrderStatus: PENDING -> DELIVERED is invalid")
    void updateStatus_pendingToDelivered_invalid() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(10L, Order.OrderStatus.DELIVERED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("updateOrderStatus: CANCELLED -> anything is invalid")
    void updateStatus_cancelledTerminal_invalid() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(10L, Order.OrderStatus.PENDING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("updateOrderStatus: PENDING -> ASSIGNED is valid")
    void updateStatus_pendingToAssigned_valid() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.ASSIGNED);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.ASSIGNED);
    }

    @Test
    @DisplayName("updateOrderStatus: IN_PROGRESS -> PICKED_UP is valid")
    void updateStatus_inProgressToPickedUp_valid() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.IN_PROGRESS);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.PICKED_UP);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.PICKED_UP);
    }

    @Test
    @DisplayName("updateOrderStatus: CANCELLED can be reached from any non-terminal status")
    void updateStatus_anyToCancelled_valid() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.IN_PROGRESS);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.CANCELLED);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("updateOrderStatus: FAILED can be reached from any non-terminal status")
    void updateStatus_anyToFailed_valid() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.PICKED_UP);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.FAILED);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.FAILED);
    }

    @Test
    @DisplayName("updateOrderStatus: IN_TRANSIT -> DELIVERED is valid")
    void updateStatus_inTransitToDelivered_valid() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.IN_TRANSIT);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.DELIVERED);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("updateOrderStatus: same status is a no-op (no transition validation)")
    void updateStatus_sameStatus_noOp() {
        User admin = mockUser(1L, User.Role.SUPER_ADMIN);
        setAuth(admin);
        Order order = buildOrder(10L, 99L);
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(10L, Order.OrderStatus.PENDING);
        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }
}