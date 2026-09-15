package com.swiftdeliver.backend.controller;

import com.swiftdeliver.backend.dto.JwtResponseDto;
import com.swiftdeliver.backend.dto.LoginDto;
import com.swiftdeliver.backend.dto.TokenRefreshDto;
import com.swiftdeliver.backend.dto.UserResponseDto;
import com.swiftdeliver.backend.service.AuditLogService;
import com.swiftdeliver.backend.service.RateLimitingService;
import com.swiftdeliver.backend.service.TokenBlacklistService;
import com.swiftdeliver.backend.service.UserService;
import com.swiftdeliver.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RateLimitingService rateLimitingService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditLogService auditLogService;

    /**
     * Comma-separated allowlist of reverse-proxy IPs that MAY set
     * X-Forwarded-For / X-Real-IP. When the direct peer is not on this list,
     * proxy headers are ignored to prevent clients from spoofing their IP and
     * bypassing the login rate limiter.
     */
    @Value("${security.rate-limiting.trusted-proxies:}")
    private String trustedProxies;
    
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDto> authenticateUser(@Valid @RequestBody LoginDto loginDto, HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        
        // Check rate limiting
        if (rateLimitingService.isRateLimited(clientIp)) {
            long remainingMinutes = rateLimitingService.getRemainingLockoutMinutes(clientIp);
            log.warn("Rate limited login attempt from IP: {} for user: {}", clientIp, loginDto.getUsernameOrEmail());
            
            JwtResponseDto errorResponse = new JwtResponseDto();
            errorResponse.setMessage(String.format("Too many failed login attempts. Please try again in %d minutes.", remainingMinutes));
            errorResponse.setTimestamp(LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginDto.getUsernameOrEmail(),
                    loginDto.getPassword()
                )
            );
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Record successful login (clears failed attempts)
            rateLimitingService.recordSuccessfulAttempt(clientIp);
            
            // Log successful login for audit
            auditLogService.logLoginAttempt(userDetails.getUsername(), clientIp, true);
            
            // Generate JWT tokens
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);
            
            // Get user information
            UserResponseDto userResponse = userService.findByUsernameDto(userDetails.getUsername())
                .or(() -> userService.findByEmail(userDetails.getUsername()))
                .orElse(null);
            
            // Create JWT response
            JwtResponseDto jwtResponse = new JwtResponseDto(
                accessToken,
                refreshToken,
                jwtUtil.getExpirationTime(),
                userResponse
            );
            
            log.info("Successful login for user: {} from IP: {}", userDetails.getUsername(), clientIp);
            return ResponseEntity.ok(jwtResponse);
        } catch (AuthenticationException e) {
            // Record failed login attempt
            rateLimitingService.recordFailedAttempt(clientIp);
            
            // Log failed login for audit
            auditLogService.logLoginAttempt(loginDto.getUsernameOrEmail(), clientIp, false);
            
            log.warn("Failed login attempt for user: {} from IP: {} - {}", 
                    loginDto.getUsernameOrEmail(), clientIp, e.getMessage());
            
            JwtResponseDto errorResponse = new JwtResponseDto();
            errorResponse.setMessage("Invalid username/email or password!");
            errorResponse.setTimestamp(LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponseDto> refreshToken(@Valid @RequestBody TokenRefreshDto tokenRefreshDto, HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        
        try {
            String refreshToken = tokenRefreshDto.getRefreshToken();
            
            // Validate refresh token
            if (!jwtUtil.validateToken(refreshToken)) {
                log.warn("Invalid refresh token attempt from IP: {}", clientIp);
                JwtResponseDto errorResponse = new JwtResponseDto();
                errorResponse.setMessage("Invalid refresh token!");
                errorResponse.setTimestamp(LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            // Check if token is blacklisted
            String tokenId = jwtUtil.extractTokenId(refreshToken);
            if (tokenBlacklistService.isTokenBlacklisted(tokenId)) {
                log.warn("Blacklisted refresh token attempted from IP: {}", clientIp);
                JwtResponseDto errorResponse = new JwtResponseDto();
                errorResponse.setMessage("Token has been revoked!");
                errorResponse.setTimestamp(LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            // Check if it's actually a refresh token
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                log.warn("Non-refresh token used for refresh from IP: {}", clientIp);
                JwtResponseDto errorResponse = new JwtResponseDto();
                errorResponse.setMessage("Invalid token type!");
                errorResponse.setTimestamp(LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            // Extract username and generate new tokens
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userService.loadUserByUsername(username);
            
            // Blacklist the old refresh token
            tokenBlacklistService.blacklistToken(tokenId);
            
            String newAccessToken = jwtUtil.generateToken(userDetails);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);
            
            // Get user information
            UserResponseDto userResponse = userService.findByUsernameDto(username)
                .or(() -> userService.findByEmail(username))
                .orElse(null);
            
            JwtResponseDto jwtResponse = new JwtResponseDto(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getExpirationTime(),
                userResponse
            );
            jwtResponse.setMessage("Token refreshed successfully!");
            
            // Log token refresh for audit
            auditLogService.logTokenRefresh(username, clientIp);
            
            log.info("Token refreshed successfully for user: {} from IP: {}", username, clientIp);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            log.error("Token refresh failed from IP: {} - {}", clientIp, e.getMessage());
            JwtResponseDto errorResponse = new JwtResponseDto();
            errorResponse.setMessage("Token refresh failed: " + e.getMessage());
            errorResponse.setTimestamp(LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutUser(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Extract and blacklist the current access token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String tokenId = jwtUtil.extractTokenId(token);
                tokenBlacklistService.blacklistToken(tokenId);
                
                if (authentication != null && authentication.isAuthenticated()) {
                    // Log logout for audit
                    auditLogService.logLogout(authentication.getName(), clientIp);
                    log.info("User {} logged out from IP: {}", authentication.getName(), clientIp);
                }
            } catch (Exception e) {
                log.warn("Failed to blacklist token during logout from IP: {} - {}", clientIp, e.getMessage());
            }
        }
        
        SecurityContextHolder.clearContext();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User logged out successfully!");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        return userService.findByUsernameDto(authentication.getName())
            .or(() -> userService.findByEmail(authentication.getName()))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Extract client IP address from request. Proxy headers are only trusted
     * when the request came directly from a configured trusted proxy; otherwise
     * the socket peer address is used so clients cannot spoof their identity.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (trustedProxies == null || trustedProxies.trim().isEmpty()) {
            return false;
        }
        Set<String> allowed = new HashSet<>();
        Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .forEach(allowed::add);
        return allowed.contains(remoteAddr);
    }

}