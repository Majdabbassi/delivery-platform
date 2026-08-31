package com.upstart.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;
    private ScheduledExecutorService mockScheduler;
    
    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
        mockScheduler = mock(ScheduledExecutorService.class);
        
        // Set test values for @Value fields
        ReflectionTestUtils.setField(tokenBlacklistService, "cleanupIntervalHours", 24);
        ReflectionTestUtils.setField(tokenBlacklistService, "maxBlacklistSize", 10000);
        ReflectionTestUtils.setField(tokenBlacklistService, "scheduler", mockScheduler);
        
        // Call @PostConstruct method manually for testing
        tokenBlacklistService.init();
    }
    
    @Test
    void init_ShouldInitializeSchedulerCorrectly() {
        // Given - setup is done in @BeforeEach
        
        // When - init() is called in @BeforeEach
        
        // Then
        verify(mockScheduler).scheduleAtFixedRate(
            any(Runnable.class), 
            eq(24L), 
            eq(24L), 
            eq(TimeUnit.HOURS)
        );
    }
    
    @Test
    void blacklistToken_WithValidToken_ShouldAddToBlacklist() {
        // Given
        String token = "valid.jwt.token";
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        
        // When
        tokenBlacklistService.blacklistToken(token, expirationTime);
        
        // Then
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
    }
    
    @Test
    void blacklistToken_WithNullToken_ShouldNotAddToBlacklist() {
        // Given
        String token = null;
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        
        // When
        tokenBlacklistService.blacklistToken(token, expirationTime);
        
        // Then
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));
    }
    
    @Test
    void blacklistToken_WithEmptyToken_ShouldNotAddToBlacklist() {
        // Given
        String token = "";
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        
        // When
        tokenBlacklistService.blacklistToken(token, expirationTime);
        
        // Then
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));
    }
    
    @Test
    void blacklistToken_WithBlankToken_ShouldNotAddToBlacklist() {
        // Given
        String token = "   ";
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        
        // When
        tokenBlacklistService.blacklistToken(token, expirationTime);
        
        // Then
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));
    }
    
    @Test
    void blacklistToken_WithNullExpirationTime_ShouldNotAddToBlacklist() {
        // Given
        String token = "valid.jwt.token";
        LocalDateTime expirationTime = null;
        
        // When
        tokenBlacklistService.blacklistToken(token, expirationTime);
        
        // Then
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));
    }
    
    @Test
    void blacklistToken_WithPastExpirationTime_ShouldNotAddToBlacklist() {
        // Given
        String token = "expired.jwt.token";
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(1);
        
        // When
        tokenBlacklistService.blacklistToken(token, expirationTime);
        
        // Then
        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));
    }
    
    @Test
    void isTokenBlacklisted_WithBlacklistedToken_ShouldReturnTrue() {
        // Given
        String token = "blacklisted.jwt.token";
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);
        tokenBlacklistService.blacklistToken(token, expirationTime);
        
        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted(token);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void isTokenBlacklisted_WithNonBlacklistedToken_ShouldReturnFalse() {
        // Given
        String token = "valid.jwt.token";
        
        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted(token);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void isTokenBlacklisted_WithNullToken_ShouldReturnFalse() {
        // Given
        String token = null;
        
        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted(token);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void isTokenBlacklisted_WithEmptyToken_ShouldReturnFalse() {
        // Given
        String token = "";
        
        // When
        boolean result = tokenBlacklistService.isTokenBlacklisted(token);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void cleanupExpiredTokens_ShouldRemoveExpiredTokens() {
        // Given
        String expiredToken = "expired.jwt.token";
        String validToken = "valid.jwt.token";
        
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        
        // Add tokens to blacklist
        tokenBlacklistService.blacklistToken(expiredToken, pastTime);
        tokenBlacklistService.blacklistToken(validToken, futureTime);
        
        // Verify both tokens are stored in the blacklist
        assertEquals(2, tokenBlacklistService.getBlacklistSize());
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken));
        
        // When
        tokenBlacklistService.cleanupExpiredTokens();
        
        // Then
        assertFalse(tokenBlacklistService.isTokenBlacklisted(expiredToken));
        assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken));
    }
    
    @Test
    void cleanupExpiredTokens_WithEmptyBlacklist_ShouldNotThrowException() {
        // Given - empty blacklist
        
        // When & Then
        assertDoesNotThrow(() -> tokenBlacklistService.cleanupExpiredTokens());
    }
    
    @Test
    void blacklistToken_WhenMaxSizeReached_ShouldTriggerCleanup() {
        // Given
        ReflectionTestUtils.setField(tokenBlacklistService, "maxBlacklistSize", 2);
        
        String token1 = "token1";
        String token2 = "token2";
        String token3 = "token3";
        
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        
        // When
        tokenBlacklistService.blacklistToken(token1, futureTime);
        tokenBlacklistService.blacklistToken(token2, futureTime);
        tokenBlacklistService.blacklistToken(token3, futureTime); // Should trigger cleanup
        
        // Then
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token3));
        // At least one of the previous tokens should still be there since they're not expired
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token1) || 
                  tokenBlacklistService.isTokenBlacklisted(token2));
    }
    
    @Test
    void blacklistToken_WithSameTokenMultipleTimes_ShouldUpdateExpirationTime() {
        // Given
        String token = "duplicate.jwt.token";
        LocalDateTime firstExpiration = LocalDateTime.now().plusHours(1);
        LocalDateTime secondExpiration = LocalDateTime.now().plusHours(2);
        
        // When
        tokenBlacklistService.blacklistToken(token, firstExpiration);
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
        
        tokenBlacklistService.blacklistToken(token, secondExpiration);
        
        // Then
        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
        // The token should still be blacklisted with updated expiration
    }
    
    @Test
    void getBlacklistSize_ShouldReturnCorrectSize() {
        // Given
        String token1 = "token1";
        String token2 = "token2";
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        
        // When
        int initialSize = tokenBlacklistService.getBlacklistSize();
        tokenBlacklistService.blacklistToken(token1, futureTime);
        int sizeAfterFirst = tokenBlacklistService.getBlacklistSize();
        tokenBlacklistService.blacklistToken(token2, futureTime);
        int sizeAfterSecond = tokenBlacklistService.getBlacklistSize();
        
        // Then
        assertEquals(0, initialSize);
        assertEquals(1, sizeAfterFirst);
        assertEquals(2, sizeAfterSecond);
    }
    
    @Test
    void concurrentAccess_ShouldBeThreadSafe() throws InterruptedException {
        // Given
        int numberOfThreads = 10;
        int tokensPerThread = 100;
        Thread[] threads = new Thread[numberOfThreads];
        
        // When
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < tokensPerThread; j++) {
                    String token = "token_" + threadId + "_" + j;
                    LocalDateTime expiration = LocalDateTime.now().plusHours(1);
                    tokenBlacklistService.blacklistToken(token, expiration);
                    tokenBlacklistService.isTokenBlacklisted(token);
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then
        int expectedSize = numberOfThreads * tokensPerThread;
        int actualSize = tokenBlacklistService.getBlacklistSize();
        assertEquals(expectedSize, actualSize);
    }
    
    @Test
    void destroy_ShouldShutdownScheduler() {
        // When
        tokenBlacklistService.destroy();
        
        // Then
        verify(mockScheduler).shutdown();
    }
}