package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.VendorOwner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendorOwnerRepository extends JpaRepository<VendorOwner, Long>, JpaSpecificationExecutor<VendorOwner> {
    

    Optional<VendorOwner> findByPhoneNumber(String phoneNumber);
    Optional<VendorOwner> findByNationalId(String nationalId);
    
    List<VendorOwner> findByIsEnabled(Boolean isEnabled);
    List<VendorOwner> findByIsVerifiedOwner(Boolean isVerified);
    List<VendorOwner> findByPreferredBusinessCategory(String category);
    
    @Query("SELECT v FROM VendorOwner v WHERE v.isEnabled = true")
    List<VendorOwner> findAllActiveVendorOwners();
    
    @Query("SELECT v FROM VendorOwner v WHERE v.isEnabled = true")
    Page<VendorOwner> findAllActiveVendorOwners(Pageable pageable);
    
    @Query("SELECT v FROM VendorOwner v WHERE v.businessExperienceYears >= :minYears AND v.isEnabled = true ORDER BY v.businessExperienceYears DESC")
    List<VendorOwner> findExperiencedVendorOwners(@Param("minYears") Integer minYears);
    
    @Query("SELECT v FROM VendorOwner v WHERE v.preferredBusinessCategory = :category AND v.isEnabled = true")
    List<VendorOwner> findByBusinessCategory(@Param("category") String category);
    
    @Query("SELECT v FROM VendorOwner v WHERE v.address LIKE %:location% AND v.isEnabled = true")
    List<VendorOwner> findByLocation(@Param("location") String location);
    
    @Query("SELECT COUNT(v) FROM VendorOwner v WHERE v.isVerifiedOwner = true")
    Long countVerifiedVendorOwners();
    
    @Query("SELECT COUNT(v) FROM VendorOwner v WHERE v.isEnabled = true")
    Long countActiveVendorOwners();
    
    @Query("SELECT AVG(v.businessExperienceYears) FROM VendorOwner v WHERE v.isEnabled = true")
    Double getAverageExperience();
    
    @Query("SELECT v FROM VendorOwner v WHERE v.firstName LIKE %:name% OR v.lastName LIKE %:name% OR v.username LIKE %:name%")
    List<VendorOwner> findVendorOwnersByName(@Param("name") String name);
    
    // Existence check methods
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
    
    // Existence check methods excluding specific ID
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByNationalIdAndIdNot(String nationalId, Long id);
}