package com.swiftdeliver.backend.config;

import com.swiftdeliver.backend.repository.UserRepository;
import com.swiftdeliver.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Authenticates the STOMP WebSocket handshake with the access JWT passed as {@code ?token=}
 * (browsers cannot set headers on a WebSocket). A handshake without a valid token is refused:
 * anonymous sockets used to be allowed and could subscribe to the global order feeds.
 *
 * <p>The principal carries two authorities that {@link StompChannelInterceptor} uses to decide
 * which topics the session may subscribe to: {@code ROLE_WEBSOCKET:<userId>} and
 * {@code WS_ROLE:<ROLE>}.
 */
@RequiredArgsConstructor
public class StompHandshakeInterceptor implements HandshakeInterceptor {

    static final String USER_AUTHORITY_PREFIX = "ROLE_WEBSOCKET:";
    static final String ROLE_AUTHORITY_PREFIX = "WS_ROLE:";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = tokenFrom(request);
        if (token == null || !jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token)) {
            return false;
        }
        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            return false;
        }
        return userRepository.findByUsername(username).map(user -> {
            Principal principal = new UsernamePasswordAuthenticationToken(
                    username, null,
                    List.of(new SimpleGrantedAuthority(USER_AUTHORITY_PREFIX + user.getId()),
                            new SimpleGrantedAuthority(ROLE_AUTHORITY_PREFIX + user.getRole())));
            attributes.put("principal", principal);
            return true;
        }).orElse(false);
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // nothing to do
    }

    private String tokenFrom(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getParameter("token");
        }
        return null;
    }
}
