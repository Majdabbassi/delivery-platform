package com.upstart.backend.controller;

import com.upstart.backend.entity.DriverPerson;
import com.upstart.backend.service.DriverPersonService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/driver-persons")
@RequiredArgsConstructor
@Slf4j
public class DriverPersonController {

    private final DriverPersonService driverPersonService;

    // CREATE
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new driver person", description = "Creates a new driver person with the provided details")
    public ResponseEntity<DriverPerson> createDriverPerson(@RequestBody DriverPerson driverPerson) {
        log.info("Creating new driver person: {}", driverPerson.getUsername());
        DriverPerson createdDriverPerson = driverPersonService.createDriverPerson(driverPerson);
        return new ResponseEntity<>(createdDriverPerson, HttpStatus.CREATED);
    }

    // READ - Get driver person by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DRIVER')")
    @Operation(summary = "Get driver person by ID", description = "Retrieves a driver person by their unique identifier")
    public ResponseEntity<DriverPerson> getDriverPersonById(@PathVariable Long id) {
        log.info("Fetching driver person with ID: {}", id);
        DriverPerson driverPerson = driverPersonService.getDriverPersonById(id);
        return ResponseEntity.ok(driverPerson);
    }

    // READ - Get all driver persons with pagination
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all driver persons", description = "Retrieves all driver persons with pagination")
    public ResponseEntity<Page<DriverPerson>> getAllDriverPersons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.info("Fetching all driver persons - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DriverPerson> driverPersons = driverPersonService.getAllDriverPersons(pageable);
        return ResponseEntity.ok(driverPersons);
    }

    // READ - Search driver persons with dynamic criteria
    @GetMapping("/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search driver persons with dynamic criteria", description = "Search driver persons using multiple optional criteria with pagination and sorting")
    public ResponseEntity<Page<DriverPerson>> searchDriverPersons(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String licenseNumber,
            @RequestParam(required = false) String vehiclePlate,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String deliveryZone,
            @RequestParam(required = false) String currentLocation,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) BigDecimal maxRating,
            @RequestParam(required = false) Long minTotalDeliveries,
            @RequestParam(required = false) Long maxTotalDeliveries,
            @RequestParam(required = false) BigDecimal minTotalEarnings,
            @RequestParam(required = false) BigDecimal maxTotalEarnings,
            @RequestParam(required = false) String vehicleModel,
            @RequestParam(required = false) String vehicleColor,
            @RequestParam(required = false) LocalDateTime lastActiveAfter,
            @RequestParam(required = false) LocalDateTime lastActiveBefore,
            @RequestParam(required = false) LocalDateTime createdAfter,
            @RequestParam(required = false) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.info("Searching driver persons with criteria");
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DriverPerson> driverPersons = driverPersonService.searchDriverPersons(
            username, email, firstName, lastName, licenseNumber, vehiclePlate,
            phoneNumber, isAvailable, isVerified, enabled, vehicleType,
            deliveryZone, currentLocation, minRating, maxRating,
            minTotalDeliveries, maxTotalDeliveries, minTotalEarnings, maxTotalEarnings,
            vehicleModel, vehicleColor, lastActiveAfter, lastActiveBefore,
            createdAfter, createdBefore, pageable);
        
        return ResponseEntity.ok(driverPersons);
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DRIVER')")
    @Operation(summary = "Update driver person", description = "Updates an existing driver person's details")
    public ResponseEntity<DriverPerson> updateDriverPerson(@PathVariable Long id, @RequestBody DriverPerson driverPersonDetails) {
        log.info("Updating driver person with ID: {}", id);
        DriverPerson updatedDriverPerson = driverPersonService.updateDriverPerson(id, driverPersonDetails);
        return ResponseEntity.ok(updatedDriverPerson);
    }

    // UPDATE - Update availability status
    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DRIVER')")
    public ResponseEntity<DriverPerson> updateAvailabilityStatus(
            @PathVariable Long id, 
            @RequestParam boolean isAvailable) {
        log.info("Updating availability status for driver person ID: {} to: {}", id, isAvailable);
        DriverPerson updatedDriverPerson = driverPersonService.updateAvailabilityStatus(id, isAvailable);
        return ResponseEntity.ok(updatedDriverPerson);
    }

    // UPDATE - Update verification status
    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DriverPerson> updateVerificationStatus(
            @PathVariable Long id, 
            @RequestParam boolean isVerified) {
        log.info("Updating verification status for driver person ID: {} to: {}", id, isVerified);
        DriverPerson updatedDriverPerson = driverPersonService.updateVerificationStatus(id, isVerified);
        return ResponseEntity.ok(updatedDriverPerson);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Soft delete driver person", description = "Soft deletes a driver person (disables the account)")
    public ResponseEntity<Void> deleteDriverPerson(@PathVariable Long id) {
        log.info("Deleting driver person with ID: {}", id);
        driverPersonService.deleteDriverPerson(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Hard delete driver person", description = "Permanently deletes a driver person from the database")
    public ResponseEntity<Void> hardDeleteDriverPerson(@PathVariable Long id) {
        log.info("Deleting driver person with ID: {}", id);
        driverPersonService.deleteDriverPerson(id);
        return ResponseEntity.noContent().build();
    }

    // STATISTICS
    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDriverPersonCounts() {
        Map<String, Object> stats = driverPersonService.getDriverPersonStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/count/total")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countTotalDriverPersons() {
        return ResponseEntity.ok(driverPersonService.countAllDriverPersons());
    }

    @GetMapping("/stats/count/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countActiveDriverPersons() {
        Long count = driverPersonService.countActiveDriverPersons();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/count/available")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countAvailableDriverPersons() {
        Long count = driverPersonService.countAvailableDriverPersons();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/count/verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countVerifiedDriverPersons() {
        Long count = driverPersonService.countVerifiedDriverPersons();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/count/active-verified")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countActiveAndVerifiedDriverPersons() {
        return ResponseEntity.ok(driverPersonService.countActiveAndVerifiedDriverPersons());
    }

    @GetMapping("/stats/count/experienced")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countExperiencedDriverPersons() {
        return ResponseEntity.ok(driverPersonService.countExperiencedDriverPersons());
    }

    @GetMapping("/stats/count/multiple-deliveries")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countDriversWithMultipleDeliveries() {
        return ResponseEntity.ok(driverPersonService.countDriversWithMultipleDeliveries());
    }

    @GetMapping("/stats/average-rating")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BigDecimal> getAverageRating() {
        BigDecimal averageRating = driverPersonService.getAverageRating();
        return ResponseEntity.ok(averageRating);
    }

    @GetMapping("/stats/average-earnings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BigDecimal> getAverageEarnings() {
        BigDecimal averageEarnings = driverPersonService.getAverageEarnings();
        return ResponseEntity.ok(averageEarnings);
    }

    @GetMapping("/stats/total-deliveries")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getTotalDeliveries() {
        Long totalDeliveries = driverPersonService.getTotalDeliveries();
        return ResponseEntity.ok(totalDeliveries);
    }
}