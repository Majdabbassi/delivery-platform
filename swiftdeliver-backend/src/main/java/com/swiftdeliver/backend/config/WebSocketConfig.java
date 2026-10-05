package com.swiftdeliver.backend.config;

import com.swiftdeliver.backend.repository.UserRepository;
import com.swiftdeliver.backend.service.OrderService;
import com.swiftdeliver.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final String[] allowedOrigins;

    // @Lazy: OrderService publishes through the messaging template, which needs this configuration.
    public WebSocketConfig(JwtUtil jwtUtil, UserRepository userRepository, @Lazy OrderService orderService,
                           @Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .addInterceptors(new StompHandshakeInterceptor(jwtUtil, userRepository))
                // Spring takes the session's user from the handshake handler, not from the attribute the
                // interceptor stores, so without this every socket had no user and every subscription failed.
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                                      org.springframework.web.socket.WebSocketHandler wsHandler,
                                                      java.util.Map<String, Object> attributes) {
                        return (Principal) attributes.get("principal");
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompChannelInterceptor(orderService::canUserReadOrder));
    }
}
