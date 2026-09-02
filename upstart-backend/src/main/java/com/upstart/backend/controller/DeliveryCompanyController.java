package com.upstart.backend.controller;

import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.service.DeliveryCompanyService;
import com.upstart.backend.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/delivery-companies")
public class DeliveryCompanyController {

    @Autowired
    private DeliveryCompanyService deliveryCompanyService;

    @Autowired
    private SecurityService securityService;

    private void assertCompanyReadAccess(Long companyId) {
        if (securityService.getCurrentRole() == com.upstart.backend.entity.User.Role.SUPER_ADMIN) {
            return;
        }
        securityService.getOwnedDeliveryCompanyOrThrow(companyId);
    }

    private void assertOwnerIdMatchesCurrent(Long ownerId) {
        if (securityService.getCurrentRole() == com.upstart.backend.entity.User.Role.SUPER_ADMIN) {
            return;
        }
        if (!securityService.getCurrentDeliveryOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("You can only access your own delivery companies");
        }
    }

    // Create operations
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<DeliveryCompany> createDeliveryCompany(@Valid @RequestBody DeliveryCompany deliveryCompany) {
        DeliveryCompany createdDeliveryCompany = deliveryCompanyService.createDeliveryCompany(deliveryCompany);
        return new ResponseEntity<>(createdDeliveryCompany, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/with-owner/{ownerId}")
    public ResponseEntity<DeliveryCompany> createDeliveryCompanyWithOwner(
            @PathVariable Long ownerId,
            @Valid @RequestBody DeliveryCompany deliveryCompany) {
        DeliveryCompany createdDeliveryCompany = deliveryCompanyService.createDeliveryCompanyWithOwner(deliveryCompany, ownerId);
        return new ResponseEntity<>(createdDeliveryCompany, HttpStatus.CREATED);
    }

    // Read operations
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/{id}")
    public ResponseEntity<DeliveryCompany> getDeliveryCompanyById(@PathVariable Long id) {
        assertCompanyReadAccess(id);
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(id);
        return ResponseEntity.ok(deliveryCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping
    public ResponseEntity<Page<DeliveryCompany>> getAllDeliveryCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        if (securityService.getCurrentRole() == com.upstart.backend.entity.User.Role.DELIVERY_OWNER) {
            Long ownerId = securityService.getCurrentDeliveryOwner().getId();
            return ResponseEntity.ok(deliveryCompanyService.findByOwner(ownerId, pageable));
        }
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.getAllDeliveryCompanies(pageable);
        return ResponseEntity.ok(deliveryCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/search")
    public ResponseEntity<Page<DeliveryCompany>> searchDeliveryCompanies(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String businessAddress,
            @RequestParam(required = false) String businessLicense,
            @RequestParam(required = false) String contactEmail,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String serviceArea,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer minTotalDeliveries,
            @RequestParam(required = false) BigDecimal minTotalRevenue,
            @RequestParam(required = false) BigDecimal maxCommissionRate,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registeredAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registeredBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.searchDeliveryCompanies(
                companyName, businessAddress, businessLicense, contactEmail, contactPhone,
                serviceType, serviceArea, isActive, isVerified, ownerId,
                minRating, minTotalDeliveries, minTotalRevenue, maxCommissionRate, minCapacity,
                registeredAfter, registeredBefore, createdAfter, createdBefore, pageable);
        
        return ResponseEntity.ok(deliveryCompanies);
    }

    // Quick search endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/search/name-region")
    public ResponseEntity<Page<DeliveryCompany>> searchByNameOrRegion(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be null or empty");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.searchByNameOrRegion(searchTerm.trim(), pageable);
        return ResponseEntity.ok(deliveryCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/search/contact")
    public ResponseEntity<Page<DeliveryCompany>> searchByContact(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be null or empty");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.searchByContact(searchTerm.trim(), pageable);
        return ResponseEntity.ok(deliveryCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/active-licensed")
    public ResponseEntity<Page<DeliveryCompany>> getActiveAndLicensedDeliveryCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.getActiveAndLicensedDeliveryCompanies(pageable);
        return ResponseEntity.ok(deliveryCompanies);
    }

 

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/popular")
    public ResponseEntity<Page<DeliveryCompany>> getPopularDeliveryCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.getPopularDeliveryCompanies(pageable);
        return ResponseEntity.ok(deliveryCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/high-capacity")
    public ResponseEntity<Page<DeliveryCompany>> getHighCapacityDeliveryCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.getHighCapacityDeliveryCompanies(pageable);
        return ResponseEntity.ok(deliveryCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/new")
    public ResponseEntity<Page<DeliveryCompany>> getNewDeliveryCompanies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.getNewDeliveryCompanies(since, pageable);
        return ResponseEntity.ok(deliveryCompanies);
    }

    // Lookup endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/name/{companyName}")
    public ResponseEntity<DeliveryCompany> getByCompanyName(@PathVariable String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be null or empty");
        }
        Optional<DeliveryCompany> deliveryCompany = deliveryCompanyService.findByCompanyName(companyName.trim());
        return deliveryCompany.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/license/{operatingLicense}")
    public ResponseEntity<DeliveryCompany> getByOperatingLicense(@PathVariable String operatingLicense) {
        if (operatingLicense == null || operatingLicense.trim().isEmpty()) {
            throw new IllegalArgumentException("Operating license cannot be null or empty");
        }
        Optional<DeliveryCompany> deliveryCompany = deliveryCompanyService.findByOperatingLicense(operatingLicense.trim());
        return deliveryCompany.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/email/{contactEmail}")
    public ResponseEntity<DeliveryCompany> getByContactEmail(@PathVariable String contactEmail) {
        if (contactEmail == null || contactEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact email cannot be null or empty");
        }
        Optional<DeliveryCompany> deliveryCompany = deliveryCompanyService.findByContactEmail(contactEmail.trim());
        return deliveryCompany.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<DeliveryCompany>> getByOwner(@PathVariable Long ownerId) {
        assertOwnerIdMatchesCurrent(ownerId);
        List<DeliveryCompany> deliveryCompanies = deliveryCompanyService.findByOwnerId(ownerId);
        return ResponseEntity.ok(deliveryCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/owner/{ownerId}/paginated")
    public ResponseEntity<Page<DeliveryCompany>> getByOwnerPaginated(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        assertOwnerIdMatchesCurrent(ownerId);
        Page<DeliveryCompany> deliveryCompanies = deliveryCompanyService.findByOwner(ownerId, pageable);
        return ResponseEntity.ok(deliveryCompanies);
    }

    // Update operations
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<DeliveryCompany> updateDeliveryCompany(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryCompany deliveryCompany) {
        assertCompanyReadAccess(id);
        DeliveryCompany updatedDeliveryCompany = deliveryCompanyService.updateDeliveryCompany(id, deliveryCompany);
        return ResponseEntity.ok(updatedDeliveryCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/active")
    public ResponseEntity<DeliveryCompany> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        DeliveryCompany updatedDeliveryCompany = deliveryCompanyService.updateActiveStatus(id, isActive);
        return ResponseEntity.ok(updatedDeliveryCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/licensed")
    public ResponseEntity<DeliveryCompany> updateLicensedStatus(
            @PathVariable Long id,
            @RequestParam boolean isLicensed) {
        DeliveryCompany updatedDeliveryCompany = deliveryCompanyService.updateLicensedStatus(id, isLicensed);
        return ResponseEntity.ok(updatedDeliveryCompany);
    }

    // Note: Rating update commented out until rating field is added to entity
    /*
    @PatchMapping("/{id}/rating")
    public ResponseEntity<DeliveryCompany> updateRating(
            @PathVariable Long id,
            @RequestParam BigDecimal rating) {
        DeliveryCompany updatedDeliveryCompany = deliveryCompanyService.updateRating(id, rating);
        return ResponseEntity.ok(updatedDeliveryCompany);
    }
    */

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/deliveries")
    public ResponseEntity<DeliveryCompany> updateTotalDeliveries(
            @PathVariable Long id,
            @RequestParam Long totalDeliveries) {
        DeliveryCompany updatedDeliveryCompany = deliveryCompanyService.updateTotalDeliveries(id, totalDeliveries);
        return ResponseEntity.ok(updatedDeliveryCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/revenue")
    public ResponseEntity<DeliveryCompany> updateTotalRevenue(
            @PathVariable Long id,
            @RequestParam BigDecimal totalRevenue) {
        DeliveryCompany updatedDeliveryCompany = deliveryCompanyService.updateTotalRevenue(id, totalRevenue);
        return ResponseEntity.ok(updatedDeliveryCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/commission-rate")
    public ResponseEntity<DeliveryCompany> updateCommissionRate(
            @PathVariable Long id,
            @RequestParam BigDecimal commissionRate) {
        DeliveryCompany updatedDeliveryCompany = deliveryCompanyService.updateCommissionRate(id, commissionRate);
        return ResponseEntity.ok(updatedDeliveryCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/max-drivers")
    public ResponseEntity<DeliveryCompany> updateMaxDrivers(
            @PathVariable Long id,
            @RequestParam Integer maxDrivers) {
        DeliveryCompany updatedDeliveryCompany = deliveryCompanyService.updateMaxDrivers(id, maxDrivers);
        return ResponseEntity.ok(updatedDeliveryCompany);
    }

    // Delete operations
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDeliveryCompany(@PathVariable Long id) {
        deliveryCompanyService.deleteDeliveryCompany(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "DeliveryCompany deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}/soft")
    public ResponseEntity<Map<String, String>> softDeleteDeliveryCompany(@PathVariable Long id) {
        deliveryCompanyService.softDeleteDeliveryCompany(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "DeliveryCompany soft deleted successfully");
        return ResponseEntity.ok(response);
    }

    // Statistical endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count")
    public ResponseEntity<Map<String, Object>> getDeliveryCompanyCounts() {
        long total = deliveryCompanyService.countAllDeliveryCompanies();
        long active = deliveryCompanyService.countActiveDeliveryCompanies();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("inactive", total - active);
        stats.put("verified", deliveryCompanyService.countLicensedDeliveryCompanies());
        stats.put("activeAndVerified", deliveryCompanyService.countActiveAndLicensedDeliveryCompanies());
        stats.put("highRated", deliveryCompanyService.countHighRatedDeliveryCompanies());
        stats.put("withMultipleVehicles", deliveryCompanyService.countWithMultipleVehicleTypes());
        stats.put("pending", 0L);
        stats.put("totalRevenue", deliveryCompanyService.getTotalRevenue());
        // Backwards-compatible legacy keys
        stats.put("licensed", deliveryCompanyService.countLicensedDeliveryCompanies());
        stats.put("activeAndLicensed", deliveryCompanyService.countActiveAndLicensedDeliveryCompanies());
        stats.put("popular", deliveryCompanyService.countPopularDeliveryCompanies());
        stats.put("highCapacity", deliveryCompanyService.countHighCapacityDeliveryCompanies());
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/total")
    public ResponseEntity<Long> getTotalDeliveryCompaniesCount() {
        return ResponseEntity.ok(deliveryCompanyService.countAllDeliveryCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/active")
    public ResponseEntity<Long> getActiveDeliveryCompaniesCount() {
        return ResponseEntity.ok(deliveryCompanyService.countActiveDeliveryCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/licensed")
    public ResponseEntity<Long> getLicensedDeliveryCompaniesCount() {
        return ResponseEntity.ok(deliveryCompanyService.countLicensedDeliveryCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/active-licensed")
    public ResponseEntity<Long> getActiveAndLicensedDeliveryCompaniesCount() {
        return ResponseEntity.ok(deliveryCompanyService.countActiveAndLicensedDeliveryCompanies());
    }

   
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/popular")
    public ResponseEntity<Long> getPopularDeliveryCompaniesCount() {
        return ResponseEntity.ok(deliveryCompanyService.countPopularDeliveryCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/high-capacity")
    public ResponseEntity<Long> getHighCapacityDeliveryCompaniesCount() {
        return ResponseEntity.ok(deliveryCompanyService.countHighCapacityDeliveryCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/by-owner/{ownerId}")
    public ResponseEntity<Long> getDeliveryCompaniesCountByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(deliveryCompanyService.countDeliveryCompaniesByOwner(ownerId));
    }

    // Validation endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/exists/name/{companyName}")
    public ResponseEntity<Boolean> existsByCompanyName(@PathVariable String companyName) {
        return ResponseEntity.ok(deliveryCompanyService.existsByCompanyName(companyName));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/exists/license/{operatingLicense}")
    public ResponseEntity<Boolean> existsByOperatingLicense(@PathVariable String operatingLicense) {
        return ResponseEntity.ok(deliveryCompanyService.existsByOperatingLicense(operatingLicense));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/exists/email/{contactEmail}")
    public ResponseEntity<Boolean> existsByContactEmail(@PathVariable String contactEmail) {
        return ResponseEntity.ok(deliveryCompanyService.existsByContactEmail(contactEmail));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/exists/name/{companyName}/exclude/{id}")
    public ResponseEntity<Boolean> existsByCompanyNameAndIdNot(
            @PathVariable String companyName,
            @PathVariable Long id) {
        return ResponseEntity.ok(deliveryCompanyService.existsByCompanyNameAndIdNot(companyName, id));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/exists/license/{operatingLicense}/exclude/{id}")
    public ResponseEntity<Boolean> existsByOperatingLicenseAndIdNot(
            @PathVariable String operatingLicense,
            @PathVariable Long id) {
        return ResponseEntity.ok(deliveryCompanyService.existsByOperatingLicenseAndIdNot(operatingLicense, id));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    @GetMapping("/exists/email/{contactEmail}/exclude/{id}")
    public ResponseEntity<Boolean> existsByContactEmailAndIdNot(
            @PathVariable String contactEmail,
            @PathVariable Long id) {
        return ResponseEntity.ok(deliveryCompanyService.existsByContactEmailAndIdNot(contactEmail, id));
    }
}