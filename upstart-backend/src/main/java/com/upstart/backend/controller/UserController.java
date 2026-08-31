package com.upstart.backend.controller;

import com.upstart.backend.dto.RegisterDto;
import com.upstart.backend.dto.UserResponseDto;
import com.upstart.backend.entity.CustomerUser;
import com.upstart.backend.entity.User;
import com.upstart.backend.service.CustomerUserService;
import com.upstart.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final CustomerUserService customerUserService;
    
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterDto registerDto) {
        log.info("Registering new user with username: {}", registerDto.getUsername());
        
        CustomerUser customerUser = new CustomerUser(
            registerDto.getUsername(),
            registerDto.getEmail(),
            registerDto.getPassword(),
            registerDto.getFirstName(),
            registerDto.getLastName()
        );
        customerUser.setPhoneNumber(registerDto.getPhoneNumber());
        
        CustomerUser saved = customerUserService.createCustomerUser(customerUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDto.fromUser(saved));
    }

    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or #username == authentication.principal.username")
    public ResponseEntity<UserResponseDto> getUserByUsername(@PathVariable String username) {
        return userService.findByUsernameDto(username)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getActiveUsers() {
        List<UserResponseDto> users = userService.getActiveUsers();
        return ResponseEntity.ok(users);
    }
    
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponseDto> toggleUserStatus(@PathVariable Long id) {
        try {
            UserResponseDto updatedUser = userService.toggleUserStatus(id);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserResponseDto>> getUsersByRole(@PathVariable User.Role role) {
        List<UserResponseDto> users = userService.findUsersByRole(role);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) Boolean enabled,
            Pageable pageable) {
        Page<UserResponseDto> users = userService.searchUsers(searchTerm, role, enabled, pageable);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/find/{usernameOrEmail}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or #usernameOrEmail == authentication.principal.username")
    public ResponseEntity<UserResponseDto> findByUsernameOrEmail(@PathVariable String usernameOrEmail) {
        return userService.findByUsernameOrEmail(usernameOrEmail)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

}