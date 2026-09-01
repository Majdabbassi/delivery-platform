package com.upstart.backend.controller;

import com.upstart.backend.entity.DeliveryOwner;
import com.upstart.backend.service.DeliveryOwnerService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/delivery-owners")
public class DeliveryOwnerController {

    @Autowired
    private DeliveryOwnerService deliveryOwnerService;

    // Create operations
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DeliveryOwner> createDeliveryOwner(@Valid @RequestBody DeliveryOwner deliveryOwner) {
        DeliveryOwner createdDeliveryOwner = deliveryOwnerService.createDeliveryOwner(deliveryOwner);
        return new ResponseEntity<>(createdDeliveryOwner, HttpStatus.CREATED);
    }

    // Read operations
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<DeliveryOwner> getDeliveryOwnerById(@PathVariable Long id) {
        DeliveryOwner deliveryOwner = deliveryOwnerService.getDeliveryOwnerById(id);
        return ResponseEntity.ok(deliveryOwner);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<DeliveryOwner>> getAllDeliveryOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DeliveryOwner> deliveryOwners = deliveryOwnerService.getAllDeliveryOwners(pageable);
        return ResponseEntity.ok(deliveryOwners);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<DeliveryOwner>> searchDeliveryOwners(
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
        
        Page<DeliveryOwner> deliveryOwners = deliveryOwnerService.searchDeliveryOwners(
                username, email, firstName, lastName, phoneNumber, nationalId,
                enabled, verified, preferredBusinessCategory, minBusinessExperience,
                createdAfter, createdBefore, bornAfter, bornBefore,
                address, emergencyContact, pageable);
        
        return ResponseEntity.ok(deliveryOwners);
    }

    // Quick search endpoints
    @GetMapping("/search/name")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<DeliveryOwner>> searchByName(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be null or empty");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryOwner> deliveryOwners = deliveryOwnerService.searchByName(searchTerm.trim(), pageable);
        return ResponseEntity.ok(deliveryOwners);
    }

    @GetMapping("/search/contact")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<DeliveryOwner>> searchByContact(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be null or empty");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryOwner> deliveryOwners = deliveryOwnerService.searchByContact(searchTerm.trim(), pageable);
        return ResponseEntity.ok(deliveryOwners);
    }

    @GetMapping("/active-verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<DeliveryOwner>> getActiveAndVerifiedDeliveryOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryOwner> deliveryOwners = deliveryOwnerService.getActiveAndVerifiedDeliveryOwners(pageable);
        return ResponseEntity.ok(deliveryOwners);
    }

    @GetMapping("/experienced")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<DeliveryOwner>> getExperiencedDeliveryOwners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryOwner> deliveryOwners = deliveryOwnerService.getExperiencedDeliveryOwners(pageable);
        return ResponseEntity.ok(deliveryOwners);
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<DeliveryOwner>> getNewDeliveryOwners(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryOwner> deliveryOwners = deliveryOwnerService.getNewDeliveryOwners(since, pageable);
        return ResponseEntity.ok(deliveryOwners);
    }

    // Lookup endpoints
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<DeliveryOwner> getByUsername(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        Optional<DeliveryOwner> deliveryOwner = deliveryOwnerService.findByUsername(username.trim());
        return deliveryOwner.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<DeliveryOwner> getByEmail(@PathVariable String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        Optional<DeliveryOwner> deliveryOwner = deliveryOwnerService.findByEmail(email.trim());
        return deliveryOwner.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/national-id/{nationalId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DeliveryOwner> getByNationalId(@PathVariable String nationalId) {
        if (nationalId == null || nationalId.trim().isEmpty()) {
            throw new IllegalArgumentException("National ID cannot be null or empty");
        }
        Optional<DeliveryOwner> deliveryOwner = deliveryOwnerService.findByNationalId(nationalId.trim());
        return deliveryOwner.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    // Update operations
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<DeliveryOwner> updateDeliveryOwner(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryOwner deliveryOwner) {
        DeliveryOwner updatedDeliveryOwner = deliveryOwnerService.updateDeliveryOwner(id, deliveryOwner);
        return ResponseEntity.ok(updatedDeliveryOwner);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DeliveryOwner> updateEnabledStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        DeliveryOwner updatedDeliveryOwner = deliveryOwnerService.updateEnabledStatus(id, enabled);
        return ResponseEntity.ok(updatedDeliveryOwner);
    }

    @PatchMapping("/{id}/verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DeliveryOwner> updateVerifiedStatus(
            @PathVariable Long id,
            @RequestParam boolean verified) {
        DeliveryOwner updatedDeliveryOwner = deliveryOwnerService.updateVerifiedStatus(id, verified);
        return ResponseEntity.ok(updatedDeliveryOwner);
    }

    // Delete operations
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDeliveryOwner(@PathVariable Long id) {
        deliveryOwnerService.deleteDeliveryOwner(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "DeliveryOwner deleted successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/soft")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> softDeleteDeliveryOwner(@PathVariable Long id) {
        deliveryOwnerService.softDeleteDeliveryOwner(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "DeliveryOwner soft deleted successfully");
        return ResponseEntity.ok(response);
    }

    // Statistical endpoints
    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDeliveryOwnerCounts() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", deliveryOwnerService.countAllDeliveryOwners());
        stats.put("active", deliveryOwnerService.countActiveDeliveryOwners());
        stats.put("verified", deliveryOwnerService.countVerifiedDeliveryOwners());
        stats.put("activeAndVerified", deliveryOwnerService.countActiveAndVerifiedDeliveryOwners());
        stats.put("experienced", deliveryOwnerService.countExperiencedDeliveryOwners());
        stats.put("withMultipleCompanies", deliveryOwnerService.countDeliveryOwnersWithMultipleCompanies());
        stats.put("highRated", deliveryOwnerService.countHighRatedDeliveryOwners());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/count/total")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getTotalDeliveryOwnersCount() {
        return ResponseEntity.ok(deliveryOwnerService.countAllDeliveryOwners());
    }

    @GetMapping("/stats/count/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getActiveDeliveryOwnersCount() {
        return ResponseEntity.ok(deliveryOwnerService.countActiveDeliveryOwners());
    }

    @GetMapping("/stats/count/verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getVerifiedDeliveryOwnersCount() {
        return ResponseEntity.ok(deliveryOwnerService.countVerifiedDeliveryOwners());
    }

    @GetMapping("/stats/count/active-verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getActiveAndVerifiedDeliveryOwnersCount() {
        return ResponseEntity.ok(deliveryOwnerService.countActiveAndVerifiedDeliveryOwners());
    }

    @GetMapping("/stats/count/experienced")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getExperiencedDeliveryOwnersCount() {
        return ResponseEntity.ok(deliveryOwnerService.countExperiencedDeliveryOwners());
    }

    @GetMapping("/stats/count/multiple-companies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getDeliveryOwnersWithMultipleCompaniesCount() {
        return ResponseEntity.ok(deliveryOwnerService.countDeliveryOwnersWithMultipleCompanies());
    }

    // Validation endpoints
    @GetMapping("/exists/username/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Boolean> existsByUsername(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return ResponseEntity.ok(deliveryOwnerService.existsByUsername(username.trim()));
    }

    @GetMapping("/exists/email/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Boolean> existsByEmail(@PathVariable String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        return ResponseEntity.ok(deliveryOwnerService.existsByEmail(email.trim()));
    }

    @GetMapping("/exists/national-id/{nationalId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Boolean> existsByNationalId(@PathVariable String nationalId) {
        if (nationalId == null || nationalId.trim().isEmpty()) {
            throw new IllegalArgumentException("National ID cannot be null or empty");
        }
        return ResponseEntity.ok(deliveryOwnerService.existsByNationalId(nationalId.trim()));
    }

    @GetMapping("/exists/username/{username}/exclude/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Boolean> existsByUsernameAndIdNot(
            @PathVariable String username,
            @PathVariable Long id) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return ResponseEntity.ok(deliveryOwnerService.existsByUsernameAndIdNot(username.trim(), id));
    }

    @GetMapping("/exists/email/{email}/exclude/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Boolean> existsByEmailAndIdNot(
            @PathVariable String email,
            @PathVariable Long id) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        return ResponseEntity.ok(deliveryOwnerService.existsByEmailAndIdNot(email.trim(), id));
    }

    @GetMapping("/exists/national-id/{nationalId}/exclude/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Boolean> existsByNationalIdAndIdNot(
            @PathVariable String nationalId,
            @PathVariable Long id) {
        if (nationalId == null || nationalId.trim().isEmpty()) {
            throw new IllegalArgumentException("National ID cannot be null or empty");
        }
        return ResponseEntity.ok(deliveryOwnerService.existsByNationalIdAndIdNot(nationalId.trim(), id));
    }
}