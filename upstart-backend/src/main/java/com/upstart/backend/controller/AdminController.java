package com.upstart.backend.controller;

import com.upstart.backend.entity.Admin;
import com.upstart.backend.service.AdminService;

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
@RequestMapping("/api/admins")
@Tag(name = "Admin Management", description = "APIs for managing admins")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping
    @Operation(summary = "Create a new admin", description = "Creates a new admin with the provided details")
    public ResponseEntity<Admin> createAdmin(@RequestBody Admin admin) {
        return ResponseEntity.ok(adminService.createAdmin(admin));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get admin by ID", description = "Retrieves an admin by their unique identifier")
    public ResponseEntity<Admin> getAdminById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAdminById(id));
    }

    @GetMapping
    @Operation(summary = "Get all admins", description = "Retrieves all admins with pagination")
    public ResponseEntity<Page<Admin>> getAllAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(adminService.getAllAdmins(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search admins with dynamic criteria", description = "Search admins using multiple optional criteria with pagination and sorting")
    public ResponseEntity<Page<Admin>> searchAdmins(
            @Parameter(description = "Username filter (partial match)") @RequestParam(required = false) String username,
            @Parameter(description = "Email filter (partial match)") @RequestParam(required = false) String email,
            @Parameter(description = "First name filter (partial match)") @RequestParam(required = false) String firstName,
            @Parameter(description = "Last name filter (partial match)") @RequestParam(required = false) String lastName,
            @Parameter(description = "Phone number filter (partial match)") @RequestParam(required = false) String phoneNumber,
            @Parameter(description = "Role filter") @RequestParam(required = false) Admin.AdminRole role,
            @Parameter(description = "Enabled status filter") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Verified status filter") @RequestParam(required = false) Boolean verified,
            @Parameter(description = "Department filter") @RequestParam(required = false) String department,
            @Parameter(description = "Position filter") @RequestParam(required = false) String position,
            @Parameter(description = "National ID filter") @RequestParam(required = false) String nationalId,
            @Parameter(description = "Created after date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @Parameter(description = "Created before date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @Parameter(description = "Hired after date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hiredAfter,
            @Parameter(description = "Hired before date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hiredBefore,
            @Parameter(description = "Born after date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bornAfter,
            @Parameter(description = "Born before date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bornBefore,
            @Parameter(description = "Address filter (partial match)") @RequestParam(required = false) String address,
            @Parameter(description = "Emergency contact filter") @RequestParam(required = false) String emergencyContact,
            @Parameter(description = "Minimum salary") @RequestParam(required = false) BigDecimal minSalary,
            @Parameter(description = "Maximum salary") @RequestParam(required = false) BigDecimal maxSalary,
            @Parameter(description = "Active status filter") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<Admin> admins = adminService.searchAdmins(
                username, email, firstName, lastName, phoneNumber, role, enabled, verified,
                department, position, nationalId,
                createdAfter, createdBefore, hiredAfter, hiredBefore,
                bornAfter, bornBefore, address, emergencyContact,
                minSalary, maxSalary, isActive,
                pageable);
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/search/name")
    @Operation(summary = "Search admins by name", description = "Searches admins whose first or last name matches the search term")
    public ResponseEntity<Page<Admin>> searchByName(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(adminService.searchByName(searchTerm, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/search/contact")
    @Operation(summary = "Search admins by contact", description = "Searches admins whose phone or email matches the search term")
    public ResponseEntity<Page<Admin>> searchByContact(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(adminService.searchByContact(searchTerm, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/active-verified")
    @Operation(summary = "Get active and verified admins", description = "Retrieves admins that are both active and verified")
    public ResponseEntity<Page<Admin>> getActiveAndVerifiedAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(adminService.getActiveAndVerifiedAdmins(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/super-admins")
    @Operation(summary = "Get super admins", description = "Retrieves all admins with the SUPER_ADMIN role")
    public ResponseEntity<Page<Admin>> getSuperAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(adminService.getSuperAdmins(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get admins by role", description = "Retrieves admins with the given role")
    public ResponseEntity<Page<Admin>> getAdminsByRole(
            @PathVariable Admin.AdminRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(adminService.getAdminsByRole(role, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/department/{department}")
    @Operation(summary = "Get admins by department", description = "Retrieves admins in the given department")
    public ResponseEntity<Page<Admin>> getAdminsByDepartment(
            @PathVariable String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(adminService.getAdminsByDepartment(department, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/recently-hired")
    @Operation(summary = "Get recently hired admins", description = "Retrieves admins hired after the given date (defaults to last 90 days)")
    public ResponseEntity<Page<Admin>> getRecentlyHiredAdmins(
            @Parameter(description = "Hired since date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(adminService.getRecentlyHiredAdmins(since, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get admin by username", description = "Retrieves an admin by their username")
    public ResponseEntity<Admin> getAdminByUsername(@PathVariable String username) {
        return ResponseEntity.ok(adminService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin not found with username: " + username)));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get admin by email", description = "Retrieves an admin by their email")
    public ResponseEntity<Admin> getAdminByEmail(@PathVariable String email) {
        return ResponseEntity.ok(adminService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found with email: " + email)));
    }

    @GetMapping("/national-id/{nationalId}")
    @Operation(summary = "Get admin by national ID", description = "Retrieves an admin by their national ID")
    public ResponseEntity<Admin> getAdminByNationalId(@PathVariable String nationalId) {
        return ResponseEntity.ok(adminService.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("Admin not found with national ID: " + nationalId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update admin", description = "Updates an existing admin's details")
    public ResponseEntity<Admin> updateAdmin(@PathVariable Long id, @RequestBody Admin admin) {
        return ResponseEntity.ok(adminService.updateAdmin(id, admin));
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Update enabled status", description = "Updates whether an admin account is enabled")
    public ResponseEntity<Admin> updateEnabledStatus(@PathVariable Long id, @RequestParam Boolean enabled) {
        return ResponseEntity.ok(adminService.updateEnabledStatus(id, enabled));
    }

    @PatchMapping("/{id}/verified")
    @Operation(summary = "Update verified status", description = "Updates whether an admin is verified")
    public ResponseEntity<Admin> updateVerifiedStatus(@PathVariable Long id, @RequestParam Boolean verified) {
        return ResponseEntity.ok(adminService.updateVerifiedStatus(id, verified));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update admin role", description = "Updates an admin's role")
    public ResponseEntity<Admin> updateRole(@PathVariable Long id, @RequestParam Admin.AdminRole role) {
        return ResponseEntity.ok(adminService.updateRole(id, role));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Update active status", description = "Updates whether an admin is active")
    public ResponseEntity<Admin> updateActiveStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        return ResponseEntity.ok(adminService.updateActiveStatus(id, isActive));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Update admin password", description = "Updates an admin's password")
    public ResponseEntity<Map<String, String>> updatePassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String password = body != null ? body.get("password") : null;
        if (password == null || password.isBlank()) {
            throw new RuntimeException("Password is required");
        }
        Admin admin = adminService.getAdminById(id);
        admin.setPassword(password);
        adminService.updateAdmin(id, admin);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PatchMapping("/{id}/lock")
    @Operation(summary = "Lock admin", description = "Locks an admin account with an optional reason")
    public ResponseEntity<Admin> lockAdmin(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(adminService.lockAdmin(id, reason));
    }

    @PatchMapping("/{id}/unlock")
    @Operation(summary = "Unlock admin", description = "Unlocks an admin account and resets failed login attempts")
    public ResponseEntity<Admin> unlockAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unlockAdmin(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete admin", description = "Permanently deletes an admin from the database")
    public ResponseEntity<Map<String, String>> deleteAdmin(@PathVariable Long id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok(Map.of("message", "Admin deleted successfully"));
    }

    @DeleteMapping("/{id}/soft")
    @Operation(summary = "Soft delete admin", description = "Soft deletes an admin by marking them as inactive and disabled")
    public ResponseEntity<Map<String, String>> softDeleteAdmin(@PathVariable Long id) {
        adminService.softDeleteAdmin(id);
        return ResponseEntity.ok(Map.of("message", "Admin soft-deleted successfully"));
    }

    @GetMapping("/stats/count")
    @Operation(summary = "Get admin statistics", description = "Retrieves overall statistics about admins")
    public ResponseEntity<Map<String, Object>> getAdminStatistics() {
        return ResponseEntity.ok(adminService.getAdminStatistics());
    }

    @GetMapping("/stats/count/total")
    @Operation(summary = "Count total admins", description = "Returns the total count of admins")
    public ResponseEntity<Long> countTotalAdmins() {
        return ResponseEntity.ok(adminService.countAllAdmins());
    }

    @GetMapping("/stats/count/active")
    @Operation(summary = "Count active admins", description = "Returns the count of active admins")
    public ResponseEntity<Long> countActiveAdmins() {
        return ResponseEntity.ok(adminService.countActiveAdmins());
    }

    @GetMapping("/stats/count/verified")
    @Operation(summary = "Count verified admins", description = "Returns the count of verified admins")
    public ResponseEntity<Long> countVerifiedAdmins() {
        return ResponseEntity.ok(adminService.countVerifiedAdmins());
    }

    @GetMapping("/stats/count/active-verified")
    @Operation(summary = "Count active and verified admins", description = "Returns the count of admins that are both active and verified")
    public ResponseEntity<Long> countActiveAndVerifiedAdmins() {
        return ResponseEntity.ok(adminService.countActiveAndVerifiedAdmins());
    }

    @GetMapping("/stats/count/super-admins")
    @Operation(summary = "Count super admins", description = "Returns the count of super admins")
    public ResponseEntity<Long> countSuperAdmins() {
        return ResponseEntity.ok(adminService.countSuperAdmins());
    }

    @GetMapping("/stats/count/admins")
    @Operation(summary = "Count admins", description = "Returns the count of admins with the ADMIN role")
    public ResponseEntity<Long> countAdmins() {
        return ResponseEntity.ok(adminService.countAdmins());
    }

    @GetMapping("/stats/count/moderators")
    @Operation(summary = "Count moderators", description = "Returns the count of moderators")
    public ResponseEntity<Long> countModerators() {
        return ResponseEntity.ok(adminService.countModerators());
    }

    @GetMapping("/stats/count/recently-hired")
    @Operation(summary = "Count recently hired admins", description = "Returns the count of admins hired within the last 90 days")
    public ResponseEntity<Long> countRecentlyHiredAdmins() {
        return ResponseEntity.ok(adminService.countRecentlyHiredAdmins());
    }

    @GetMapping("/exists/username/{username}")
    @Operation(summary = "Check username existence", description = "Checks whether an admin with the given username already exists")
    public ResponseEntity<Boolean> existsByUsername(@PathVariable String username) {
        return ResponseEntity.ok(adminService.existsByUsername(username));
    }

    @GetMapping("/exists/email/{email}")
    @Operation(summary = "Check email existence", description = "Checks whether an admin with the given email already exists")
    public ResponseEntity<Boolean> existsByEmail(@PathVariable String email) {
        return ResponseEntity.ok(adminService.existsByEmail(email));
    }

    @GetMapping("/exists/national-id/{nationalId}")
    @Operation(summary = "Check national ID existence", description = "Checks whether an admin with the given national ID already exists")
    public ResponseEntity<Boolean> existsByNationalId(@PathVariable String nationalId) {
        return ResponseEntity.ok(adminService.existsByNationalId(nationalId));
    }

    @GetMapping("/exists/username/{username}/exclude/{id}")
    @Operation(summary = "Check username existence excluding admin", description = "Checks whether another admin with the given username already exists")
    public ResponseEntity<Boolean> existsByUsernameAndIdNot(@PathVariable String username, @PathVariable Long id) {
        return ResponseEntity.ok(adminService.existsByUsernameAndIdNot(username, id));
    }

    @GetMapping("/exists/email/{email}/exclude/{id}")
    @Operation(summary = "Check email existence excluding admin", description = "Checks whether another admin with the given email already exists")
    public ResponseEntity<Boolean> existsByEmailAndIdNot(@PathVariable String email, @PathVariable Long id) {
        return ResponseEntity.ok(adminService.existsByEmailAndIdNot(email, id));
    }

    @GetMapping("/exists/national-id/{nationalId}/exclude/{id}")
    @Operation(summary = "Check national ID existence excluding admin", description = "Checks whether another admin with the given national ID already exists")
    public ResponseEntity<Boolean> existsByNationalIdAndIdNot(@PathVariable String nationalId, @PathVariable Long id) {
        return ResponseEntity.ok(adminService.existsByNationalIdAndIdNot(nationalId, id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
}