package com.upstart.backend.controller;

import com.upstart.backend.entity.VendorOwner;
import com.upstart.backend.service.VendorOwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import com.upstart.backend.dto.VendorOwnerDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vendor-owners")
@CrossOrigin(origins = "*")
public class VendorOwnerController {

    @Autowired
    private VendorOwnerService vendorOwnerService;

    // Create operations
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VendorOwner> createVendorOwner(@Valid @RequestBody VendorOwner vendorOwner) {
        VendorOwner createdVendorOwner = vendorOwnerService.createVendorOwner(vendorOwner);
        return new ResponseEntity<>(createdVendorOwner, HttpStatus.CREATED);
    }

    // Read operations
    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<VendorOwner>> searchVendorOwners(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String nationalId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String preferredBusinessCategory,
            @RequestParam(required = false) Integer minBusinessExperience,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bornAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bornBefore,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String emergencyContact,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<VendorOwner> vendorOwners = vendorOwnerService.searchVendorOwners(
                username, email, firstName, lastName, phoneNumber, nationalId,
                enabled, verified, preferredBusinessCategory, minBusinessExperience,
                createdAfter, createdBefore, bornAfter, bornBefore,
                address, emergencyContact, pageable);
        
        return ResponseEntity.ok(vendorOwners);
    }

    // Quick search endpoints

    @GetMapping("/active-verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<VendorOwner>> getActiveAndVerifiedVendorOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorOwner> vendorOwners = vendorOwnerService.getActiveAndVerifiedVendorOwners(pageable);
        return ResponseEntity.ok(vendorOwners);
    }

    @GetMapping("/experienced")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<VendorOwner>> getExperiencedVendorOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorOwner> vendorOwners = vendorOwnerService.getExperiencedVendorOwners(pageable);
        return ResponseEntity.ok(vendorOwners);
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<VendorOwner>> getNewVendorOwners(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorOwner> vendorOwners = vendorOwnerService.getNewVendorOwners(since, pageable);
        return ResponseEntity.ok(vendorOwners);
    }

    // Lookup endpoints

    @GetMapping("/national-id/{nationalId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VendorOwner> getByNationalId(@PathVariable String nationalId) {
        if (nationalId == null || nationalId.trim().isEmpty()) {
            throw new IllegalArgumentException("National ID cannot be null or empty");
        }
        Optional<VendorOwner> vendorOwner = vendorOwnerService.findByNationalId(nationalId.trim());
        return vendorOwner.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    // Update operations
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<VendorOwner> updateVendorOwner(
            @PathVariable Long id,
            @Valid @RequestBody VendorOwnerDTO vendorOwnerDTO) {
        VendorOwner updatedVendorOwner = vendorOwnerService.updateVendorOwner(id, vendorOwnerDTO);
        return ResponseEntity.ok(updatedVendorOwner);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VendorOwner> updateEnabledStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        VendorOwner updatedVendorOwner = vendorOwnerService.updateEnabledStatus(id, enabled);
        return ResponseEntity.ok(updatedVendorOwner);
    }

    @PatchMapping("/{id}/verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VendorOwner> updateVerifiedStatus(
            @PathVariable Long id,
            @RequestParam boolean verified) {
        VendorOwner updatedVendorOwner = vendorOwnerService.updateVerifiedStatus(id, verified);
        return ResponseEntity.ok(updatedVendorOwner);
    }

    // Delete operations
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteVendorOwner(@PathVariable Long id) {
        vendorOwnerService.deleteVendorOwner(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "VendorOwner deleted successfully");
        return ResponseEntity.ok(response);
    }

    // Statistical endpoints
    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getVendorOwnerCounts() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", vendorOwnerService.countAllVendorOwners());
        stats.put("active", vendorOwnerService.countActiveVendorOwners());
        stats.put("verified", vendorOwnerService.countVerifiedVendorOwners());
        stats.put("activeAndVerified", vendorOwnerService.countActiveAndVerifiedVendorOwners());
        stats.put("experienced", vendorOwnerService.countExperiencedVendorOwners());
        stats.put("withMultipleCompanies", vendorOwnerService.countVendorOwnersWithMultipleCompanies());
        stats.put("totalCompanies", vendorOwnerService.countTotalVendorCompanies());
        stats.put("totalRevenue", vendorOwnerService.getTotalVendorCompaniesRevenue());
        stats.put("avgExperience", vendorOwnerService.getAverageVendorOwnerExperience());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/count/total")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getTotalVendorOwnersCount() {
        return ResponseEntity.ok(vendorOwnerService.countAllVendorOwners());
    }

    @GetMapping("/stats/count/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getActiveVendorOwnersCount() {
        return ResponseEntity.ok(vendorOwnerService.countActiveVendorOwners());
    }

    @GetMapping("/stats/count/verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getVerifiedVendorOwnersCount() {
        return ResponseEntity.ok(vendorOwnerService.countVerifiedVendorOwners());
    }

    @GetMapping("/stats/count/active-verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getActiveAndVerifiedVendorOwnersCount() {
        return ResponseEntity.ok(vendorOwnerService.countActiveAndVerifiedVendorOwners());
    }

    @GetMapping("/stats/count/experienced")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getExperiencedVendorOwnersCount() {
        return ResponseEntity.ok(vendorOwnerService.countExperiencedVendorOwners());
    }

    @GetMapping("/stats/count/multiple-companies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getVendorOwnersWithMultipleCompaniesCount() {
        return ResponseEntity.ok(vendorOwnerService.countVendorOwnersWithMultipleCompanies());
    }

    // Validation endpoints
    @GetMapping("/exists/username/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Boolean> existsByUsername(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return ResponseEntity.ok(vendorOwnerService.existsByUsername(username.trim()));
    }

    @GetMapping("/exists/email/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Boolean> existsByEmail(@PathVariable String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        return ResponseEntity.ok(vendorOwnerService.existsByEmail(email.trim()));
    }

    @GetMapping("/exists/national-id/{nationalId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Boolean> existsByNationalId(@PathVariable String nationalId) {
        if (nationalId == null || nationalId.trim().isEmpty()) {
            throw new IllegalArgumentException("National ID cannot be null or empty");
        }
        return ResponseEntity.ok(vendorOwnerService.existsByNationalId(nationalId.trim()));
    }

    @GetMapping("/exists/username/{username}/exclude/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Boolean> existsByUsernameAndIdNot(
            @PathVariable String username,
            @PathVariable Long id) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return ResponseEntity.ok(vendorOwnerService.existsByUsernameAndIdNot(username.trim(), id));
    }

    @GetMapping("/exists/email/{email}/exclude/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Boolean> existsByEmailAndIdNot(
            @PathVariable String email,
            @PathVariable Long id) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        return ResponseEntity.ok(vendorOwnerService.existsByEmailAndIdNot(email.trim(), id));
    }

    @GetMapping("/exists/national-id/{nationalId}/exclude/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Boolean> existsByNationalIdAndIdNot(
            @PathVariable String nationalId,
            @PathVariable Long id) {
        return ResponseEntity.ok(vendorOwnerService.existsByNationalIdAndIdNot(nationalId, id));
    }
}