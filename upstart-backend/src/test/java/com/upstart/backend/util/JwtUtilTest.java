package com.upstart.backend.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails testUserDetails;
    private UserDetails adminUserDetails;
    
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        
        // Set test values for @Value fields
        ReflectionTestUtils.setField(jwtUtil, "secret", "myTestSecretKey123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L); // 7 days
        ReflectionTestUtils.setField(jwtUtil, "issuer", "upstart-backend");
        
        // Create test user details
        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_CLIENT")
        );
        testUserDetails = new User("testuser", "password", authorities);
        
        Collection<GrantedAuthority> adminAuthorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")
        );
        adminUserDetails = new User("admin", "password", adminAuthorities);
    }
    
    // Token Generation Tests
    
    @Test
    void generateToken_WithValidUserDetails_ShouldReturnValidToken() {
        // When
        String token = jwtUtil.generateToken(testUserDetails);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
        
        // Verify token content
        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertEquals("access", jwtUtil.getTokenType(token));
        assertTrue(jwtUtil.isAccessToken(token));
        assertFalse(jwtUtil.isRefreshToken(token));
    }
    
    @Test
    void generateRefreshToken_WithValidUserDetails_ShouldReturnValidRefreshToken() {
        // When
        String refreshToken = jwtUtil.generateRefreshToken(testUserDetails);
        
        // Then
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertTrue(refreshToken.contains("."));
        
        // Verify token content
        assertEquals("testuser", jwtUtil.extractUsername(refreshToken));
        assertEquals("refresh", jwtUtil.getTokenType(refreshToken));
        assertTrue(jwtUtil.isRefreshToken(refreshToken));
        assertFalse(jwtUtil.isAccessToken(refreshToken));
    }
    
    @Test
    void generateToken_WithDifferentUsers_ShouldReturnDifferentTokens() {
        // When
        String token1 = jwtUtil.generateToken(testUserDetails);
        String token2 = jwtUtil.generateToken(adminUserDetails);
        
        // Then
        assertNotEquals(token1, token2);
        assertEquals("testuser", jwtUtil.extractUsername(token1));
        assertEquals("admin", jwtUtil.extractUsername(token2));
    }
    
    // Token Validation Tests
    
    @Test
    void validateToken_WithValidTokenAndUserDetails_ShouldReturnTrue() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        Boolean isValid = jwtUtil.validateToken(token, testUserDetails);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        Boolean isValid = jwtUtil.validateToken(token);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void validateToken_WithWrongUser_ShouldReturnFalse() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        Boolean isValid = jwtUtil.validateToken(token, adminUserDetails);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "invalid.token.format";
        
        // When
        Boolean isValid = jwtUtil.validateToken(malformedToken);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void validateToken_WithNullToken_ShouldReturnFalse() {
        // When
        Boolean isValid = jwtUtil.validateToken(null);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void validateToken_WithEmptyToken_ShouldReturnFalse() {
        // When
        Boolean isValid = jwtUtil.validateToken("");
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given - Create a token with very short expiration
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1L); // 1 millisecond
        String token = jwtUtil.generateToken(testUserDetails);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        Boolean isValid = jwtUtil.validateToken(token);
        
        // Then
        assertFalse(isValid);
    }
    
    // Token Extraction Tests
    
    @Test
    void extractUsername_WithValidToken_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        String username = jwtUtil.extractUsername(token);
        
        // Then
        assertEquals("testuser", username);
    }
    
    @Test
    void extractExpiration_WithValidToken_ShouldReturnFutureDate() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        Date expiration = jwtUtil.extractExpiration(token);
        
        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }
    
    @Test
    void extractIssuer_WithValidToken_ShouldReturnCorrectIssuer() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        String issuer = jwtUtil.extractIssuer(token);
        
        // Then
        assertEquals("upstart-backend", issuer);
    }
    
    @Test
    void extractTokenId_WithValidToken_ShouldReturnNonNullId() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        String tokenId = jwtUtil.extractTokenId(token);
        
        // Then
        assertNotNull(tokenId);
        assertFalse(tokenId.isEmpty());
    }
    
    @Test
    void extractTokenId_WithDifferentTokens_ShouldReturnDifferentIds() {
        // Given
        String token1 = jwtUtil.generateToken(testUserDetails);
        String token2 = jwtUtil.generateToken(testUserDetails);
        
        // When
        String tokenId1 = jwtUtil.extractTokenId(token1);
        String tokenId2 = jwtUtil.extractTokenId(token2);
        
        // Then
        assertNotEquals(tokenId1, tokenId2);
    }
    
    @Test
    void extractRoles_WithValidToken_ShouldReturnCorrectRoles() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        List<String> roles = jwtUtil.extractRoles(token);
        
        // Then
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_CLIENT"));
    }
    
    @Test
    void extractRoles_WithAdminToken_ShouldReturnAdminRoles() {
        // Given
        String token = jwtUtil.generateToken(adminUserDetails);
        
        // When
        List<String> roles = jwtUtil.extractRoles(token);
        
        // Then
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_SUPER_ADMIN"));
    }
    
    @Test
    void extractRoles_WithRefreshToken_ShouldReturnNull() {
        // Given
        String refreshToken = jwtUtil.generateRefreshToken(testUserDetails);
        
        // When
        List<String> roles = jwtUtil.extractRoles(refreshToken);
        
        // Then
        assertNull(roles);
    }
    
    // Token Type Tests
    
    @Test
    void getTokenType_WithAccessToken_ShouldReturnAccess() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        String tokenType = jwtUtil.getTokenType(token);
        
        // Then
        assertEquals("access", tokenType);
    }
    
    @Test
    void getTokenType_WithRefreshToken_ShouldReturnRefresh() {
        // Given
        String refreshToken = jwtUtil.generateRefreshToken(testUserDetails);
        
        // When
        String tokenType = jwtUtil.getTokenType(refreshToken);
        
        // Then
        assertEquals("refresh", tokenType);
    }
    
    @Test
    void isAccessToken_WithAccessToken_ShouldReturnTrue() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        boolean isAccessToken = jwtUtil.isAccessToken(token);
        
        // Then
        assertTrue(isAccessToken);
    }
    
    @Test
    void isAccessToken_WithRefreshToken_ShouldReturnFalse() {
        // Given
        String refreshToken = jwtUtil.generateRefreshToken(testUserDetails);
        
        // When
        boolean isAccessToken = jwtUtil.isAccessToken(refreshToken);
        
        // Then
        assertFalse(isAccessToken);
    }
    
    @Test
    void isRefreshToken_WithRefreshToken_ShouldReturnTrue() {
        // Given
        String refreshToken = jwtUtil.generateRefreshToken(testUserDetails);
        
        // When
        boolean isRefreshToken = jwtUtil.isRefreshToken(refreshToken);
        
        // Then
        assertTrue(isRefreshToken);
    }
    
    @Test
    void isRefreshToken_WithAccessToken_ShouldReturnFalse() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        boolean isRefreshToken = jwtUtil.isRefreshToken(token);
        
        // Then
        assertFalse(isRefreshToken);
    }
    
    // Configuration Tests
    
    @Test
    void getExpirationTime_ShouldReturnConfiguredValue() {
        // When
        Long expirationTime = jwtUtil.getExpirationTime();
        
        // Then
        assertEquals(86400000L, expirationTime);
    }
    
    @Test
    void getRefreshExpirationTime_ShouldReturnConfiguredValue() {
        // When
        Long refreshExpirationTime = jwtUtil.getRefreshExpirationTime();
        
        // Then
        assertEquals(604800000L, refreshExpirationTime);
    }
    
    // Edge Cases and Error Handling Tests
    
    @Test
    void extractUsername_WithMalformedToken_ShouldThrowException() {
        // Given
        String malformedToken = "invalid.token";
        
        // When & Then
        assertThrows(Exception.class, () -> jwtUtil.extractUsername(malformedToken));
    }
    
    @Test
    void extractClaim_WithValidToken_ShouldReturnCorrectClaim() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When
        String subject = jwtUtil.extractClaim(token, Claims::getSubject);
        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
        
        // Then
        assertEquals("testuser", subject);
        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()));
    }
    
    @Test
    void validateToken_WithTokenFromDifferentIssuer_ShouldReturnFalse() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        ReflectionTestUtils.setField(jwtUtil, "issuer", "different-issuer");
        
        // When
        Boolean isValid = jwtUtil.validateToken(token);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void generateToken_WithUserWithoutAuthorities_ShouldGenerateTokenWithEmptyRoles() {
        // Given
        UserDetails userWithoutRoles = new User("noRoleUser", "password", Arrays.asList());
        
        // When
        String token = jwtUtil.generateToken(userWithoutRoles);
        List<String> roles = jwtUtil.extractRoles(token);
        
        // Then
        assertNotNull(token);
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }
    
    @Test
    void tokenGeneration_ShouldIncludeAllRequiredClaims() {
        // Given
        String token = jwtUtil.generateToken(testUserDetails);
        
        // When & Then
        assertNotNull(jwtUtil.extractUsername(token));
        assertNotNull(jwtUtil.extractExpiration(token));
        assertNotNull(jwtUtil.extractIssuer(token));
        assertNotNull(jwtUtil.extractTokenId(token));
        assertNotNull(jwtUtil.getTokenType(token));
        assertNotNull(jwtUtil.extractRoles(token));
    }
}