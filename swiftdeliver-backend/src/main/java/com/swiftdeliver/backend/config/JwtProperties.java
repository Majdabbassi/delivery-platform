package com.swiftdeliver.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {
    
    @NotBlank(message = "JWT secret is required")
    private String secret = "REDACTED_JWT_SECRET";
    
    @Positive(message = "JWT expiration must be positive")
    private Long expiration = 86400000L; // 24 hours in milliseconds
    
    @NotBlank(message = "JWT issuer is required")
    private String issuer = "swiftdeliver-backend";
    
    @Data
    @ConfigurationProperties(prefix = "jwt.refresh")
    public static class Refresh {
        @Positive(message = "JWT refresh expiration must be positive")
        private Long expiration = 604800000L; // 7 days in milliseconds
    }
    
    private Refresh refresh = new Refresh();
}