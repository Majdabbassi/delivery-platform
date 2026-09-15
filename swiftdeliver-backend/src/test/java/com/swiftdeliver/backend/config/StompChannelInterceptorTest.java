package com.swiftdeliver.backend.config;

import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.DeliveryOwner;
import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.entity.User;
import com.swiftdeliver.backend.entity.VendorCompany;
import com.swiftdeliver.backend.entity.VendorOwner;
import com.swiftdeliver.backend.repository.OrderRepository;
import com.swiftdeliver.backend.repository.UserRepository;
import com.swiftdeliver.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompChannelInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MessageChannel channel;

    private StompChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompChannelInterceptor(jwtUtil, userRepository, orderRepository);
    }

    /**
     * Builds a STOMP message the same way Spring's own broker tests do, so the
     * StompHeaderAccessor (including the CONNECT/SUBSCRIBE command and native
     * headers) is retrievable from the message afterwards.
     */
    private Message<byte[]> connectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", authorizationHeader);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> connectMessagePlainToken(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("token", token);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> connectMessageNoToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination, Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destination);
        if (userId != null) {
            accessor.setUser(authenticated(userId));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private User user(Long id, User.Role role) {
        User user = org.mockito.Mockito.mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getRole()).thenReturn(role);
        return user;
    }

    private org.springframework.security.core.Authentication authenticated(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                "user" + userId, null,
                java.util.Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_WEBSOCKET:" + userId)));
    }

    private Order order(Long id, Long createdByUserId) {
        Order order = new Order();
        order.setId(id);
        order.setCreatedByUserId(createdByUserId);
        return order;
    }

    @Test
    @DisplayName("CONNECT without a token is rejected")
    void connectWithoutTokenRejected() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessageNoToken(), channel))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("CONNECT with an invalid token is rejected")
    void connectWithInvalidTokenRejected() {
        when(jwtUtil.validateToken("bad.token")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer bad.token"), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("CONNECT with a refresh token (not access) is rejected")
    void connectWithRefreshTokenRejected() {
        when(jwtUtil.validateToken("refresh.token")).thenReturn(true);
        when(jwtUtil.isAccessToken("refresh.token")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer refresh.token"), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("CONNECT with a valid access token and existing user is accepted and sets principal")
    void connectValidTokenAccepted() {
        String token = "ok.access.token";
        User driver = user(42L, User.Role.DRIVER);
        when(driver.getUsername()).thenReturn("driver.alice");
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.isAccessToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("driver.alice");
        when(userRepository.findByUsername("driver.alice")).thenReturn(Optional.of(driver));

        Message<?> out = interceptor.preSend(connectMessage("Bearer " + token), channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(out, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("driver.alice");
    }

    @Test
    @DisplayName("CONNECT with a plain 'token' native header is accepted")
    void connectPlainTokenAccepted() {
        String token = "plain.token";
        User driver = user(42L, User.Role.DRIVER);
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.isAccessToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("driver.alice");
        when(userRepository.findByUsername("driver.alice")).thenReturn(Optional.of(driver));

        Message<?> out = interceptor.preSend(connectMessagePlainToken(token), channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(out, StompHeaderAccessor.class);
        assertThat(accessor.getUser()).isNotNull();
    }

    @Test
    @DisplayName("CONNECT with a valid token but unknown user is rejected")
    void connectUnknownUserRejected() {
        String token = "valid.but.unregistered";
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.isAccessToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("ghost.user");
        when(userRepository.findByUsername("ghost.user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer " + token), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SUBSCRIBE without an authenticated user is rejected")
    void subscribeUnauthenticatedRejected() {
        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/users/42", null), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SUBSCRIBE to the global live-location topic is always rejected")
    void globalLocationTopicRejected() {
        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/orders/location", 42L), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("SUBSCRIBE to another user's topic is rejected")
    void otherUserTopicRejected() {
        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/users/99", 42L), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SUBSCRIBE to your own user topic is allowed")
    void ownUserTopicAllowed() {
        interceptor.preSend(subscribeMessage("/topic/users/42", 42L), channel);
    }

    @Test
    @DisplayName("SUBSCRIBE to an order topic you are not involved in is rejected")
    void orderTopicNotInvolvedRejected() {
        User viewer = user(42L, User.Role.CLIENT);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order(7L, null)));
        when(userRepository.findById(42L)).thenReturn(Optional.of(viewer));

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/orders/7", 42L), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not involved");
    }

    @Test
    @DisplayName("SUBSCRIBE to an order's topic as the creating user is allowed")
    void orderTopicCreatorAllowed() {
        User creator = user(42L, User.Role.CLIENT);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order(7L, 42L)));
        when(userRepository.findById(42L)).thenReturn(Optional.of(creator));

        interceptor.preSend(subscribeMessage("/topic/orders/7", 42L), channel);
    }

    @Test
    @DisplayName("SUBSCRIBE to an order's topic as its vendor owner is allowed")
    void orderTopicVendorOwnerAllowed() {
        VendorOwner owner = org.mockito.Mockito.mock(VendorOwner.class);
        when(owner.getId()).thenReturn(42L);
        VendorCompany vendor = new VendorCompany();
        vendor.setOwner(owner);
        Order order = order(7L, null);
        order.setVendorCompany(vendor);

        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        User vendorOwnerUser = user(42L, User.Role.VENDOR_OWNER);
        when(userRepository.findById(42L)).thenReturn(Optional.of(vendorOwnerUser));

        interceptor.preSend(subscribeMessage("/topic/orders/7", 42L), channel);
    }

    @Test
    @DisplayName("SUBSCRIBE to an order's topic as its delivery owner is allowed")
    void orderTopicDeliveryOwnerAllowed() {
        DeliveryOwner owner = org.mockito.Mockito.mock(DeliveryOwner.class);
        when(owner.getId()).thenReturn(42L);
        DeliveryCompany delivery = new DeliveryCompany();
        delivery.setOwner(owner);
        Order order = order(7L, null);
        order.setDeliveryCompany(delivery);

        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        User deliveryOwner = user(42L, User.Role.DELIVERY_OWNER);
        when(userRepository.findById(42L)).thenReturn(Optional.of(deliveryOwner));

        interceptor.preSend(subscribeMessage("/topic/orders/7", 42L), channel);
    }

    @Test
    @DisplayName("SUBSCRIBE to an order's topic as SUPER_ADMIN is allowed")
    void orderTopicSuperAdminAllowed() {
        User admin = user(1L, User.Role.SUPER_ADMIN);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order(7L, null)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        interceptor.preSend(subscribeMessage("/topic/orders/7", 1L), channel);
    }

    @Test
    @DisplayName("SUBSCRIBE to an order topic for a missing order is rejected")
    void orderTopicMissingOrderRejected() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(subscribeMessage("/topic/orders/999", 42L), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("generic /topic/orders feed stays open to authenticated users")
    void genericOrdersFeedAllowed() {
        interceptor.preSend(subscribeMessage("/topic/orders", 42L), channel);
    }
}