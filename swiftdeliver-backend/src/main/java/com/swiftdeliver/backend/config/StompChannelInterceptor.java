package com.swiftdeliver.backend.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides who may subscribe to what. Everything is denied unless a rule allows it:
 * <ul>
 *   <li>{@code /topic/users/{id}}: only that user;</li>
 *   <li>{@code /topic/orders} and {@code /topic/orders/location} (every order, every driver):
 *       SUPER_ADMIN only;</li>
 *   <li>{@code /topic/orders/{id}}: only people who may read that order, by the same rule as
 *       {@code GET /api/orders/{id}}.</li>
 * </ul>
 */
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String USER_TOPIC_PREFIX = "/topic/users/";
    private static final Pattern ORDER_TOPIC = Pattern.compile("^/topic/orders/(\\d+)$");

    /** (userId, orderId) -> may this user read the order? */
    private final BiPredicate<Long, Long> orderAccess;

    public StompChannelInterceptor(BiPredicate<Long, Long> orderAccess) {
        this.orderAccess = orderAccess;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || StompCommand.SUBSCRIBE != accessor.getCommand()) {
            return message;
        }
        String destination = accessor.getDestination();
        Authentication auth = accessor.getUser() instanceof Authentication a ? a : null;
        if (destination == null || auth == null) {
            throw new AccessDeniedException("Authentication required to subscribe.");
        }
        Long userId = userIdOf(auth);
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch((StompHandshakeInterceptor.ROLE_AUTHORITY_PREFIX + "SUPER_ADMIN")::equals);

        if (destination.startsWith(USER_TOPIC_PREFIX)) {
            if (userId == null || !destination.equals(USER_TOPIC_PREFIX + userId)) {
                throw new AccessDeniedException("Cannot subscribe to another user's topic.");
            }
            return message;
        }
        if (destination.equals("/topic/orders") || destination.equals("/topic/orders/location")) {
            if (!isAdmin) {
                throw new AccessDeniedException("The global order feeds are for administrators.");
            }
            return message;
        }
        Matcher order = ORDER_TOPIC.matcher(destination);
        if (order.matches()) {
            if (isAdmin || (userId != null && orderAccess.test(userId, Long.valueOf(order.group(1))))) {
                return message;
            }
            throw new AccessDeniedException("You may not follow this order.");
        }
        throw new AccessDeniedException("Unknown destination.");
    }

    private static Long userIdOf(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String value = authority.getAuthority();
            if (value.startsWith(StompHandshakeInterceptor.USER_AUTHORITY_PREFIX)) {
                try {
                    return Long.valueOf(value.substring(StompHandshakeInterceptor.USER_AUTHORITY_PREFIX.length()));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
