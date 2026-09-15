package com.swiftdeliver.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    @Value("${jwt.refresh.expiration:604800000}") // 7 days in milliseconds
    private Long refreshExpiration;
    
    @Value("${jwt.issuer:swiftdeliver-backend}")
    private String issuer;
    
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("jti", generateTokenId()); // JWT ID for token tracking
        return createToken(claims, userDetails.getUsername(), expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("jti", generateTokenId()); // JWT ID for token tracking
        return createToken(claims, userDetails.getUsername(), refreshExpiration);
    }
    
    private String generateTokenId() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .audience().add("swiftdeliver-app").and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .notBefore(new Date(System.currentTimeMillis()))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String tokenIssuer = extractIssuer(token);
            return (username.equals(userDetails.getUsername()) 
                    && !isTokenExpired(token) 
                    && issuer.equals(tokenIssuer)
                    && isValidAudience(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !isTokenExpired(token) 
                    && issuer.equals(claims.getIssuer())
                    && isValidAudience(token);
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return false;
        }
    }

    public String getTokenType(String token) {
        Claims claims = extractAllClaims(token);
        return (String) claims.get("type");
    }
    
    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }
    
    public String extractTokenId(String token) {
        Claims claims = extractAllClaims(token);
        return (String) claims.get("jti");
    }
    
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("roles");
    }
    
    private boolean isValidAudience(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Set<String> audiences = claims.getAudience();
            return audiences != null && audiences.contains("swiftdeliver-app");
        } catch (Exception e) {
            log.warn("Failed to validate audience: {}", e.getMessage());
            return false;
        }
    }

    public Long getExpirationTime() {
        return expiration;
    }

    public Long getRefreshExpirationTime() {
        return refreshExpiration;
    }
    
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }
    
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }
}