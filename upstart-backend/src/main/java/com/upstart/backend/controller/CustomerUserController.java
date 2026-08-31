package com.upstart.backend.controller;

import com.upstart.backend.entity.CustomerUser;
import com.upstart.backend.service.CustomerUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/customer-users")
@Tag(name = "Customer User Management", description = "APIs for managing customer user entities")
public class CustomerUserController {

    @Autowired
    private CustomerUserService customerUserService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new customer user", description = "Creates a new customer user with the provided details")
    public ResponseEntity<CustomerUser> createCustomerUser(@RequestBody CustomerUser customerUser) {
        CustomerUser createdCustomerUser = customerUserService.createCustomerUser(customerUser);
        return ResponseEntity.ok(createdCustomerUser);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CLIENT')")
    @Operation(summary = "Get customer user by ID", description = "Retrieves a customer user by their unique identifier")
    public ResponseEntity<CustomerUser> getCustomerUserById(@PathVariable Long id) {
        CustomerUser customerUser = customerUserService.getCustomerUserById(id)
                .orElseThrow(() -> new RuntimeException("Customer user not found with ID: " + id));
        return ResponseEntity.ok(customerUser);
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CLIENT')")
    @Operation(summary = "Get customer user by username", description = "Retrieves a customer user by their username")
    public ResponseEntity<CustomerUser> getCustomerUserByUsername(@PathVariable String username) {
        CustomerUser customerUser = customerUserService.getCustomerUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer user not found with username: " + username));
        return ResponseEntity.ok(customerUser);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search customer users with dynamic criteria", description = "Search customer users using multiple optional criteria with pagination and sorting")
    public ResponseEntity<Page<CustomerUser>> searchCustomerUsers(
            @Parameter(description = "Username filter (partial match)") @RequestParam(required = false) String username,
            @Parameter(description = "Email filter (partial match)") @RequestParam(required = false) String email,
            @Parameter(description = "First name filter (partial match)") @RequestParam(required = false) String firstName,
            @Parameter(description = "Last name filter (partial match)") @RequestParam(required = false) String lastName,
            @Parameter(description = "Phone number filter (partial match)") @RequestParam(required = false) String phoneNumber,
            @Parameter(description = "Premium status filter") @RequestParam(required = false) Boolean isPremium,
            @Parameter(description = "Enabled status filter") @RequestParam(required = false) Boolean isEnabled,
            @Parameter(description = "Minimum loyalty points") @RequestParam(required = false) Integer minLoyaltyPoints,
            @Parameter(description = "Maximum loyalty points") @RequestParam(required = false) Integer maxLoyaltyPoints,
            @Parameter(description = "Minimum total spent") @RequestParam(required = false) BigDecimal minTotalSpent,
            @Parameter(description = "Maximum total spent") @RequestParam(required = false) BigDecimal maxTotalSpent,
            @Parameter(description = "Minimum total orders") @RequestParam(required = false) Integer minTotalOrders,
            @Parameter(description = "Maximum total orders") @RequestParam(required = false) Integer maxTotalOrders,
            @Parameter(description = "Gender filter") @RequestParam(required = false) String gender,
            @Parameter(description = "Preferred payment method") @RequestParam(required = false) String preferredPaymentMethod,
            @Parameter(description = "Default address filter (partial match)") @RequestParam(required = false) String defaultAddress,
            @Parameter(description = "Created after date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @Parameter(description = "Created before date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @Parameter(description = "Born after date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bornAfter,
            @Parameter(description = "Born before date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bornBefore,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        // Parameters are passed directly to the service method

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CustomerUser> customerUsers = customerUserService.searchCustomerUsers(
                username, email, firstName, lastName, phoneNumber, isPremium, isEnabled,
                minLoyaltyPoints, maxLoyaltyPoints, minTotalSpent, maxTotalSpent,
                minTotalOrders, maxTotalOrders, gender, preferredPaymentMethod, defaultAddress,
                createdAfter, createdBefore, bornAfter, bornBefore, pageable);
        return ResponseEntity.ok(customerUsers);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all customer users", description = "Retrieves all customer users with pagination")
    public ResponseEntity<Page<CustomerUser>> getAllCustomerUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CustomerUser> customerUsers = customerUserService.getAllCustomerUsers(pageable);
        return ResponseEntity.ok(customerUsers);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CLIENT')")
    @Operation(summary = "Update customer user", description = "Updates an existing customer user's details")
    public ResponseEntity<CustomerUser> updateCustomerUser(@PathVariable Long id, @RequestBody CustomerUser customerUserDetails) {
        CustomerUser updatedCustomerUser = customerUserService.updateCustomerUser(id, customerUserDetails);
        return ResponseEntity.ok(updatedCustomerUser);
    }

    @PatchMapping("/{id}/loyalty-points")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update loyalty points", description = "Updates a customer user's loyalty points")
    public ResponseEntity<CustomerUser> updateLoyaltyPoints(@PathVariable Long id, @RequestParam Integer loyaltyPoints) {
        CustomerUser updatedCustomerUser = customerUserService.updateLoyaltyPoints(id, loyaltyPoints);
        return ResponseEntity.ok(updatedCustomerUser);
    }

    @PatchMapping("/{id}/premium-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update premium status", description = "Updates a customer user's premium status")
    public ResponseEntity<CustomerUser> updatePremiumStatus(@PathVariable Long id, @RequestParam Boolean isPremium) {
        CustomerUser updatedCustomerUser = customerUserService.updatePremiumStatus(id, isPremium);
        return ResponseEntity.ok(updatedCustomerUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Soft delete customer user", description = "Soft deletes a customer user (disables the account)")
    public ResponseEntity<Void> deleteCustomerUser(@PathVariable Long id) {
        customerUserService.deleteCustomerUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Hard delete customer user", description = "Permanently deletes a customer user from the database")
    public ResponseEntity<Void> hardDeleteCustomerUser(@PathVariable Long id) {
        customerUserService.hardDeleteCustomerUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get customer user statistics", description = "Retrieves various statistics about customer users")
    public ResponseEntity<Map<String, Object>> getCustomerUserStatistics() {
        Map<String, Object> statistics = customerUserService.getCustomerUserStatistics();
        return ResponseEntity.ok(statistics);
    }
}