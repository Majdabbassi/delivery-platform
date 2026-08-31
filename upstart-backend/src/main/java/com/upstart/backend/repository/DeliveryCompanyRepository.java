package com.upstart.backend.repository;

import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.DeliveryOwner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryCompanyRepository extends JpaRepository<DeliveryCompany, Long>, JpaSpecificationExecutor<DeliveryCompany> {
    
    Optional<DeliveryCompany> findByCompanyName(String companyName);
    Optional<DeliveryCompany> findByOperatingLicense(String operatingLicense);
    Optional<DeliveryCompany> findByContactEmail(String contactEmail);
    Optional<DeliveryCompany> findByContactPhone(String contactPhone);
    
    // Additional finder methods for address
    List<DeliveryCompany> findByCompanyAddress(String companyAddress);
    
    List<DeliveryCompany> findByOwner(DeliveryOwner deliveryOwner);
    List<DeliveryCompany> findByIsActive(Boolean isActive);
    List<DeliveryCompany> findByIsLicensed(Boolean isLicensed);
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.isActive = true AND d.isLicensed = true")
    List<DeliveryCompany> findAllActiveLicensedCompanies();
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.isActive = true AND d.isLicensed = true")
    Page<DeliveryCompany> findAllActiveLicensedCompanies(Pageable pageable);
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.serviceRegion LIKE %:region% AND d.isActive = true")
    List<DeliveryCompany> findCompaniesByServiceRegion(@Param("region") String region);
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.managedZones LIKE %:zone% AND d.isActive = true")
    List<DeliveryCompany> findCompaniesByZone(@Param("zone") String zone);
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.totalDeliveriesManaged >= :minDeliveries AND d.isActive = true ORDER BY d.totalDeliveriesManaged DESC")
    List<DeliveryCompany> findHighVolumeCompanies(@Param("minDeliveries") Long minDeliveries);
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.totalRevenue >= :minRevenue AND d.isActive = true ORDER BY d.totalRevenue DESC")
    List<DeliveryCompany> findHighRevenueCompanies(@Param("minRevenue") BigDecimal minRevenue);
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.activeDriversCount < d.maxDrivers AND d.isActive = true")
    List<DeliveryCompany> findCompaniesWithAvailableDriverSlots();
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.companyName LIKE %:name% AND d.isActive = true")
    List<DeliveryCompany> findCompaniesByName(@Param("name") String name);
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.companyAddress LIKE %:location% AND d.isActive = true")
    List<DeliveryCompany> findCompaniesByLocation(@Param("location") String location);
    
    @Query("SELECT COUNT(d) FROM DeliveryCompany d WHERE d.isActive = true")
    Long countActiveCompanies();
    
    @Query("SELECT COUNT(d) FROM DeliveryCompany d WHERE d.isLicensed = true")
    Long countLicensedCompanies();
    
    @Query("SELECT SUM(d.activeDriversCount) FROM DeliveryCompany d WHERE d.isActive = true")
    Long getTotalActiveDrivers();
    
    @Query("SELECT SUM(d.totalDeliveriesManaged) FROM DeliveryCompany d WHERE d.isActive = true")
    Long getTotalDeliveriesManaged();
    
    @Query("SELECT SUM(d.totalRevenue) FROM DeliveryCompany d WHERE d.isActive = true")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT AVG(d.commissionRate) FROM DeliveryCompany d WHERE d.isActive = true")
    Double getAverageCommissionRate();
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.owner.id = :ownerId")
    List<DeliveryCompany> findByOwnerId(@Param("ownerId") Long ownerId);
    
    @Query("SELECT d FROM DeliveryCompany d WHERE d.owner.id = :ownerId")
    Page<DeliveryCompany> findByOwnerIdPageable(@Param("ownerId") Long ownerId, Pageable pageable);
    
    // Existence check methods
    boolean existsByCompanyName(String companyName);
    boolean existsByOperatingLicense(String operatingLicense);
    boolean existsByContactEmail(String contactEmail);
    
    // Statistical methods for service layer
    @Query("SELECT AVG(d.rating) FROM DeliveryCompany d WHERE d.isActive = true")
    BigDecimal findAverageRating();
    
    @Query("SELECT SUM(d.totalRevenue) FROM DeliveryCompany d WHERE d.isActive = true")
    BigDecimal findTotalRevenue();
    
    @Query("SELECT SUM(d.totalDeliveriesManaged) FROM DeliveryCompany d WHERE d.isActive = true")
    Long findTotalDeliveries();
    
    @Query("SELECT SUM(d.activeDriversCount) FROM DeliveryCompany d WHERE d.isActive = true")
    Long findTotalActiveDrivers();

    long countByRatingGreaterThanEqual(BigDecimal rating);

    @Query("SELECT COUNT(d) FROM DeliveryCompany d WHERE d.vehicleTypesSupported IS NOT NULL AND d.vehicleTypesSupported LIKE '%,%'")
    long countWithMultipleVehicleTypes();

    @Query("SELECT COUNT(DISTINCT d.owner.id) FROM DeliveryCompany d WHERE d.owner IS NOT NULL AND d.rating IS NOT NULL AND d.rating >= :minRating")
    long countOwnersWithHighRatedCompany(@Param("minRating") BigDecimal minRating);
}