package com.swiftdeliver.backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Refuses to start with a JWT signing secret that is public or too short, so a forgotten
 * environment variable cannot silently make every token forgeable.
 *
 * <p>Under the {@code dev} / {@code test} profiles a weak secret only logs a warning, because the
 * local Docker quick start ships a development secret; any other profile (e.g. {@code prod})
 * refuses to boot.
 */
@Component
public class JwtSecretGuard {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretGuard.class);

    /** HS256 needs at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    /** Values that appear in the README, the examples or the defaults of this repository. */
    static final Set<String> PUBLIC_SECRETS = Set.of(
            "mySecretKey123456789012345678901234567890",
            "your_256bit_secret",
            "CHANGE_ME");

    private final String secret;
    private final Environment environment;

    // The property itself rather than JwtProperties: that class is registered twice (as a @Component
    // and through @ConfigurationProperties), which makes injecting it by type ambiguous.
    public JwtSecretGuard(@Value("${jwt.secret:}") String secret, Environment environment) {
        this.secret = secret;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        String problem = problemWith(secret);
        if (problem == null) {
            return;
        }
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            log.warn("JWT secret: {}. Acceptable for a local run only: set JWT_SECRET before deploying.", problem);
            return;
        }
        throw new IllegalStateException("Refusing to start: the JWT secret " + problem
                + ". Set JWT_SECRET to a random value of at least " + MIN_SECRET_BYTES
                + " bytes (for example `openssl rand -hex 32`).");
    }

    /** @return a description of what is wrong with the secret, or null when it is acceptable. */
    static String problemWith(String secret) {
        if (secret == null || secret.isBlank()) {
            return "is empty";
        }
        if (PUBLIC_SECRETS.contains(secret)) {
            return "is a publicly known default";
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            return "is shorter than " + MIN_SECRET_BYTES + " bytes";
        }
        return null;
    }
}
