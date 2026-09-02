package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.DriverPerson;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverPersonRepository extends JpaRepository<DriverPerson, Long>, JpaSpecificationExecutor<DriverPerson> {
    
    Optional<DriverPerson> findByUsername(String username);
    
    Optional<DriverPerson> findByEmail(String email);
    
    Optional<DriverPerson> findByUsernameOrEmail(String username, String email);
    
    Optional<DriverPerson> findByLicenseNumber(String licenseNumber);
    
    Optional<DriverPerson> findByPhoneNumber(String phoneNumber);
    
    Optional<DriverPerson> findByVehiclePlate(String vehiclePlate);
    
    // Statistical queries - keeping these as they are used for analytics
    @Query("SELECT COUNT(d) FROM DriverPerson d WHERE d.isAvailable = true AND d.isVerified = true")
    Long countAvailableDriverPersons();
    
    @Query("SELECT COUNT(d) FROM DriverPerson d WHERE d.isVerified = true")
    Long countVerifiedDriverPersons();
    
    @Query("SELECT COUNT(d) FROM DriverPerson d WHERE d.isVerified = false")
    Long countPendingVerification();
    
    @Query("SELECT AVG(d.rating) FROM DriverPerson d WHERE d.isVerified = true")
    BigDecimal getAverageRating();
    
    @Query("SELECT SUM(d.totalDeliveries) FROM DriverPerson d WHERE d.isVerified = true")
    Long getTotalDeliveries();
    
    @Query("SELECT d FROM DriverPerson d WHERE d.deliveryZone IN :zones AND d.isAvailable = true AND d.isVerified = true")
    List<DriverPerson> findAvailableInZones(@Param("zones") List<String> zones);
    
    @Query("SELECT d FROM DriverPerson d WHERE d.currentLocation LIKE %:location% AND d.isAvailable = true")
    List<DriverPerson> findByCurrentLocation(@Param("location") String location);
    
    @Query("SELECT COUNT(d) FROM DriverPerson d WHERE d.isEnabled = true")
    Long countActiveDriverPersons();

    long countByIsAvailableAndIsVerified(Boolean isAvailable, Boolean isVerified);

    long countByTotalDeliveriesGreaterThanEqual(Long minDeliveries);

    long countByTotalDeliveriesGreaterThan(Long minDeliveries);
    
    @Query("SELECT AVG(d.totalEarnings) FROM DriverPerson d WHERE d.isVerified = true")
    BigDecimal getAverageEarnings();
    
    @Query("SELECT d FROM DriverPerson d WHERE d.isAvailable = true AND d.isVerified = true")
    java.util.List<DriverPerson> findAvailableVerifiedDriverPersons();
    
    @Query("SELECT d FROM DriverPerson d WHERE d.deliveryZone = :zone AND d.isAvailable = true AND d.isVerified = true")
    java.util.List<DriverPerson> findAvailableInZone(@Param("zone") String zone);

    // Derived queries by delivery company (replaces entity back-collection)
    List<DriverPerson> findByDeliveryCompanyId(Long deliveryCompanyId);

    List<DriverPerson> findByDeliveryCompanyIdAndIsAvailable(Long deliveryCompanyId, Boolean isAvailable);

    long countByDeliveryCompanyId(Long deliveryCompanyId);

    long countByDeliveryCompanyIdAndIsAvailable(Long deliveryCompanyId, Boolean isAvailable);
}