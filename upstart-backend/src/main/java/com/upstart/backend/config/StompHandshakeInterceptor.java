package com.upstart.backend.config;

import com.upstart.backend.repository.UserRepository;
import com.upstart.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

/**
 * Authenticates STOMP WebSocket handshakes using an optional ?token= JWT query param.
 * Existing anonymous (no token) connections are still allowed so current clients keep working;
 * they simply have no Principal and therefore cannot access the private per-user topics.
 */
@RequiredArgsConstructor
public class StompHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = tokenFrom(request);
        if (token == null || !jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token)) {
            return true; // allow anonymous; no Principal set
        }
        String username = jwtUtil.extractUsername(token);
        if (username == null) {
            return true;
        }
        Long userId = userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElse(null);
        if (userId == null) {
            return true;
        }
        Principal principal = new UsernamePasswordAuthenticationToken(
                username, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_WEBSOCKET:" + userId)));
        attributes.put("principal", principal);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String tokenFrom(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getParameter("token");
        }
        return null;
    }
}
