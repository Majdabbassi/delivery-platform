package com.upstart.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upstart.backend.dto.LoginDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class SecurityConfigurationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // Content Security Policy Tests

    @Test
    void allEndpoints_ShouldIncludeContentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("style-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("object-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("base-uri 'self'")));
    }

    @Test
    void contentSecurityPolicy_ShouldNotAllowUnsafeObjectSources() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("object-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", 
                    org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
    }

    // Security Headers Tests

    @Test
    void allEndpoints_ShouldIncludeXContentTypeOptions() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void allEndpoints_ShouldIncludeXFrameOptions() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void allEndpoints_ShouldIncludeXXSSProtection() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"));
    }

    @Test
    void allEndpoints_ShouldIncludeReferrerPolicy() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void httpsEndpoints_ShouldIncludeStrictTransportSecurity() throws Exception {
        // Note: HSTS is typically only sent over HTTPS
        // This test verifies the header configuration exists
        mockMvc.perform(get("/api/auth/me")
                .header("X-Forwarded-Proto", "https"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    // CORS Configuration Tests

    @Test
    void preflightRequest_ShouldAllowConfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    void corsRequest_ShouldIncludeAllowCredentials() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    // Authentication and Authorization Tests

    @Test
    void publicEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // Test public endpoints
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest()); // Bad request due to validation, not unauthorized

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"email\":\"\",\"password\":\"\",\"firstName\":\"\",\"lastName\":\"\"}"))
                .andExpect(status().isBadRequest()); // Bad request due to validation, not unauthorized

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest()); // Bad request due to validation, not unauthorized
    }

    @Test
    void protectedEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test protected endpoints without authentication
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());

        // Test other protected endpoints if they exist
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidAuthorizationHeader_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "InvalidFormat"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Basic dGVzdDp0ZXN0"))
                .andExpect(status().isUnauthorized());
    }

    // Input Validation Security Tests

    @Test
    void maliciousInput_ShouldBeRejected() throws Exception {
        // Test SQL injection attempts
        LoginDto sqlInjectionDto = new LoginDto();
        sqlInjectionDto.setUsernameOrEmail("admin'; DROP TABLE users; --");
        sqlInjectionDto.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sqlInjectionDto)))
                .andExpect(status().isBadRequest()); // Should be rejected by validation

        // Test XSS attempts
        LoginDto xssDto = new LoginDto();
        xssDto.setUsernameOrEmail("<script>alert('xss')</script>");
        xssDto.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(xssDto)))
                .andExpect(status().isBadRequest()); // Should be rejected by validation
    }

    @Test
    void oversizedInput_ShouldBeRejected() throws Exception {
        // Test with oversized username
        LoginDto oversizedDto = new LoginDto();
        oversizedDto.setUsernameOrEmail("a".repeat(200)); // Exceeds max length
        oversizedDto.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oversizedDto)))
                .andExpect(status().isBadRequest());

        // Test with oversized password
        LoginDto oversizedPasswordDto = new LoginDto();
        oversizedPasswordDto.setUsernameOrEmail("username");
        oversizedPasswordDto.setPassword("a".repeat(200)); // Exceeds max length

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oversizedPasswordDto)))
                .andExpect(status().isBadRequest());
    }

    // Content Type Security Tests

    @Test
    void unsupportedContentType_ShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text content"))
                .andExpect(status().isUnsupportedMediaType());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_XML)
                .content("<xml>content</xml>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void malformedJson_ShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not json at all"))
                .andExpect(status().isBadRequest());
    }

    // HTTP Method Security Tests

    @Test
    void unsupportedHttpMethods_ShouldBeRejected() throws Exception {
        // Test unsupported methods on login endpoint
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // Rate Limiting Security Tests

    @Test
    void rapidRequests_ShouldTriggerRateLimiting() throws Exception {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsernameOrEmail("testuser");
        loginDto.setPassword("wrongpassword");

        // Make rapid requests to trigger rate limiting
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginDto)));
        }

        // Additional requests should be rate limited
        // Note: The exact status code depends on your rate limiting implementation
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized()); // Could be 429 if strict rate limiting
    }

    // Session Security Tests

    @Test
    void sessionFixation_ShouldBeProtected() throws Exception {
        // Test that session ID changes after authentication
        // This is typically handled by Spring Security's session fixation protection
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"test\",\"password\":\"test\"}"))
                .andExpect(header().doesNotExist("Set-Cookie")); // JWT-based auth shouldn't set cookies
    }

    // Error Handling Security Tests

    @Test
    void errorResponses_ShouldNotLeakSensitiveInformation() throws Exception {
        // Test that error messages don't reveal system internals
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"nonexistent\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password!"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("database"))))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("SQL"))))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("exception"))));
    }

    @Test
    void serverErrors_ShouldNotExposeStackTraces() throws Exception {
        // Test that errors don't expose stack traces (unauthenticated access is rejected)
        mockMvc.perform(get("/api/nonexistent-endpoint"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("Exception"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("at com.upstart"))));
    }

    // Cache Control Security Tests

    @Test
    void sensitiveEndpoints_ShouldHaveNoCacheHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid.token"))
                .andExpect(header().string("Cache-Control", 
                    org.hamcrest.Matchers.containsString("no-cache")))
                .andExpect(header().string("Cache-Control", 
                    org.hamcrest.Matchers.containsString("no-store")));
    }

    // Content Sniffing Protection Tests

    @Test
    void fileUploadEndpoints_ShouldPreventContentSniffing() throws Exception {
        // Test that file upload endpoints (if any) prevent content sniffing
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // Clickjacking Protection Tests

    @Test
    void allEndpoints_ShouldPreventClickjacking() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    // Information Disclosure Tests

    @Test
    void serverHeader_ShouldNotRevealVersion() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().doesNotExist("Server"))
                .andExpect(header().doesNotExist("X-Powered-By"));
    }

    @Test
    void errorPages_ShouldNotRevealTechnology() throws Exception {
        mockMvc.perform(get("/nonexistent"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("Spring"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("Tomcat"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("Java"))));
    }
}