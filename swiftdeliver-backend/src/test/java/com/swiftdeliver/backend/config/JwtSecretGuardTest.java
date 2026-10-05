package com.swiftdeliver.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretGuardTest {

    private static final String STRONG = "a-random-looking-secret-with-more-than-32-bytes";

    private static MockEnvironment withProfile(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return environment;
    }

    @Test
    void aLongRandomSecretIsAccepted() {
        assertNull(JwtSecretGuard.problemWith(STRONG));
    }

    @Test
    void emptyShortAndPubliclyKnownSecretsAreProblems() {
        assertNotNull(JwtSecretGuard.problemWith(""));
        assertNotNull(JwtSecretGuard.problemWith(null));
        assertNotNull(JwtSecretGuard.problemWith("your_256bit_secret"));
        assertNotNull(JwtSecretGuard.problemWith("short"));
        // The default that used to be baked into application.yml and printed in the README.
        assertNotNull(JwtSecretGuard.problemWith("mySecretKey123456789012345678901234567890"));
    }

    // Regression: a forgotten JWT_SECRET silently fell back to a public value and every token was forgeable.
    @Test
    void productionRefusesToStartWithAWeakSecret() {
        JwtSecretGuard guard = new JwtSecretGuard("mySecretKey123456789012345678901234567890", withProfile("prod"));
        assertThrows(IllegalStateException.class, guard::validate);
    }

    @Test
    void developmentOnlyWarns() {
        JwtSecretGuard guard = new JwtSecretGuard("mySecretKey123456789012345678901234567890", withProfile("dev"));
        assertDoesNotThrow(guard::validate);
    }

    @Test
    void productionStartsWithAStrongSecret() {
        assertDoesNotThrow(new JwtSecretGuard(STRONG, withProfile("prod"))::validate);
    }
}
