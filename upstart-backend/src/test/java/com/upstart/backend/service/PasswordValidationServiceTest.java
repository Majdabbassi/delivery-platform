package com.upstart.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PasswordValidationServiceTest {

    private PasswordValidationService passwordValidationService;
    
    @BeforeEach
    void setUp() {
        passwordValidationService = new PasswordValidationService();
    }
    
    // Valid Password Tests
    
    @Test
    void validatePassword_WithValidPassword_ShouldReturnValid() {
        // Given
        String validPassword = "S3cure!Pass";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(validPassword);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("", result.getErrorMessage());
    }
    
    @Test
    void validatePassword_WithAnotherValidPassword_ShouldReturnValid() {
        // Given
        String validPassword = "MyStr0ng@Password";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(validPassword);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    // Length Validation Tests
    
    @Test
    void validatePassword_WithTooShortPassword_ShouldReturnInvalid() {
        // Given
        String shortPassword = "Abc1!";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(shortPassword);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must be at least 8 characters long"));
    }
    
    @Test
    void validatePassword_WithMinimumValidLength_ShouldReturnValid() {
        // Given
        String minLengthPassword = "A9!vB3xq";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(minLengthPassword);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void validatePassword_WithTooLongPassword_ShouldReturnInvalid() {
        // Given
        String longPassword = "A".repeat(120) + "bcde123!" + "X".repeat(10); // 139 characters
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(longPassword);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must not exceed 128 characters"));
    }
    
    @Test
    void validatePassword_WithMaximumValidLength_ShouldReturnValid() {
        // Given
        String maxLengthPassword = "Aa".repeat(63) + "1!"; // 128 characters, no sequential/repeated runs
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(maxLengthPassword);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    // Character Type Validation Tests
    
    @Test
    void validatePassword_WithoutUppercase_ShouldReturnInvalid() {
        // Given
        String noUppercasePassword = "lowercase123!";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(noUppercasePassword);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one uppercase letter"));
    }
    
    @Test
    void validatePassword_WithoutLowercase_ShouldReturnInvalid() {
        // Given
        String noLowercasePassword = "UPPERCASE123!";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(noLowercasePassword);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one lowercase letter"));
    }
    
    @Test
    void validatePassword_WithoutDigit_ShouldReturnInvalid() {
        // Given
        String noDigitPassword = "NoDigitsHere!";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(noDigitPassword);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one digit"));
    }
    
    @Test
    void validatePassword_WithoutSpecialCharacter_ShouldReturnInvalid() {
        // Given
        String noSpecialCharPassword = "NoSpecialChar123";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(noSpecialCharPassword);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password must contain at least one special character (!@#$%^&*()_+-=[]{};':,.<>?)"));
    }
    
    @Test
    void validatePassword_WithAllSpecialCharacters_ShouldReturnValid() {
        // Given
        String[] specialChars = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+", "-", "=", "[", "]", "{", "}", ";", "'", ":", ",", ".", "<", ">", "?"};
        
        for (String specialChar : specialChars) {
            String passwordWithSpecialChar = "P4ssw0rd" + specialChar;
            
            // When
            PasswordValidationService.PasswordValidationResult result = 
                    passwordValidationService.validatePassword(passwordWithSpecialChar);
            
            // Then
            assertTrue(result.isValid(), "Password with special character '" + specialChar + "' should be valid");
        }
    }
    
    // Common Password Validation Tests
    
    @Test
    void validatePassword_WithCommonPassword_ShouldReturnInvalid() {
        // Given
        String[] commonPasswords = {"password", "123456", "password123", "admin", "qwerty", "letmein", "welcome", "monkey"};
        
        for (String commonPassword : commonPasswords) {
            // When
            PasswordValidationService.PasswordValidationResult result = 
                    passwordValidationService.validatePassword(commonPassword);
            
            // Then
            assertFalse(result.isValid(), "Common password '" + commonPassword + "' should be invalid");
            assertTrue(result.getErrors().contains("Password is too common and easily guessable"));
        }
    }
    
    @Test
    void validatePassword_WithCommonPasswordDifferentCase_ShouldReturnInvalid() {
        // Given
        String commonPasswordUpperCase = "PASSWORD";
        String commonPasswordMixedCase = "PaSSwoRd";
        
        // When
        PasswordValidationService.PasswordValidationResult result1 = 
                passwordValidationService.validatePassword(commonPasswordUpperCase);
        PasswordValidationService.PasswordValidationResult result2 = 
                passwordValidationService.validatePassword(commonPasswordMixedCase);
        
        // Then
        assertFalse(result1.isValid());
        assertFalse(result2.isValid());
        assertTrue(result1.getErrors().contains("Password is too common and easily guessable"));
        assertTrue(result2.getErrors().contains("Password is too common and easily guessable"));
    }
    
    // Sequential Characters Validation Tests
    
    @Test
    void validatePassword_WithSequentialNumbers_ShouldReturnInvalid() {
        // Given
        String[] sequentialPasswords = {"Password123!", "Secure456Pass!", "Test789Word!"};
        
        for (String sequentialPassword : sequentialPasswords) {
            // When
            PasswordValidationService.PasswordValidationResult result = 
                    passwordValidationService.validatePassword(sequentialPassword);
            
            // Then
            assertFalse(result.isValid(), "Password with sequential numbers '" + sequentialPassword + "' should be invalid");
            assertTrue(result.getErrors().contains("Password should not contain sequential characters"));
        }
    }
    
    @Test
    void validatePassword_WithSequentialLetters_ShouldReturnInvalid() {
        // Given
        String[] sequentialPasswords = {"Passwordabc1!", "Securedef2!", "Testxyz3!"};
        
        for (String sequentialPassword : sequentialPasswords) {
            // When
            PasswordValidationService.PasswordValidationResult result = 
                    passwordValidationService.validatePassword(sequentialPassword);
            
            // Then
            assertFalse(result.isValid(), "Password with sequential letters '" + sequentialPassword + "' should be invalid");
            assertTrue(result.getErrors().contains("Password should not contain sequential characters"));
        }
    }
    
    // Repeated Characters Validation Tests
    
    @Test
    void validatePassword_WithRepeatedCharacters_ShouldReturnInvalid() {
        // Given
        String[] repeatedPasswords = {"Passwordaaa1!", "Secure111Pass!", "Test@@@Word!"};
        
        for (String repeatedPassword : repeatedPasswords) {
            // When
            PasswordValidationService.PasswordValidationResult result = 
                    passwordValidationService.validatePassword(repeatedPassword);
            
            // Then
            assertFalse(result.isValid(), "Password with repeated characters '" + repeatedPassword + "' should be invalid");
            assertTrue(result.getErrors().contains("Password should not contain more than 2 consecutive identical characters"));
        }
    }
    
    @Test
    void validatePassword_WithTwoConsecutiveCharacters_ShouldReturnValid() {
        // Given
        String passwordWithTwoConsecutive = "Password11!";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(passwordWithTwoConsecutive);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    // Null and Empty Password Tests
    
    @Test
    void validatePassword_WithNullPassword_ShouldReturnInvalid() {
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(null);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password cannot be empty"));
    }
    
    @Test
    void validatePassword_WithEmptyPassword_ShouldReturnInvalid() {
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword("");
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password cannot be empty"));
    }
    
    @Test
    void validatePassword_WithWhitespaceOnlyPassword_ShouldReturnInvalid() {
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword("   ");
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password cannot be empty"));
    }
    
    // Username Validation Tests
    
    @Test
    void validatePasswordWithUsername_WithValidPasswordAndUsername_ShouldReturnValid() {
        // Given
        String password = "S3cure!Pass";
        String username = "testuser";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePasswordWithUsername(password, username);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void validatePasswordWithUsername_WithPasswordContainingUsername_ShouldReturnInvalid() {
        // Given
        String password = "testuserPassword123!";
        String username = "testuser";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePasswordWithUsername(password, username);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password should not contain the username"));
    }
    
    @Test
    void validatePasswordWithUsername_WithPasswordContainingUsernameDifferentCase_ShouldReturnInvalid() {
        // Given
        String password = "TESTUSERPassword123!";
        String username = "testuser";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePasswordWithUsername(password, username);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Password should not contain the username"));
    }
    
    @Test
    void validatePasswordWithUsername_WithNullUsername_ShouldValidatePasswordOnly() {
        // Given
        String password = "S3cure!Pass";
        String username = null;
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePasswordWithUsername(password, username);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void validatePasswordWithUsername_WithEmptyUsername_ShouldValidatePasswordOnly() {
        // Given
        String password = "S3cure!Pass";
        String username = "";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePasswordWithUsername(password, username);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    // Multiple Errors Test
    
    @Test
    void validatePassword_WithMultipleViolations_ShouldReturnAllErrors() {
        // Given
        String badPassword = "abc"; // Too short, no uppercase, no digit, no special char, sequential letters
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(badPassword);
        
        // Then
        assertFalse(result.isValid());
        assertEquals(5, result.getErrors().size());
        assertTrue(result.getErrors().contains("Password must be at least 8 characters long"));
        assertTrue(result.getErrors().contains("Password must contain at least one uppercase letter"));
        assertTrue(result.getErrors().contains("Password must contain at least one digit"));
        assertTrue(result.getErrors().contains("Password must contain at least one special character (!@#$%^&*()_+-=[]{};':,.<>?)"));
        assertTrue(result.getErrors().contains("Password should not contain sequential characters"));
    }
    
    // Error Message Test
    
    @Test
    void getErrorMessage_WithMultipleErrors_ShouldReturnJoinedMessage() {
        // Given
        String badPassword = "abc";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(badPassword);
        String errorMessage = result.getErrorMessage();
        
        // Then
        assertFalse(errorMessage.isEmpty());
        assertTrue(errorMessage.contains(";"));
        assertTrue(errorMessage.contains("Password must be at least 8 characters long"));
    }
    
    @Test
    void getErrorMessage_WithNoErrors_ShouldReturnEmptyString() {
        // Given
        String validPassword = "S3cure!Pass";
        
        // When
        PasswordValidationService.PasswordValidationResult result = 
                passwordValidationService.validatePassword(validPassword);
        String errorMessage = result.getErrorMessage();
        
        // Then
        assertEquals("", errorMessage);
    }
    
    // PasswordValidationResult Tests
    
    @Test
    void passwordValidationResult_SettersAndGetters_ShouldWorkCorrectly() {
        // Given
        PasswordValidationService.PasswordValidationResult result = 
                new PasswordValidationService.PasswordValidationResult(true, new java.util.ArrayList<>());
        
        // When
        result.setValid(false);
        result.getErrors().add("Test error");
        
        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Test error", result.getErrors().get(0));
        assertEquals("Test error", result.getErrorMessage());
    }
}