package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.VendorCompany;
import com.swiftdeliver.backend.entity.VendorOwner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorCompanyRepository extends JpaRepository<VendorCompany, Long>, JpaSpecificationExecutor<VendorCompany> {
    
    Optional<VendorCompany> findByCompanyName(String companyName);
    Optional<VendorCompany> findByBusinessLicense(String businessLicense);
    Optional<VendorCompany> findByContactEmail(String contactEmail);
    Optional<VendorCompany> findByContactPhone(String contactPhone);
    
    List<VendorCompany> findByOwner(VendorOwner owner);
    List<VendorCompany> findByIsActive(Boolean isActive);
    List<VendorCompany> findByIsVerified(Boolean isVerified);
    
    @Query("SELECT v FROM VendorCompany v WHERE v.isActive = true AND v.isVerified = true")
    List<VendorCompany> findAllActiveVerifiedCompanies();
    
    @Query("SELECT v FROM VendorCompany v WHERE v.isActive = true AND v.isVerified = true")
    Page<VendorCompany> findAllActiveVerifiedCompanies(Pageable pageable);
    
    @Query("SELECT v FROM VendorCompany v WHERE v.rating >= :minRating AND v.isActive = true ORDER BY v.rating DESC")
    List<VendorCompany> findTopRatedCompanies(@Param("minRating") Double minRating);
    
    @Query("SELECT v FROM VendorCompany v WHERE v.totalOrders >= :minOrders AND v.isActive = true ORDER BY v.totalOrders DESC")
    List<VendorCompany> findHighVolumeCompanies(@Param("minOrders") Long minOrders);
    
    @Query("SELECT v FROM VendorCompany v WHERE v.totalRevenue >= :minRevenue AND v.isActive = true ORDER BY v.totalRevenue DESC")
    List<VendorCompany> findHighRevenueCompanies(@Param("minRevenue") BigDecimal minRevenue);
    
    @Query("SELECT v FROM VendorCompany v WHERE v.businessAddress LIKE %:location% AND v.isActive = true")
    List<VendorCompany> findCompaniesByLocation(@Param("location") String location);
    
    @Query("SELECT v FROM VendorCompany v WHERE v.companyName LIKE %:name% AND v.isActive = true")
    List<VendorCompany> findCompaniesByName(@Param("name") String name);
    
    @Query("SELECT COUNT(v) FROM VendorCompany v WHERE v.isActive = true")
    Long countActiveCompanies();
    
    @Query("SELECT COUNT(v) FROM VendorCompany v WHERE v.isVerified = true")
    Long countVerifiedCompanies();
    
    @Query("SELECT AVG(v.rating) FROM VendorCompany v WHERE v.isActive = true")
    Double getAverageRating();
    
    @Query("SELECT SUM(v.totalRevenue) FROM VendorCompany v WHERE v.isActive = true")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT SUM(v.totalOrders) FROM VendorCompany v WHERE v.isActive = true")
    Long getTotalOrders();
    
    @Query("SELECT SUM(v.totalRevenue) FROM VendorCompany v WHERE v.isActive = true")
    BigDecimal findTotalRevenue();
    
    @Query("SELECT SUM(v.totalOrders) FROM VendorCompany v WHERE v.isActive = true")
    Long findTotalOrders();
    
    @Query("SELECT v FROM VendorCompany v WHERE v.owner.id = :ownerId")
    List<VendorCompany> findByOwnerId(@Param("ownerId") Long ownerId);
    
    @Query("SELECT v FROM VendorCompany v WHERE v.owner.id = :ownerId")
    Page<VendorCompany> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
    
    // Existence check methods
    boolean existsByCompanyName(String companyName);
    boolean existsByBusinessLicense(String businessLicense);
    boolean existsByContactEmail(String contactEmail);
    
    // Existence check methods with ID exclusion
    boolean existsByCompanyNameAndIdNot(String companyName, Long id);
    boolean existsByBusinessLicenseAndIdNot(String businessLicense, Long id);
    boolean existsByContactEmailAndIdNot(String contactEmail, Long id);

    long countByRegistrationDateGreaterThanEqual(LocalDateTime since);
}