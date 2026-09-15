package com.swiftdeliver.backend.config;

import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.entity.User;
import com.swiftdeliver.backend.repository.OrderRepository;
import com.swiftdeliver.backend.repository.UserRepository;
import com.swiftdeliver.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

/**
 * Authenticates STOMP sessions at the CONNECT frame and enforces per-topic
 * authorization on SUBSCRIBE frames.
 *
 * <p>Handshake-level (?token=) authentication cannot work reliably with SockJS:
 * the query parameter is dropped on XHR/iframe transports. Authenticating the
 * STOMP CONNECT frame works over every transport and is the only place the
 * token can be reliably read.</p>
 *
 * <p>Anonymous connections are refused: a valid access token in the CONNECT
 * frame is mandatory.</p>
 */
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String USER_TOPIC_PREFIX = "/topic/users/";
    private static final String ORDER_TOPIC_PREFIX = "/topic/orders/";
    private static final String GLOBAL_LOCATION_TOPIC = "/topic/orders/location";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT == accessor.getCommand()) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
            authorizeSubscribe(accessor);
        }
        return message;
    }

    /**
     * Authenticates the CONNECT frame using a Bearer JWT (Authorization header
     * or a plain {@code token} STOMP header) and ties the principal to the
     * session. Rejects the connection when no valid access token is provided.
     */
    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = tokenFrom(accessor);
        if (token == null || !jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token)) {
            throw new AccessDeniedException("Authentication required to open a WebSocket connection");
        }
        String username = jwtUtil.extractUsername(token);
        User user = username == null ? null : userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new AccessDeniedException("Authentication required to open a WebSocket connection");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_WEBSOCKET:" + user.getId())));
        accessor.setUser(authentication);
        accessor.setLeaveMutable(true);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("Subscription destination is required");
        }
        if (GLOBAL_LOCATION_TOPIC.equals(destination)) {
            // Live GPS is only delivered per-order / per-user, never globally.
            throw new AccessDeniedException("The global location topic is not available");
        }

        Authentication auth = accessor.getUser() instanceof Authentication a ? a : null;
        if (auth == null) {
            throw new AccessDeniedException("Authentication required to subscribe");
        }
        Long currentUserId = userIdFrom(auth);
        if (currentUserId == null) {
            throw new AccessDeniedException("Malformed WebSocket session credentials");
        }

        if (destination.startsWith(USER_TOPIC_PREFIX)) {
            Long topicUserId = parseId(destination.substring(USER_TOPIC_PREFIX.length()));
            if (topicUserId == null || !currentUserId.equals(topicUserId)) {
                throw new AccessDeniedException("Cannot subscribe to another user's topic");
            }
            return;
        }
        if (destination.startsWith(ORDER_TOPIC_PREFIX)) {
            authorizeOrderTopic(destination, currentUserId);
        }
        // The generic /topic/orders status feed stays open to any authenticated
        // user; per-order and location topics are strictly scoped above.
    }

    /**
     * A user may subscribe to an order topic only when they are involved in the
     * order: the SUPPLIER (createdByUserId), customer, assigned driver, vendor
     * company owner, delivery company owner, or a SUPER_ADMIN.
     */
    private void authorizeOrderTopic(String destination, Long currentUserId) {
        Long orderId = parseId(destination.substring(ORDER_TOPIC_PREFIX.length()));
        if (orderId == null) {
            throw new AccessDeniedException("Invalid order topic destination");
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new AccessDeniedException("Order not found");
        }

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null) {
            throw new AccessDeniedException("Unknown user");
        }
        if (currentUser.getRole() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (order.getCreatedByUserId() != null && currentUserId.equals(order.getCreatedByUserId())) {
            return;
        }
        if (order.getCustomerUser() != null && currentUserId.equals(order.getCustomerUser().getId())) {
            return;
        }
        if (order.getDriverPerson() != null && currentUserId.equals(order.getDriverPerson().getId())) {
            return;
        }
        if (order.getVendorCompany() != null && order.getVendorCompany().getOwner() != null
                && currentUserId.equals(order.getVendorCompany().getOwner().getId())) {
            return;
        }
        if (order.getDeliveryCompany() != null && order.getDeliveryCompany().getOwner() != null
                && currentUserId.equals(order.getDeliveryCompany().getOwner().getId())) {
            return;
        }
        throw new AccessDeniedException("You are not involved in this order");
    }

    private String tokenFrom(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        String token = accessor.getFirstNativeHeader("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        return null;
    }

    private Long userIdFrom(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_WEBSOCKET:")) {
                return parseId(value.substring("ROLE_WEBSOCKET:".length()));
            }
        }
        return null;
    }

    private Long parseId(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}