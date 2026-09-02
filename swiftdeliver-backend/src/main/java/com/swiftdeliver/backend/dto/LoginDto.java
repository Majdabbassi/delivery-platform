package com.swiftdeliver.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {
    
    @NotBlank(message = "Username or email is required")
    @Size(min = 3, max = 100, message = "Username or email must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9@._-]+$", message = "Username or email contains invalid characters")
    private String usernameOrEmail;
    
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 128, message = "Password length is invalid")
    private String password;
}