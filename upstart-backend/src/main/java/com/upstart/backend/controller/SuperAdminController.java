package com.upstart.backend.controller;

import com.upstart.backend.entity.SuperAdmin;
import com.upstart.backend.service.SuperAdminService;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admins")
@Tag(name = "Super Admin Management", description = "APIs for managing super admin entities")
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new super admin", description = "Creates a new super admin with the provided details")
    public ResponseEntity<SuperAdmin> createSuperAdmin(@RequestBody SuperAdmin superAdmin) {
        SuperAdmin createdSuperAdmin = superAdminService.createSuperAdmin(superAdmin);
        return ResponseEntity.ok(createdSuperAdmin);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get super admin by ID", description = "Retrieves a super admin by their unique identifier")
    public ResponseEntity<SuperAdmin> getSuperAdminById(@PathVariable Long id) {
        SuperAdmin superAdmin = superAdminService.getSuperAdminById(id)
                .orElseThrow(() -> new RuntimeException("Super admin not found with ID: " + id));
        return ResponseEntity.ok(superAdmin);
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get super admin by username", description = "Retrieves a super admin by their username")
    public ResponseEntity<SuperAdmin> getSuperAdminByUsername(@PathVariable String username) {
        SuperAdmin superAdmin = superAdminService.getSuperAdminByUsername(username)
                .orElseThrow(() -> new RuntimeException("Super admin not found with username: " + username));
        return ResponseEntity.ok(superAdmin);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get super admin by email", description = "Retrieves a super admin by their email")
    public ResponseEntity<SuperAdmin> getSuperAdminByEmail(@PathVariable String email) {
        SuperAdmin superAdmin = superAdminService.getSuperAdminByEmail(email)
                .orElseThrow(() -> new RuntimeException("Super admin not found with email: " + email));
        return ResponseEntity.ok(superAdmin);
    }

    @GetMapping("/find/{usernameOrEmail}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Find super admin by username or email", description = "Retrieves a super admin by their username or email")
    public ResponseEntity<SuperAdmin> getSuperAdminByUsernameOrEmail(@PathVariable String usernameOrEmail) {
        SuperAdmin superAdmin = superAdminService.getSuperAdminByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new RuntimeException("Super admin not found with username or email: " + usernameOrEmail));
        return ResponseEntity.ok(superAdmin);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search super admins with dynamic criteria", description = "Search super admins using multiple optional criteria with pagination and sorting")
    public ResponseEntity<Page<SuperAdmin>> searchSuperAdmins(
            @Parameter(description = "Username filter (partial match)") @RequestParam(required = false) String username,
            @Parameter(description = "Email filter (partial match)") @RequestParam(required = false) String email,
            @Parameter(description = "First name filter (partial match)") @RequestParam(required = false) String firstName,
            @Parameter(description = "Last name filter (partial match)") @RequestParam(required = false) String lastName,
            @Parameter(description = "Phone number filter (partial match)") @RequestParam(required = false) String phoneNumber,
            @Parameter(description = "Enabled status filter") @RequestParam(required = false) Boolean isEnabled,
            @Parameter(description = "Minimum security level") @RequestParam(required = false) Integer minSecurityLevel,
            @Parameter(description = "Maximum security level") @RequestParam(required = false) Integer maxSecurityLevel,
            @Parameter(description = "System permissions filter (partial match)") @RequestParam(required = false) String systemPermissions,
            @Parameter(description = "Last access after date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastAccessAfter,
            @Parameter(description = "Last access before date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastAccessBefore,
            @Parameter(description = "Created after date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @Parameter(description = "Created before date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SuperAdmin> superAdmins = superAdminService.searchSuperAdmins(
                username, email, firstName, lastName, phoneNumber, isEnabled,
                minSecurityLevel, maxSecurityLevel, systemPermissions,
                lastAccessAfter, lastAccessBefore, createdAfter, createdBefore, pageable);
        return ResponseEntity.ok(superAdmins);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all super admins", description = "Retrieves all super admins with pagination")
    public ResponseEntity<Page<SuperAdmin>> getAllSuperAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SuperAdmin> superAdmins = superAdminService.getAllSuperAdmins(pageable);
        return ResponseEntity.ok(superAdmins);
    }

    @GetMapping("/active-since")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get active admins since date", description = "Retrieves super admins who have been active since the specified date")
    public ResponseEntity<List<SuperAdmin>> getActiveAdminsSince(
            @Parameter(description = "Date to check activity since") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        List<SuperAdmin> activeAdmins = superAdminService.getActiveAdminsSince(since);
        return ResponseEntity.ok(activeAdmins);
    }

    @GetMapping("/security-level/{minLevel}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get admins by minimum security level", description = "Retrieves super admins with at least the specified security level")
    public ResponseEntity<List<SuperAdmin>> getAdminsByMinimumSecurityLevel(@PathVariable Integer minLevel) {
        List<SuperAdmin> admins = superAdminService.getAdminsByMinimumSecurityLevel(minLevel);
        return ResponseEntity.ok(admins);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update super admin", description = "Updates an existing super admin's details")
    public ResponseEntity<SuperAdmin> updateSuperAdmin(@PathVariable Long id, @RequestBody SuperAdmin superAdminDetails) {
        SuperAdmin updatedSuperAdmin = superAdminService.updateSuperAdmin(id, superAdminDetails);
        return ResponseEntity.ok(updatedSuperAdmin);
    }

    @PatchMapping("/{id}/last-access")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update last system access", description = "Updates a super admin's last system access timestamp to current time")
    public ResponseEntity<SuperAdmin> updateLastSystemAccess(@PathVariable Long id) {
        SuperAdmin updatedSuperAdmin = superAdminService.updateLastSystemAccess(id);
        return ResponseEntity.ok(updatedSuperAdmin);
    }

    @PatchMapping("/{id}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update system permissions", description = "Updates a super admin's system permissions")
    public ResponseEntity<SuperAdmin> updateSystemPermissions(@PathVariable Long id, @RequestParam String systemPermissions) {
        SuperAdmin updatedSuperAdmin = superAdminService.updateSystemPermissions(id, systemPermissions);
        return ResponseEntity.ok(updatedSuperAdmin);
    }

    @PatchMapping("/{id}/security-level")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update security level", description = "Updates a super admin's security level")
    public ResponseEntity<SuperAdmin> updateSecurityLevel(@PathVariable Long id, @RequestParam Integer securityLevel) {
        SuperAdmin updatedSuperAdmin = superAdminService.updateSecurityLevel(id, securityLevel);
        return ResponseEntity.ok(updatedSuperAdmin);
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Disable super admin", description = "Soft deletes a super admin (disables the account)")
    public ResponseEntity<Void> disableSuperAdmin(@PathVariable Long id) {
        superAdminService.disableSuperAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete super admin", description = "Permanently deletes a super admin from the database")
    public ResponseEntity<Void> deleteSuperAdmin(@PathVariable Long id) {
        superAdminService.deleteSuperAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get super admin statistics", description = "Retrieves various statistics about super admins")
    public ResponseEntity<Map<String, Object>> getSuperAdminStatistics() {
        Map<String, Object> statistics = superAdminService.getSuperAdminStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/count/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count active super admins", description = "Returns the count of active super admins")
    public ResponseEntity<Long> countActiveSuperAdmins() {
        Long count = superAdminService.countActiveSuperAdmins();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/total")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count total super admins", description = "Returns the total count of super admins")
    public ResponseEntity<Long> countTotalSuperAdmins() {
        Long count = superAdminService.countTotalSuperAdmins();
        return ResponseEntity.ok(count);
    }
}