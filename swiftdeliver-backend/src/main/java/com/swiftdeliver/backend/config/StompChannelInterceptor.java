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

/**
 * Guards SUBSCRIBE frames targeting the private per-user topic (/topic/users/{id}).
 * A session may only subscribe to its own user topic. Global topics (/topic/orders*) stay open.
 */
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String USER_TOPIC_PREFIX = "/topic/users/";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || StompCommand.SUBSCRIBE != accessor.getCommand()) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(USER_TOPIC_PREFIX)) {
            return message; // global topics unaffected
        }

        String topicUserIdStr = destination.substring(USER_TOPIC_PREFIX.length());
        long topicUserId;
        try {
            topicUserId = Long.parseLong(topicUserIdStr);
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Invalid user topic destination.");
        }

        Authentication auth = accessor.getUser() instanceof Authentication a ? a : null;
        if (auth == null) {
            throw new AccessDeniedException("Authentication required to subscribe to private user topic.");
        }

        String expected = "ROLE_WEBSOCKET:" + topicUserId;
        boolean allowed = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
        if (!allowed) {
            throw new AccessDeniedException("Cannot subscribe to another user's topic.");
        }
        return message;
    }
}
