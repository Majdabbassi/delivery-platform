package com.upstart.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordValidationService {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    
    // Common weak passwords to check against
    private static final List<String> COMMON_PASSWORDS = List.of(
        "password", "123456", "password123", "admin", "qwerty",
        "letmein", "welcome", "monkey", "1234567890", "abc123",
        "password1", "123456789", "welcome123", "admin123"
    );
    
    // Regex patterns for password validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':,.<>?]");
    private static final Pattern SEQUENTIAL_PATTERN = Pattern.compile("(012|123|234|345|456|567|678|789|890|abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz)");
    private static final Pattern REPEATED_PATTERN = Pattern.compile("(.)\\1{2,}");
    
    public PasswordValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.trim().isEmpty()) {
            errors.add("Password cannot be empty");
            return new PasswordValidationResult(false, errors);
        }
        
        // Length validation
        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        
        if (password.length() > MAX_LENGTH) {
            errors.add("Password must not exceed " + MAX_LENGTH + " characters");
        }
        
        // Character type validation
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        
        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{};':,.<>?)");
        }
        
        // Common password validation
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("Password is too common and easily guessable");
        }
        
        // Sequential characters validation
        if (SEQUENTIAL_PATTERN.matcher(password.toLowerCase()).find()) {
            errors.add("Password should not contain sequential characters");
        }
        
        // Repeated characters validation
        if (REPEATED_PATTERN.matcher(password).find()) {
            errors.add("Password should not contain more than 2 consecutive identical characters");
        }
        
        // Check for username in password (would need username parameter)
        // This can be added when called from registration/password change endpoints
        
        return new PasswordValidationResult(errors.isEmpty(), errors);
    }
    
    public PasswordValidationResult validatePasswordWithUsername(String password, String username) {
        PasswordValidationResult result = validatePassword(password);
        
        if (username != null && !username.trim().isEmpty()) {
            if (password.toLowerCase().contains(username.toLowerCase())) {
                result.getErrors().add("Password should not contain the username");
                result.setValid(false);
            }
        }
        
        return result;
    }
    
    public static class PasswordValidationResult {
        private boolean valid;
        private List<String> errors;
        
        public PasswordValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}