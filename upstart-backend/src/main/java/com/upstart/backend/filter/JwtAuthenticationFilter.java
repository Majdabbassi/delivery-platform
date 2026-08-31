package com.upstart.backend.filter;

import com.upstart.backend.service.TokenBlacklistService;
import com.upstart.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        
        // Check if Authorization header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract JWT token from Authorization header
        jwt = authHeader.substring(7);
        
        try {
            // First validate the token format and extract basic claims
            if (!jwtUtil.validateToken(jwt)) {
                log.warn("Invalid JWT token received from IP: {}", getClientIpAddress(request));
                filterChain.doFilter(request, response);
                return;
            }
            
            // Check if token is blacklisted
            String tokenId = jwtUtil.extractTokenId(jwt);
            if (tokenBlacklistService.isTokenBlacklisted(tokenId)) {
                log.warn("Blacklisted token attempted access from IP: {}", getClientIpAddress(request));
                filterChain.doFilter(request, response);
                return;
            }
            
            // Ensure this is an access token, not a refresh token
            if (!jwtUtil.isAccessToken(jwt)) {
                log.warn("Non-access token used for authentication from IP: {}", getClientIpAddress(request));
                filterChain.doFilter(request, response);
                return;
            }
            
            username = jwtUtil.extractUsername(jwt);
            
            // If username is extracted and no authentication is set in SecurityContext
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                // Final validation with user details
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Successfully authenticated user: {} from IP: {}", username, getClientIpAddress(request));
                } else {
                    log.warn("Token validation failed for user: {} from IP: {}", username, getClientIpAddress(request));
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication from IP: {}, error: {}", getClientIpAddress(request), e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract client IP address from request, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}