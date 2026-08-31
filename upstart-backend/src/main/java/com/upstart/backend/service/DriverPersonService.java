package com.upstart.backend.service;

import com.upstart.backend.entity.DriverPerson;
import com.upstart.backend.repository.DriverPersonRepository;
import com.upstart.backend.specification.DriverPersonSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DriverPersonService {

    private final DriverPersonRepository driverPersonRepository;
    private final PasswordEncoder passwordEncoder;

    // CREATE
    public DriverPerson createDriverPerson(DriverPerson driverPerson) {
        log.info("Creating new driver person with username: {} and license: {}", 
                driverPerson.getUsername(), driverPerson.getLicenseNumber());
        
        // Check if username or email already exists
        if (driverPersonRepository.findByUsername(driverPerson.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + driverPerson.getUsername());
        }
        
        if (driverPersonRepository.findByEmail(driverPerson.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + driverPerson.getEmail());
        }
        
        // Check if license number already exists
        if (driverPersonRepository.findByLicenseNumber(driverPerson.getLicenseNumber()).isPresent()) {
            throw new RuntimeException("License number already exists: " + driverPerson.getLicenseNumber());
        }
        
        // Check if vehicle plate already exists
        if (driverPersonRepository.findByVehiclePlate(driverPerson.getVehiclePlate()).isPresent()) {
            throw new RuntimeException("Vehicle plate already exists: " + driverPerson.getVehiclePlate());
        }
        
        // Check if phone number already exists
        if (driverPersonRepository.findByPhoneNumber(driverPerson.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number already exists: " + driverPerson.getPhoneNumber());
        }
        
        // Set initial values
        driverPerson.setLastActive(LocalDateTime.now());
        
        if (driverPerson.getPassword() != null && !driverPerson.getPassword().startsWith("$2")) {
            driverPerson.setPassword(passwordEncoder.encode(driverPerson.getPassword()));
        }
        
        DriverPerson savedDriverPerson = driverPersonRepository.save(driverPerson);
        log.info("Driver person created successfully with ID: {}", savedDriverPerson.getId());
        return savedDriverPerson;
    }

    // READ - Get by ID
    @Transactional(readOnly = true)
    public DriverPerson getDriverPersonById(Long id) {
        log.debug("Fetching driver person by ID: {}", id);
        return driverPersonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + id));
    }

    // READ - Get by username
    @Transactional(readOnly = true)
    public Optional<DriverPerson> getDriverPersonByUsername(String username) {
        log.debug("Fetching driver person by username: {}", username);
        return driverPersonRepository.findByUsername(username);
    }

    // READ - Get by email
    @Transactional(readOnly = true)
    public Optional<DriverPerson> getDriverPersonByEmail(String email) {
        log.debug("Fetching driver person by email: {}", email);
        return driverPersonRepository.findByEmail(email);
    }

    // READ - Get by license number
    @Transactional(readOnly = true)
    public Optional<DriverPerson> getDriverPersonByLicenseNumber(String licenseNumber) {
        log.debug("Fetching driver person by license number: {}", licenseNumber);
        return driverPersonRepository.findByLicenseNumber(licenseNumber);
    }

    // READ - Get by phone number
    @Transactional(readOnly = true)
    public Optional<DriverPerson> getDriverPersonByPhoneNumber(String phoneNumber) {
        log.debug("Fetching driver person by phone number: {}", phoneNumber);
        return driverPersonRepository.findByPhoneNumber(phoneNumber);
    }

    // READ - Get by vehicle plate
    @Transactional(readOnly = true)
    public Optional<DriverPerson> getDriverPersonByVehiclePlate(String vehiclePlate) {
        log.debug("Fetching driver person by vehicle plate: {}", vehiclePlate);
        return driverPersonRepository.findByVehiclePlate(vehiclePlate);
    }

    // READ - Get all driver persons
    @Transactional(readOnly = true)
    public List<DriverPerson> getAllDriverPersons() {
        log.debug("Fetching all driver persons");
        return driverPersonRepository.findAll();
    }

    // READ - Get all driver persons with pagination
    @Transactional(readOnly = true)
    public Page<DriverPerson> getAllDriverPersons(Pageable pageable) {
        log.debug("Fetching all driver persons with pagination");
        return driverPersonRepository.findAll(pageable);
    }

    // READ - Search driver persons with dynamic criteria
    @Transactional(readOnly = true)
    public Page<DriverPerson> searchDriverPersons(
            String username,
            String email,
            String firstName,
            String lastName,
            String licenseNumber,
            String vehiclePlate,
            String phoneNumber,
            Boolean isAvailable,
            Boolean isVerified,
            Boolean enabled,
            String vehicleType,
            String deliveryZone,
            String currentLocation,
            BigDecimal minRating,
            BigDecimal maxRating,
            Long minTotalDeliveries,
            Long maxTotalDeliveries,
            BigDecimal minTotalEarnings,
            BigDecimal maxTotalEarnings,
            String vehicleModel,
            String vehicleColor,
            LocalDateTime lastActiveAfter,
            LocalDateTime lastActiveBefore,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable) {
        
        log.debug("Searching driver persons with dynamic criteria");
        
        Specification<DriverPerson> spec = null;
        
        if (username != null && !username.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasUsername(username) : spec.and(DriverPersonSpecifications.hasUsername(username));
        }
        if (email != null && !email.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasEmail(email) : spec.and(DriverPersonSpecifications.hasEmail(email));
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasFirstName(firstName) : spec.and(DriverPersonSpecifications.hasFirstName(firstName));
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasLastName(lastName) : spec.and(DriverPersonSpecifications.hasLastName(lastName));
        }
        if (licenseNumber != null && !licenseNumber.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasLicenseNumber(licenseNumber) : spec.and(DriverPersonSpecifications.hasLicenseNumber(licenseNumber));
        }
        if (vehiclePlate != null && !vehiclePlate.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasVehiclePlate(vehiclePlate) : spec.and(DriverPersonSpecifications.hasVehiclePlate(vehiclePlate));
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasPhoneNumber(phoneNumber) : spec.and(DriverPersonSpecifications.hasPhoneNumber(phoneNumber));
        }
        if (isAvailable != null) {
            spec = (spec == null) ? DriverPersonSpecifications.isAvailable(isAvailable) : spec.and(DriverPersonSpecifications.isAvailable(isAvailable));
        }
        if (isVerified != null) {
            spec = (spec == null) ? DriverPersonSpecifications.isVerified(isVerified) : spec.and(DriverPersonSpecifications.isVerified(isVerified));
        }
        if (enabled != null) {
            spec = (spec == null) ? DriverPersonSpecifications.isEnabled(enabled) : spec.and(DriverPersonSpecifications.isEnabled(enabled));
        }
        if (vehicleType != null && !vehicleType.trim().isEmpty()) {
            try {
                DriverPerson.VehicleType vehicleTypeEnum = DriverPerson.VehicleType.valueOf(vehicleType.toUpperCase());
                spec = (spec == null) ? DriverPersonSpecifications.hasVehicleType(vehicleTypeEnum) : spec.and(DriverPersonSpecifications.hasVehicleType(vehicleTypeEnum));
            } catch (IllegalArgumentException e) {
                // Invalid vehicle type, skip this filter
                log.warn("Invalid vehicle type provided: {}", vehicleType);
            }
        }
        if (deliveryZone != null && !deliveryZone.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasDeliveryZone(deliveryZone) : spec.and(DriverPersonSpecifications.hasDeliveryZone(deliveryZone));
        }
        if (currentLocation != null && !currentLocation.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasCurrentLocation(currentLocation) : spec.and(DriverPersonSpecifications.hasCurrentLocation(currentLocation));
        }
        if (minRating != null) {
            spec = (spec == null) ? DriverPersonSpecifications.hasMinRating(minRating) : spec.and(DriverPersonSpecifications.hasMinRating(minRating));
        }
        if (maxRating != null) {
            spec = (spec == null) ? DriverPersonSpecifications.hasMaxRating(maxRating) : spec.and(DriverPersonSpecifications.hasMaxRating(maxRating));
        }
        if (minTotalDeliveries != null) {
            spec = (spec == null) ? DriverPersonSpecifications.hasMinTotalDeliveries(minTotalDeliveries) : spec.and(DriverPersonSpecifications.hasMinTotalDeliveries(minTotalDeliveries));
        }
        if (maxTotalDeliveries != null) {
            spec = (spec == null) ? DriverPersonSpecifications.hasMaxTotalDeliveries(maxTotalDeliveries) : spec.and(DriverPersonSpecifications.hasMaxTotalDeliveries(maxTotalDeliveries));
        }
        if (minTotalEarnings != null) {
            spec = (spec == null) ? DriverPersonSpecifications.hasMinTotalEarnings(minTotalEarnings) : spec.and(DriverPersonSpecifications.hasMinTotalEarnings(minTotalEarnings));
        }
        if (maxTotalEarnings != null) {
            spec = (spec == null) ? DriverPersonSpecifications.hasMaxTotalEarnings(maxTotalEarnings) : spec.and(DriverPersonSpecifications.hasMaxTotalEarnings(maxTotalEarnings));
        }
        if (vehicleModel != null && !vehicleModel.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasVehicleModel(vehicleModel) : spec.and(DriverPersonSpecifications.hasVehicleModel(vehicleModel));
        }
        if (vehicleColor != null && !vehicleColor.trim().isEmpty()) {
            spec = (spec == null) ? DriverPersonSpecifications.hasVehicleColor(vehicleColor) : spec.and(DriverPersonSpecifications.hasVehicleColor(vehicleColor));
        }
        if (lastActiveAfter != null) {
            spec = (spec == null) ? DriverPersonSpecifications.lastActiveAfter(lastActiveAfter) : spec.and(DriverPersonSpecifications.lastActiveAfter(lastActiveAfter));
        }
        if (lastActiveBefore != null) {
            spec = (spec == null) ? DriverPersonSpecifications.lastActiveBefore(lastActiveBefore) : spec.and(DriverPersonSpecifications.lastActiveBefore(lastActiveBefore));
        }
        if (createdAfter != null) {
            spec = (spec == null) ? DriverPersonSpecifications.createdAfter(createdAfter) : spec.and(DriverPersonSpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? DriverPersonSpecifications.createdBefore(createdBefore) : spec.and(DriverPersonSpecifications.createdBefore(createdBefore));
        }
        
        return driverPersonRepository.findAll(spec, pageable);
    }

    // READ - Get available verified driver persons
    @Transactional(readOnly = true)
    public List<DriverPerson> getAvailableVerifiedDriverPersons() {
        log.debug("Fetching available verified driver persons");
        Specification<DriverPerson> spec = DriverPersonSpecifications.isAvailableAndVerified();
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get driver persons with pagination
    @Transactional(readOnly = true)
    public Page<DriverPerson> getAvailableVerifiedDriverPersons(Pageable pageable) {
        log.debug("Fetching available verified driver persons with pagination");
        Specification<DriverPerson> spec = DriverPersonSpecifications.isAvailableAndVerified();
        return driverPersonRepository.findAll(spec, pageable);
    }

    // READ - Get available driver persons
    @Transactional(readOnly = true)
    public List<DriverPerson> getAvailableDriverPersons() {
        log.debug("Fetching available driver persons");
        Specification<DriverPerson> spec = DriverPersonSpecifications.isAvailable(true);
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get verified driver persons
    @Transactional(readOnly = true)
    public List<DriverPerson> getVerifiedDriverPersons() {
        log.debug("Fetching verified driver persons");
        Specification<DriverPerson> spec = DriverPersonSpecifications.isVerified(true);
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get pending verification driver persons
    @Transactional(readOnly = true)
    public List<DriverPerson> getPendingVerificationDriverPersons() {
        log.debug("Fetching driver persons pending verification");
        Specification<DriverPerson> spec = DriverPersonSpecifications.isPendingVerification();
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get driver persons by vehicle type
    @Transactional(readOnly = true)
    public List<DriverPerson> getDriverPersonsByVehicleType(DriverPerson.VehicleType vehicleType) {
        log.debug("Fetching driver persons by vehicle type: {}", vehicleType);
        Specification<DriverPerson> spec = DriverPersonSpecifications.hasVehicleType(vehicleType);
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get driver persons by delivery zone
    @Transactional(readOnly = true)
    public List<DriverPerson> getDriverPersonsByDeliveryZone(String zone) {
        log.debug("Fetching driver persons by delivery zone: {}", zone);
        Specification<DriverPerson> spec = DriverPersonSpecifications.hasDeliveryZone(zone);
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get available driver persons in zone
    @Transactional(readOnly = true)
    public List<DriverPerson> getAvailableDriverPersonsInZone(String zone) {
        log.debug("Fetching available driver persons in zone: {}", zone);
        Specification<DriverPerson> spec = DriverPersonSpecifications.isAvailableInZone(zone);
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get available driver persons by vehicle type
    @Transactional(readOnly = true)
    public List<DriverPerson> getAvailableDriverPersonsByVehicleType(DriverPerson.VehicleType vehicleType) {
        log.debug("Fetching available driver persons by vehicle type: {}", vehicleType);
        Specification<DriverPerson> spec = DriverPersonSpecifications.isAvailable(true)
                .and(DriverPersonSpecifications.isVerified(true))
                .and(DriverPersonSpecifications.hasVehicleType(vehicleType));
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get driver persons by minimum rating
    @Transactional(readOnly = true)
    public List<DriverPerson> getDriverPersonsByMinimumRating(BigDecimal minRating) {
        log.debug("Fetching driver persons with minimum rating: {}", minRating);
        Specification<DriverPerson> spec = DriverPersonSpecifications.hasMinRating(minRating)
                .and(DriverPersonSpecifications.isVerified(true));
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get top rated driver persons
    @Transactional(readOnly = true)
    public List<DriverPerson> getTopRatedDriverPersons(int limit) {
        log.debug("Fetching top {} rated driver persons", limit);
        Specification<DriverPerson> spec = DriverPersonSpecifications.isVerified(true);
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "rating"));
        return driverPersonRepository.findAll(spec, pageable).getContent();
    }

    // READ - Get top performing driver persons
    @Transactional(readOnly = true)
    public List<DriverPerson> getTopPerformingDriverPersons(int limit) {
        log.debug("Fetching top {} performing driver persons", limit);
        Specification<DriverPerson> spec = DriverPersonSpecifications.isVerified(true);
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "totalDeliveries"));
        return driverPersonRepository.findAll(spec, pageable).getContent();
    }

    // READ - Search driver persons by name
    @Transactional(readOnly = true)
    public List<DriverPerson> searchDriverPersonsByName(String name) {
        log.debug("Searching driver persons by name: {}", name);
        Specification<DriverPerson> spec = DriverPersonSpecifications.hasNameContaining(name);
        return driverPersonRepository.findAll(spec);
    }

    // READ - Get active driver persons since
    @Transactional(readOnly = true)
    public List<DriverPerson> getActiveDriverPersonsSince(LocalDateTime since) {
        log.debug("Fetching active driver persons since: {}", since);
        Specification<DriverPerson> spec = DriverPersonSpecifications.isActiveDriverPersonSince(since);
        return driverPersonRepository.findAll(spec);
    }

    // UPDATE
    public DriverPerson updateDriverPerson(Long id, DriverPerson driverPersonDetails) {
        log.info("Updating driver person with ID: {}", id);
        
        DriverPerson existingDriverPerson = driverPersonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + id));

        // Update fields
        if (driverPersonDetails.getFirstName() != null) {
            existingDriverPerson.setFirstName(driverPersonDetails.getFirstName());
        }
        if (driverPersonDetails.getLastName() != null) {
            existingDriverPerson.setLastName(driverPersonDetails.getLastName());
        }
        if (driverPersonDetails.getEmail() != null && !driverPersonDetails.getEmail().equals(existingDriverPerson.getEmail())) {
            // Check if new email already exists
            if (driverPersonRepository.findByEmail(driverPersonDetails.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists: " + driverPersonDetails.getEmail());
            }
            existingDriverPerson.setEmail(driverPersonDetails.getEmail());
        }
        if (driverPersonDetails.getPhoneNumber() != null && !driverPersonDetails.getPhoneNumber().equals(existingDriverPerson.getPhoneNumber())) {
            // Check if new phone number already exists
            if (driverPersonRepository.findByPhoneNumber(driverPersonDetails.getPhoneNumber()).isPresent()) {
                throw new RuntimeException("Phone number already exists: " + driverPersonDetails.getPhoneNumber());
            }
            existingDriverPerson.setPhoneNumber(driverPersonDetails.getPhoneNumber());
        }
        if (driverPersonDetails.getPassword() != null && !driverPersonDetails.getPassword().startsWith("$2")) {
            existingDriverPerson.setPassword(passwordEncoder.encode(driverPersonDetails.getPassword()));
        }
        if (driverPersonDetails.getVehiclePlate() != null && !driverPersonDetails.getVehiclePlate().equals(existingDriverPerson.getVehiclePlate())) {
            // Check if new vehicle plate already exists
            if (driverPersonRepository.findByVehiclePlate(driverPersonDetails.getVehiclePlate()).isPresent()) {
                throw new RuntimeException("Vehicle plate already exists: " + driverPersonDetails.getVehiclePlate());
            }
            existingDriverPerson.setVehiclePlate(driverPersonDetails.getVehiclePlate());
        }
        if (driverPersonDetails.getVehicleType() != null) {
            existingDriverPerson.setVehicleType(driverPersonDetails.getVehicleType());
        }
        if (driverPersonDetails.getVehicleModel() != null) {
            existingDriverPerson.setVehicleModel(driverPersonDetails.getVehicleModel());
        }
        if (driverPersonDetails.getVehicleColor() != null) {
            existingDriverPerson.setVehicleColor(driverPersonDetails.getVehicleColor());
        }
        if (driverPersonDetails.getDeliveryZone() != null) {
            existingDriverPerson.setDeliveryZone(driverPersonDetails.getDeliveryZone());
        }
        if (driverPersonDetails.getEmergencyContact() != null) {
            existingDriverPerson.setEmergencyContact(driverPersonDetails.getEmergencyContact());
        }
        if (driverPersonDetails.getCurrentLocation() != null) {
            existingDriverPerson.setCurrentLocation(driverPersonDetails.getCurrentLocation());
        }

        DriverPerson updatedDriverPerson = driverPersonRepository.save(existingDriverPerson);
        log.info("Driver person updated successfully with ID: {}", updatedDriverPerson.getId());
        return updatedDriverPerson;
    }

    // UPDATE - Update availability status
    public DriverPerson updateAvailabilityStatus(Long driverPersonId, boolean isAvailable) {
        log.info("Updating availability status for driver person ID: {} to: {}", driverPersonId, isAvailable);
        
        DriverPerson driverPerson = driverPersonRepository.findById(driverPersonId)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + driverPersonId));
        
        driverPerson.setIsAvailable(isAvailable);
        driverPerson.setLastActive(LocalDateTime.now());
        return driverPersonRepository.save(driverPerson);
    }

    // UPDATE - Update verification status
    public DriverPerson updateVerificationStatus(Long driverPersonId, boolean isVerified) {
        log.info("Updating verification status for driver person ID: {} to: {}", driverPersonId, isVerified);
        
        DriverPerson driverPerson = driverPersonRepository.findById(driverPersonId)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + driverPersonId));
        
        driverPerson.setIsVerified(isVerified);
        DriverPerson updatedDriverPerson = driverPersonRepository.save(driverPerson);
        log.info("Driver person verification status updated successfully with ID: {}", driverPersonId);
        return updatedDriverPerson;
    }

    // UPDATE - Update rating
    public DriverPerson updateRating(Long driverPersonId, BigDecimal newRating) {
        log.info("Updating rating for driver person ID: {} to: {}", driverPersonId, newRating);
        
        if (newRating.compareTo(BigDecimal.ZERO) < 0 || newRating.compareTo(new BigDecimal("5.0")) > 0) {
            throw new RuntimeException("Rating must be between 0.0 and 5.0");
        }
        
        DriverPerson driverPerson = driverPersonRepository.findById(driverPersonId)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + driverPersonId));
        
        driverPerson.setRating(newRating);
        return driverPersonRepository.save(driverPerson);
    }

    // UPDATE - Update location
    public DriverPerson updateLocation(Long driverPersonId, String location) {
        log.info("Updating location for driver person ID: {} to: {}", driverPersonId, location);
        
        DriverPerson driverPerson = driverPersonRepository.findById(driverPersonId)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + driverPersonId));
        
        driverPerson.setCurrentLocation(location);
        driverPerson.setLastActive(LocalDateTime.now());
        return driverPersonRepository.save(driverPerson);
    }

    // UPDATE - Complete delivery
    public DriverPerson completeDelivery(Long driverPersonId, BigDecimal earnings) {
        log.info("Recording delivery completion for driver person ID: {} with earnings: {}", driverPersonId, earnings);
        
        DriverPerson driverPerson = driverPersonRepository.findById(driverPersonId)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + driverPersonId));
        
        driverPerson.setTotalDeliveries(driverPerson.getTotalDeliveries() + 1);
        driverPerson.setTotalEarnings(driverPerson.getTotalEarnings().add(earnings));
        driverPerson.setLastActive(LocalDateTime.now());
        
        return driverPersonRepository.save(driverPerson);
    }

    // UPDATE - Update last active
    public DriverPerson updateLastActive(Long driverPersonId) {
        log.debug("Updating last active time for driver person ID: {}", driverPersonId);
        
        DriverPerson driverPerson = driverPersonRepository.findById(driverPersonId)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + driverPersonId));
        
        driverPerson.setLastActive(LocalDateTime.now());
        return driverPersonRepository.save(driverPerson);
    }

    // DELETE - Soft delete (disable)
    public void disableDriverPerson(Long id) {
        log.info("Disabling driver person with ID: {}", id);
        
        DriverPerson driverPerson = driverPersonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver person not found with ID: " + id));
        
        driverPerson.setEnabled(false);
        driverPerson.setIsAvailable(false);
        driverPersonRepository.save(driverPerson);
        log.info("Driver person disabled successfully with ID: {}", id);
    }

    // DELETE - Hard delete
    public void deleteDriverPerson(Long id) {
        log.info("Deleting driver person with ID: {}", id);
        
        if (!driverPersonRepository.existsById(id)) {
            throw new RuntimeException("Driver person not found with ID: " + id);
        }
        
        driverPersonRepository.deleteById(id);
        log.info("Driver person deleted successfully with ID: {}", id);
    }

    // STATISTICS
    @Transactional(readOnly = true)
    public Long countAllDriverPersons() {
        return driverPersonRepository.count();
    }

    @Transactional(readOnly = true)
    public Long countActiveDriverPersons() {
        return driverPersonRepository.countActiveDriverPersons();
    }

    @Transactional(readOnly = true)
    public Long countAvailableDriverPersons() {
        return driverPersonRepository.countAvailableDriverPersons();
    }

    @Transactional(readOnly = true)
    public Long countVerifiedDriverPersons() {
        return driverPersonRepository.countVerifiedDriverPersons();
    }

    @Transactional(readOnly = true)
    public Long countPendingVerification() {
        return driverPersonRepository.countPendingVerification();
    }

    @Transactional(readOnly = true)
    public Long countActiveAndVerifiedDriverPersons() {
        return driverPersonRepository.countByIsAvailableAndIsVerified(true, true);
    }

    @Transactional(readOnly = true)
    public Long countExperiencedDriverPersons() {
        return driverPersonRepository.countByTotalDeliveriesGreaterThanEqual(100L);
    }

    @Transactional(readOnly = true)
    public Long countDriversWithMultipleDeliveries() {
        return driverPersonRepository.countByTotalDeliveriesGreaterThan(1L);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDriverPersonStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", driverPersonRepository.count());
        stats.put("active", countActiveDriverPersons());
        stats.put("verified", countVerifiedDriverPersons());
        stats.put("available", countAvailableDriverPersons());
        stats.put("activeAndVerified", countActiveAndVerifiedDriverPersons());
        stats.put("experienced", countExperiencedDriverPersons());
        stats.put("withMultipleDeliveries", countDriversWithMultipleDeliveries());
        return stats;
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageRating() {
        return driverPersonRepository.getAverageRating();
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageEarnings() {
        return driverPersonRepository.getAverageEarnings();
    }

    @Transactional(readOnly = true)
    public Long getTotalDeliveries() {
        return driverPersonRepository.getTotalDeliveries();
    }

    @Transactional(readOnly = true)
    public List<DriverPerson> getDriverPersonsByVehicleTypeString(String vehicleType) {
        try {
            DriverPerson.VehicleType vehicleTypeEnum = DriverPerson.VehicleType.valueOf(vehicleType.toUpperCase());
            Specification<DriverPerson> spec = DriverPersonSpecifications.hasVehicleType(vehicleTypeEnum);
            return driverPersonRepository.findAll(spec);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid vehicle type: " + vehicleType);
        }
    }

    @Transactional(readOnly = true)
    public Page<DriverPerson> where(Specification<DriverPerson> spec, Pageable pageable) {
        return driverPersonRepository.findAll(spec, pageable);
    }
}