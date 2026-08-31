package com.upstart.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshDto {
    
    @NotBlank(message = "Refresh token is required")
    @Size(min = 10, max = 2048, message = "Invalid token format")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Token contains invalid characters")
    private String refreshToken;
}