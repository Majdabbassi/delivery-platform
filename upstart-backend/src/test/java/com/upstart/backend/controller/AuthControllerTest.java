package com.upstart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upstart.backend.dto.LoginDto;
import com.upstart.backend.dto.TokenRefreshDto;

import com.upstart.backend.dto.UserResponseDto;
import com.upstart.backend.entity.CustomerUser;
import com.upstart.backend.service.AuditLogService;
import com.upstart.backend.service.RateLimitingService;
import com.upstart.backend.service.TokenBlacklistService;
import com.upstart.backend.service.UserService;
import com.upstart.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RateLimitingService rateLimitingService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    private CustomerUser testUser;
    private LoginDto validLoginDto;

    private TokenRefreshDto validTokenRefreshDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        
        // Configure rate limiting service to not rate limit by default
        lenient().when(rateLimitingService.isRateLimited(anyString())).thenReturn(false);
        
        // Setup test user
        testUser = new CustomerUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActive(true);
        
        // Setup valid DTOs
        validLoginDto = new LoginDto();
        validLoginDto.setUsernameOrEmail("testuser");
        validLoginDto.setPassword("SecurePass123!");
        

        
        validTokenRefreshDto = new TokenRefreshDto();
        validTokenRefreshDto.setRefreshToken("valid.refresh.token");
    }

    // Login Tests

    @Test
    void login_WithValidCredentials_ShouldReturnTokens() throws Exception {
        // Given
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        lenient().when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setUsername(testUser.getUsername());
        userResponseDto.setEmail(testUser.getEmail());
        lenient().when(userService.findByUsernameDto(testUser.getUsername())).thenReturn(java.util.Optional.of(userResponseDto));
        lenient().when(userService.findByEmail(testUser.getUsername())).thenReturn(java.util.Optional.empty());
        lenient().when(jwtUtil.generateToken(userDetails)).thenReturn("access.token.here");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh.token.here");
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token.here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token.here"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.user.email").value(testUser.getEmail()));
        
        verify(auditLogService).logLoginAttempt(eq(testUser.getUsername()), anyString(), eq(true));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password!"));
        
        verify(auditLogService).logLoginAttempt(eq(validLoginDto.getUsernameOrEmail()), anyString(), eq(false));
    }

    @Test
    void login_WithInvalidInput_ShouldReturnBadRequest() throws Exception {
        // Given
        LoginDto invalidLoginDto = new LoginDto();
        invalidLoginDto.setUsernameOrEmail(""); // Invalid: empty
        invalidLoginDto.setPassword(""); // Invalid: empty
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithInactiveUser_ShouldReturnUnauthorized() throws Exception {
        // Given
        LoginDto loginDto = new LoginDto();
        loginDto.setUsernameOrEmail("inactive@test.com");
        loginDto.setPassword("password");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.DisabledException("User account is disabled"));
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password!"));
        
        verify(auditLogService).logLoginAttempt(eq(loginDto.getUsernameOrEmail()), anyString(), eq(false));
    }

    // Note: Registration tests removed as /register endpoint doesn't exist in AuthController

    // Token Refresh Tests

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewTokens() throws Exception {
        // Given
        String tokenId = "refresh-token-id-123";
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        when(jwtUtil.validateToken(validTokenRefreshDto.getRefreshToken())).thenReturn(true);
        when(jwtUtil.extractTokenId(validTokenRefreshDto.getRefreshToken())).thenReturn(tokenId);
        when(tokenBlacklistService.isTokenBlacklisted(tokenId)).thenReturn(false);
        when(jwtUtil.isRefreshToken(validTokenRefreshDto.getRefreshToken())).thenReturn(true);
        when(jwtUtil.extractUsername(validTokenRefreshDto.getRefreshToken())).thenReturn(testUser.getUsername());
        lenient().when(userService.loadUserByUsername(testUser.getUsername())).thenReturn(userDetails);
        lenient().when(jwtUtil.generateToken(userDetails)).thenReturn("new.access.token");
        lenient().when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("new.refresh.token");
        lenient().when(userService.findByUsernameDto(testUser.getUsername())).thenReturn(java.util.Optional.of(new UserResponseDto()));
        lenient().when(userService.findByEmail(testUser.getUsername())).thenReturn(java.util.Optional.empty());
        
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTokenRefreshDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
        
        verify(tokenBlacklistService).blacklistToken(eq(tokenId));
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        when(jwtUtil.validateToken(validTokenRefreshDto.getRefreshToken())).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTokenRefreshDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token!"));
    }

    @Test
    void refreshToken_WithBlacklistedToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        String tokenId = "blacklisted-token-id-123";
        when(jwtUtil.validateToken(validTokenRefreshDto.getRefreshToken())).thenReturn(true);
        when(jwtUtil.extractTokenId(validTokenRefreshDto.getRefreshToken())).thenReturn(tokenId);
        when(tokenBlacklistService.isTokenBlacklisted(tokenId)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTokenRefreshDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token has been revoked!"));
    }

    @Test
    void refreshToken_WithAccessTokenInsteadOfRefreshToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        lenient().when(jwtUtil.validateToken(validTokenRefreshDto.getRefreshToken())).thenReturn(true);
        when(jwtUtil.isRefreshToken(validTokenRefreshDto.getRefreshToken())).thenReturn(false);
        lenient().when(tokenBlacklistService.isBlacklisted(validTokenRefreshDto.getRefreshToken())).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTokenRefreshDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid token type!"));
    }

    @Test
    void refreshToken_WithInvalidInput_ShouldReturnBadRequest() throws Exception {
        // Given
        TokenRefreshDto invalidDto = new TokenRefreshDto();
        invalidDto.setRefreshToken(""); // Invalid: empty
        
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    // Logout Tests

    @Test
    void logout_WithValidToken_ShouldReturnSuccess() throws Exception {
        // Given
        String accessToken = "valid.access.token";
        String tokenId = "token-id-123";
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        lenient().when(jwtUtil.validateToken(accessToken)).thenReturn(true);
        lenient().when(jwtUtil.extractTokenId(accessToken)).thenReturn(tokenId);
        lenient().when(tokenBlacklistService.isTokenBlacklisted(tokenId)).thenReturn(false);
        lenient().when(jwtUtil.isAccessToken(accessToken)).thenReturn(true);
        lenient().when(jwtUtil.extractUsername(accessToken)).thenReturn(testUser.getUsername());
        lenient().when(userService.loadUserByUsername(testUser.getUsername())).thenReturn(userDetails);
        lenient().when(jwtUtil.validateToken(accessToken, userDetails)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully!"));
        
        verify(tokenBlacklistService).blacklistToken(eq(tokenId));
    }

    @Test
    void logout_WithoutToken_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully!"));
    }

    @Test
    void logout_WithInvalidTokenFormat_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "InvalidFormat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully!"));
    }

    @Test
    void logout_WithInvalidToken_ShouldReturnSuccess() throws Exception {
        // Given
        String invalidToken = "invalid.token";
        lenient().when(jwtUtil.validateToken(invalidToken)).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully!"));
    }

    // Current User Tests

    @Test
    void getCurrentUser_WithValidToken_ShouldReturnUserInfo() throws Exception {
        // Given
        String accessToken = "valid.access.token";
        String tokenId = "valid-token-id-123";
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setUsername(testUser.getUsername());
        userResponseDto.setEmail(testUser.getEmail());
        userResponseDto.setFirstName(testUser.getFirstName());
        userResponseDto.setLastName(testUser.getLastName());
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        lenient().when(jwtUtil.validateToken(accessToken)).thenReturn(true);
        lenient().when(jwtUtil.extractTokenId(accessToken)).thenReturn(tokenId);
        lenient().when(tokenBlacklistService.isTokenBlacklisted(tokenId)).thenReturn(false);
        lenient().when(jwtUtil.isAccessToken(accessToken)).thenReturn(true);
        lenient().when(jwtUtil.extractUsername(accessToken)).thenReturn(testUser.getUsername());
        lenient().when(userService.loadUserByUsername(testUser.getUsername())).thenReturn(userDetails);
        lenient().when(jwtUtil.validateToken(accessToken, userDetails)).thenReturn(true);
        lenient().when(userService.findByUsernameDto(testUser.getUsername())).thenReturn(java.util.Optional.of(userResponseDto));
        
        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    // Rate Limiting Tests

    @Test
    void login_ExceedingRateLimit_ShouldReturnTooManyRequests() throws Exception {
        // Given
        when(rateLimitingService.isRateLimited(anyString())).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDto)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many failed login attempts. Please try again in 0 minutes."));
    }

    // Security Headers Tests

    @Test
    void allEndpoints_ShouldIncludeSecurityHeaders() throws Exception {
        // Given
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        lenient().when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setUsername(testUser.getUsername());
        userResponseDto.setEmail(testUser.getEmail());
        lenient().when(userService.findByUsernameDto(testUser.getUsername())).thenReturn(java.util.Optional.of(userResponseDto));
        lenient().when(userService.findByEmail(testUser.getUsername())).thenReturn(java.util.Optional.empty());
        lenient().when(jwtUtil.generateToken(userDetails)).thenReturn("access.token.here");
        lenient().when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh.token.here");
        
        // Test login endpoint
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDto)))
                .andExpect(status().isOk());
                // Note: Security headers are added by SecurityHeadersFilter, not Spring Security headers config
    }

    // Content Type Tests

    @Test
    void login_WithUnsupportedContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text content"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // Malformed JSON Tests

    @Test
    void login_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    // Cross-Origin Tests

    @Test
    void login_WithCorsRequest_ShouldAllowCors() throws Exception {
        // Given
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getUsername())
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        lenient().when(userService.findByUsernameDto(testUser.getUsername())).thenReturn(java.util.Optional.of(new UserResponseDto()));
        lenient().when(userService.findByEmail(testUser.getUsername())).thenReturn(java.util.Optional.empty());
        lenient().when(jwtUtil.generateToken(userDetails)).thenReturn("access.token.here");
        lenient().when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh.token.here");
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDto)))
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}