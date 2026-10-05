package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.DeliveryOwner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryOwnerRepository extends JpaRepository<DeliveryOwner, Long>, JpaSpecificationExecutor<DeliveryOwner> {
    
    Optional<DeliveryOwner> findByUsername(String username);
    Optional<DeliveryOwner> findByEmail(String email);
    Optional<DeliveryOwner> findByUsernameOrEmail(String username, String email);
    Optional<DeliveryOwner> findByPhoneNumber(String phoneNumber);
    Optional<DeliveryOwner> findByNationalId(String nationalId);
    Optional<DeliveryOwner> findByTransportLicenseNumber(String licenseNumber);
    
    List<DeliveryOwner> findByIsEnabled(Boolean isEnabled);
    List<DeliveryOwner> findByIsVerifiedOwner(Boolean isVerified);
    
    @Query("SELECT d FROM DeliveryOwner d WHERE d.isEnabled = true")
    List<DeliveryOwner> findAllActiveDeliveryOwners();
    
    @Query("SELECT d FROM DeliveryOwner d WHERE d.isEnabled = true")
    Page<DeliveryOwner> findAllActiveDeliveryOwners(Pageable pageable);
    
    @Query("SELECT d FROM DeliveryOwner d WHERE d.logisticsExperienceYears >= :minYears AND d.isEnabled = true ORDER BY d.logisticsExperienceYears DESC")
    List<DeliveryOwner> findExperiencedDeliveryOwners(@Param("minYears") Integer minYears);
    
    @Query("SELECT d FROM DeliveryOwner d WHERE d.preferredServiceRegions LIKE %:region% AND d.isEnabled = true")
    List<DeliveryOwner> findByServiceRegion(@Param("region") String region);
    
    @Query("SELECT d FROM DeliveryOwner d WHERE d.address LIKE %:location% AND d.isEnabled = true")
    List<DeliveryOwner> findByLocation(@Param("location") String location);
    
    @Query("SELECT COUNT(d) FROM DeliveryOwner d WHERE d.isVerifiedOwner = true")
    Long countVerifiedDeliveryOwners();
    
    @Query("SELECT COUNT(d) FROM DeliveryOwner d WHERE d.isEnabled = true")
    Long countActiveDeliveryOwners();
    
    @Query("SELECT AVG(d.logisticsExperienceYears) FROM DeliveryOwner d WHERE d.isEnabled = true")
    Double getAverageExperience();
    
    @Query("SELECT d FROM DeliveryOwner d WHERE d.firstName LIKE %:name% OR d.lastName LIKE %:name% OR d.username LIKE %:name%")
    List<DeliveryOwner> findDeliveryOwnersByName(@Param("name") String name);
    
    @Query("SELECT d FROM DeliveryOwner d WHERE (SELECT COUNT(c) FROM DeliveryCompany c WHERE c.owner = d) < d.maxCompaniesAllowed AND d.isEnabled = true")
    List<DeliveryOwner> findOwnersWithAvailableSlots();
    
    // Existence check methods
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
    
    // Existence check methods excluding specific ID
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByNationalIdAndIdNot(String nationalId, Long id);
}