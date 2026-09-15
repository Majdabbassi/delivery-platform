package com.swiftdeliver.backend.util;

import com.swiftdeliver.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUtilTest {

    private static final String SECRET =
            "test-jwt-secret-test-jwt-secret-test-jwt-secret-1234567890";

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86_400_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604_800_000L);
        ReflectionTestUtils.setField(jwtUtil, "issuer", "swiftdeliver-backend");

        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("driver.alice");
        when(userDetails.getAuthorities())
                .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
    }

    @Test
    @DisplayName("generateToken produces an access token that validates")
    void generateAccessToken() {
        String token = jwtUtil.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.validateToken(token, userDetails)).isTrue();
        assertThat(jwtUtil.isAccessToken(token)).isTrue();
        assertThat(jwtUtil.isRefreshToken(token)).isFalse();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("driver.alice");
        assertThat(jwtUtil.extractRoles(token)).containsExactly("ROLE_DRIVER");
    }

    @Test
    @DisplayName("generateRefreshToken produces a refresh token that does not validate as access")
    void generateRefreshToken() {
        String token = jwtUtil.generateRefreshToken(userDetails);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
        assertThat(jwtUtil.isAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("validating an access token against a different user fails")
    void validateAgainstWrongUser() {
        String token = jwtUtil.generateToken(userDetails);

        UserDetails other = mock(UserDetails.class);
        when(other.getUsername()).thenReturn("someone.else");

        assertThat(jwtUtil.validateToken(token, other)).isFalse();
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredTokenRejected() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -60_000L);
        String token = jwtUtil.generateToken(userDetails);

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void tamperedTokenRejected() {
        String token = jwtUtil.generateToken(userDetails);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void wrongSecretRejected() {
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret",
                "another-secret-another-secret-another-secret-xyz");
        ReflectionTestUtils.setField(other, "expiration", 86_400_000L);
        ReflectionTestUtils.setField(other, "refreshExpiration", 604_800_000L);
        ReflectionTestUtils.setField(other, "issuer", "swiftdeliver-backend");

        String token = other.generateToken(userDetails);

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }
}