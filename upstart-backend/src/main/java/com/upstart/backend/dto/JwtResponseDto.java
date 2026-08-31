package com.upstart.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserResponseDto user;
    private LocalDateTime timestamp;
    private String message;
    
    public JwtResponseDto(String accessToken, String refreshToken, Long expiresIn, UserResponseDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.timestamp = LocalDateTime.now();
        this.message = "Authentication successful";
    }
}