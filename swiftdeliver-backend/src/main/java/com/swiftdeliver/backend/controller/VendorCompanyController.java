package com.swiftdeliver.backend.controller;

import com.swiftdeliver.backend.entity.User;
import com.swiftdeliver.backend.entity.VendorCompany;
import com.swiftdeliver.backend.service.SecurityService;
import com.swiftdeliver.backend.service.VendorCompanyService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/vendor-companies")
public class VendorCompanyController {

    @Autowired
    private VendorCompanyService vendorCompanyService;

    @Autowired
    private SecurityService securityService;

    // Create operations
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<VendorCompany> createVendorCompany(@Valid @RequestBody VendorCompany vendorCompany) {
        VendorCompany createdVendorCompany = vendorCompanyService.createVendorCompany(vendorCompany);
        return new ResponseEntity<>(createdVendorCompany, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/with-owner/{ownerId}")
    public ResponseEntity<VendorCompany> createVendorCompanyWithOwner(
            @PathVariable Long ownerId,
            @Valid @RequestBody VendorCompany vendorCompany) {
        VendorCompany createdVendorCompany = vendorCompanyService.createVendorCompanyWithOwner(vendorCompany, ownerId);
        return new ResponseEntity<>(createdVendorCompany, HttpStatus.CREATED);
    }

    // Read operations
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/{id}")
    public ResponseEntity<VendorCompany> getVendorCompanyById(@PathVariable Long id) {
        VendorCompany vendorCompany = securityService.getOwnedVendorCompanyOrThrow(id);
        return ResponseEntity.ok(vendorCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<VendorCompany>> getAllVendorCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VendorCompany> vendorCompanies = vendorCompanyService.getAllVendorCompanies(pageable);
        return ResponseEntity.ok(vendorCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<Page<VendorCompany>> searchVendorCompanies(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String businessAddress,
            @RequestParam(required = false) String businessLicense,
            @RequestParam(required = false) String contactEmail,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String businessDescription,
            @RequestParam(required = false) String serviceArea,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer minTotalOrders,
            @RequestParam(required = false) BigDecimal minTotalRevenue,
            @RequestParam(required = false) BigDecimal maxCommissionRate,
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
        
        Page<VendorCompany> vendorCompanies = vendorCompanyService.searchVendorCompanies(
                companyName, businessAddress, businessLicense, contactEmail, contactPhone,
                businessDescription, serviceArea, isActive, isVerified, ownerId,
                minRating, minTotalOrders, minTotalRevenue, maxCommissionRate,
                registeredAfter, registeredBefore, createdAfter, createdBefore, pageable);
        
        return ResponseEntity.ok(vendorCompanies);
    }

    // Quick search endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/search/name-category")
    public ResponseEntity<Page<VendorCompany>> searchByNameOrDescription(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be null or empty");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorCompany> vendorCompanies = vendorCompanyService.searchByNameOrDescription(searchTerm.trim(), pageable);
        return ResponseEntity.ok(vendorCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/search/contact")
    public ResponseEntity<Page<VendorCompany>> searchByContact(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be null or empty");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorCompany> vendorCompanies = vendorCompanyService.searchByContact(searchTerm.trim(), pageable);
        return ResponseEntity.ok(vendorCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/active-verified")
    public ResponseEntity<Page<VendorCompany>> getActiveAndVerifiedVendorCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorCompany> vendorCompanies = vendorCompanyService.getActiveAndVerifiedVendorCompanies(pageable);
        return ResponseEntity.ok(vendorCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/high-rated")
    public ResponseEntity<Page<VendorCompany>> getHighRatedVendorCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorCompany> vendorCompanies = vendorCompanyService.getHighRatedVendorCompanies(pageable);
        return ResponseEntity.ok(vendorCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/popular")
    public ResponseEntity<Page<VendorCompany>> getPopularVendorCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorCompany> vendorCompanies = vendorCompanyService.getPopularVendorCompanies(pageable);
        return ResponseEntity.ok(vendorCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/new")
    public ResponseEntity<Page<VendorCompany>> getNewVendorCompanies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorCompany> vendorCompanies = vendorCompanyService.getNewVendorCompanies(since, pageable);
        return ResponseEntity.ok(vendorCompanies);
    }

    // Lookup endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/name/{companyName}")
    public ResponseEntity<VendorCompany> getByCompanyName(@PathVariable String companyName) {
        Optional<VendorCompany> vendorCompany = vendorCompanyService.findByCompanyName(companyName);
        return vendorCompany.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/license/{businessLicense}")
    public ResponseEntity<VendorCompany> getByBusinessLicense(@PathVariable String businessLicense) {
        Optional<VendorCompany> vendorCompany = vendorCompanyService.findByBusinessLicense(businessLicense);
        return vendorCompany.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/email/{contactEmail}")
    public ResponseEntity<VendorCompany> getByContactEmail(@PathVariable String contactEmail) {
        Optional<VendorCompany> vendorCompany = vendorCompanyService.findByContactEmail(contactEmail);
        return vendorCompany.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<VendorCompany>> getByOwner(@PathVariable Long ownerId) {
        Long requestedOwnerId = securityService.resolveOwnerIdForCurrentUser(ownerId, User.Role.VENDOR_OWNER);
        List<VendorCompany> vendorCompanies = vendorCompanyService.findByOwnerId(requestedOwnerId);
        return ResponseEntity.ok(vendorCompanies);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/owner/{ownerId}/paginated")
    public ResponseEntity<Page<VendorCompany>> getByOwnerPaginated(
            @PathVariable Long ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long requestedOwnerId = securityService.resolveOwnerIdForCurrentUser(ownerId, User.Role.VENDOR_OWNER);
        Pageable pageable = PageRequest.of(page, size);
        Page<VendorCompany> vendorCompanies = vendorCompanyService.findByOwner(requestedOwnerId, pageable);
        return ResponseEntity.ok(vendorCompanies);
    }

    // Update operations
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<VendorCompany> updateVendorCompany(
            @PathVariable Long id,
            @Valid @RequestBody VendorCompany vendorCompany) {
        securityService.getOwnedVendorCompanyOrThrow(id);
        VendorCompany updatedVendorCompany = vendorCompanyService.updateVendorCompany(id, vendorCompany);
        return ResponseEntity.ok(updatedVendorCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/active")
    public ResponseEntity<VendorCompany> updateActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        VendorCompany updatedVendorCompany = vendorCompanyService.updateActiveStatus(id, isActive);
        return ResponseEntity.ok(updatedVendorCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/verified")
    public ResponseEntity<VendorCompany> updateVerifiedStatus(
            @PathVariable Long id,
            @RequestParam boolean isVerified) {
        VendorCompany updatedVendorCompany = vendorCompanyService.updateVerifiedStatus(id, isVerified);
        return ResponseEntity.ok(updatedVendorCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/rating")
    public ResponseEntity<VendorCompany> updateRating(
            @PathVariable Long id,
            @RequestParam BigDecimal rating) {
        VendorCompany updatedVendorCompany = vendorCompanyService.updateRating(id, rating);
        return ResponseEntity.ok(updatedVendorCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/orders")
    public ResponseEntity<VendorCompany> updateTotalOrders(
            @PathVariable Long id,
            @RequestParam Integer totalOrders) {
        VendorCompany updatedVendorCompany = vendorCompanyService.updateTotalOrders(id, totalOrders);
        return ResponseEntity.ok(updatedVendorCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/revenue")
    public ResponseEntity<VendorCompany> updateTotalRevenue(
            @PathVariable Long id,
            @RequestParam BigDecimal totalRevenue) {
        VendorCompany updatedVendorCompany = vendorCompanyService.updateTotalRevenue(id, totalRevenue);
        return ResponseEntity.ok(updatedVendorCompany);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/commission-rate")
    public ResponseEntity<VendorCompany> updateCommissionRate(
            @PathVariable Long id,
            @RequestParam BigDecimal commissionRate) {
        VendorCompany updatedVendorCompany = vendorCompanyService.updateCommissionRate(id, commissionRate);
        return ResponseEntity.ok(updatedVendorCompany);
    }

    // Delete operations
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteVendorCompany(@PathVariable Long id) {
        vendorCompanyService.deleteVendorCompany(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "VendorCompany deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}/soft")
    public ResponseEntity<Map<String, String>> softDeleteVendorCompany(@PathVariable Long id) {
        vendorCompanyService.softDeleteVendorCompany(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "VendorCompany soft deleted successfully");
        return ResponseEntity.ok(response);
    }

    // Statistical endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count")
    public ResponseEntity<Map<String, Object>> getVendorCompanyCounts() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", vendorCompanyService.countAllVendorCompanies());
        stats.put("active", vendorCompanyService.countActiveVendorCompanies());
        stats.put("pending", 0L);
        stats.put("verified", vendorCompanyService.countVerifiedVendorCompanies());
        stats.put("activeAndVerified", vendorCompanyService.countActiveAndVerifiedVendorCompanies());
        stats.put("highRated", vendorCompanyService.countHighRatedVendorCompanies());
        stats.put("withMultipleProducts", vendorCompanyService.countVendorCompaniesWithProducts());
        stats.put("recentlyEstablished", vendorCompanyService.countRecentlyEstablishedVendorCompanies());
        stats.put("totalRevenue", vendorCompanyService.getTotalRevenue());
        // Backwards-compatible legacy keys
        stats.put("popular", vendorCompanyService.countPopularVendorCompanies());
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/total")
    public ResponseEntity<Long> getTotalVendorCompaniesCount() {
        return ResponseEntity.ok(vendorCompanyService.countAllVendorCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/active")
    public ResponseEntity<Long> getActiveVendorCompaniesCount() {
        return ResponseEntity.ok(vendorCompanyService.countActiveVendorCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/verified")
    public ResponseEntity<Long> getVerifiedVendorCompaniesCount() {
        return ResponseEntity.ok(vendorCompanyService.countVerifiedVendorCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/active-verified")
    public ResponseEntity<Long> getActiveAndVerifiedVendorCompaniesCount() {
        return ResponseEntity.ok(vendorCompanyService.countActiveAndVerifiedVendorCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/high-rated")
    public ResponseEntity<Long> getHighRatedVendorCompaniesCount() {
        return ResponseEntity.ok(vendorCompanyService.countHighRatedVendorCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/popular")
    public ResponseEntity<Long> getPopularVendorCompaniesCount() {
        return ResponseEntity.ok(vendorCompanyService.countPopularVendorCompanies());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/stats/count/by-owner/{ownerId}")
    public ResponseEntity<Long> getVendorCompaniesCountByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(vendorCompanyService.countVendorCompaniesByOwner(ownerId));
    }

    // Validation endpoints
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/exists/name/{companyName}")
    public ResponseEntity<Boolean> existsByCompanyName(@PathVariable String companyName) {
        return ResponseEntity.ok(vendorCompanyService.existsByCompanyName(companyName));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/exists/license/{businessLicense}")
    public ResponseEntity<Boolean> existsByBusinessLicense(@PathVariable String businessLicense) {
        return ResponseEntity.ok(vendorCompanyService.existsByBusinessLicense(businessLicense));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/exists/email/{contactEmail}")
    public ResponseEntity<Boolean> existsByContactEmail(@PathVariable String contactEmail) {
        return ResponseEntity.ok(vendorCompanyService.existsByContactEmail(contactEmail));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/exists/name/{companyName}/exclude/{id}")
    public ResponseEntity<Boolean> existsByCompanyNameAndIdNot(
            @PathVariable String companyName,
            @PathVariable Long id) {
        return ResponseEntity.ok(vendorCompanyService.existsByCompanyNameAndIdNot(companyName, id));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/exists/license/{businessLicense}/exclude/{id}")
    public ResponseEntity<Boolean> existsByBusinessLicenseAndIdNot(
            @PathVariable String businessLicense,
            @PathVariable Long id) {
        return ResponseEntity.ok(vendorCompanyService.existsByBusinessLicenseAndIdNot(businessLicense, id));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    @GetMapping("/exists/email/{contactEmail}/exclude/{id}")
    public ResponseEntity<Boolean> existsByContactEmailAndIdNot(
            @PathVariable String contactEmail,
            @PathVariable Long id) {
        return ResponseEntity.ok(vendorCompanyService.existsByContactEmailAndIdNot(contactEmail, id));
    }
}