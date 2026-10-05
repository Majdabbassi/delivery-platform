package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.Partnership;
import com.swiftdeliver.backend.entity.VendorCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartnershipRepository extends JpaRepository<Partnership, Long>, JpaSpecificationExecutor<Partnership> {
    
    // Basic finders
    List<Partnership> findByVendorCompany(VendorCompany vendorCompany);
    List<Partnership> findByDeliveryCompany(DeliveryCompany deliveryCompany);
    Page<Partnership> findByVendorCompany(VendorCompany vendorCompany, Pageable pageable);
    Page<Partnership> findByDeliveryCompany(DeliveryCompany deliveryCompany, Pageable pageable);
    
    // Find by status
    List<Partnership> findByStatus(Partnership.PartnershipStatus status);
    Page<Partnership> findByStatus(Partnership.PartnershipStatus status, Pageable pageable);
    
    // Find by vendor and delivery company combination
    Optional<Partnership> findByVendorCompanyAndDeliveryCompany(VendorCompany vendorCompany, DeliveryCompany deliveryCompany);
    
    // Find by vendor company and status
    List<Partnership> findByVendorCompanyAndStatus(VendorCompany vendorCompany, Partnership.PartnershipStatus status);
    Page<Partnership> findByVendorCompanyAndStatus(VendorCompany vendorCompany, Partnership.PartnershipStatus status, Pageable pageable);
    
    // Find by vendor company, exclusive flag and status
    List<Partnership> findByVendorCompanyAndIsExclusiveAndStatus(VendorCompany vendorCompany, Boolean isExclusive, Partnership.PartnershipStatus status);
    
    // Find by delivery company and status
    List<Partnership> findByDeliveryCompanyAndStatus(DeliveryCompany deliveryCompany, Partnership.PartnershipStatus status);
    Page<Partnership> findByDeliveryCompanyAndStatus(DeliveryCompany deliveryCompany, Partnership.PartnershipStatus status, Pageable pageable);
    
    // Find active partnerships
    @Query("SELECT p FROM Partnership p WHERE p.status = 'ACTIVE'")
    List<Partnership> findAllActivePartnerships();
    
    @Query("SELECT p FROM Partnership p WHERE p.status = 'ACTIVE'")
    Page<Partnership> findAllActivePartnerships(Pageable pageable);
    
    // Find by contract dates
    List<Partnership> findByContractStartDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Partnership> findByContractEndDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT p FROM Partnership p WHERE p.contractEndDate < :currentDate AND p.status = 'ACTIVE'")
    List<Partnership> findExpiredActivePartnerships(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT p FROM Partnership p WHERE p.contractEndDate BETWEEN :startDate AND :endDate AND p.status = 'ACTIVE'")
    List<Partnership> findPartnershipsExpiringBetween(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    // Find by exclusivity
    List<Partnership> findByIsExclusive(Boolean isExclusive);
    
    @Query("SELECT p FROM Partnership p WHERE p.vendorCompany = :vendorCompany AND p.isExclusive = true AND p.status = 'ACTIVE'")
    Optional<Partnership> findExclusivePartnershipByVendorCompany(@Param("vendorCompany") VendorCompany vendorCompany);
    
    @Query("SELECT p FROM Partnership p WHERE p.deliveryCompany = :deliveryCompany AND p.isExclusive = true AND p.status = 'ACTIVE'")
    List<Partnership> findExclusivePartnershipsByDeliveryCompany(@Param("deliveryCompany") DeliveryCompany deliveryCompany);
    
    // Count queries
    long countByVendorCompany(VendorCompany vendorCompany);
    long countByDeliveryCompany(DeliveryCompany deliveryCompany);
    long countByStatus(Partnership.PartnershipStatus status);
    
    @Query("SELECT COUNT(p) FROM Partnership p WHERE p.vendorCompany = :vendorCompany AND p.status = :status")
    long countByVendorCompanyAndStatus(@Param("vendorCompany") VendorCompany vendorCompany, 
                                     @Param("status") Partnership.PartnershipStatus status);
    
    @Query("SELECT COUNT(p) FROM Partnership p WHERE p.deliveryCompany = :deliveryCompany AND p.status = :status")
    long countByDeliveryCompanyAndStatus(@Param("deliveryCompany") DeliveryCompany deliveryCompany, 
                                       @Param("status") Partnership.PartnershipStatus status);
    
    // Revenue and performance queries
    @Query("SELECT SUM(p.totalRevenueGenerated) FROM Partnership p WHERE p.vendorCompany = :vendorCompany")
    BigDecimal calculateTotalRevenueByVendorCompany(@Param("vendorCompany") VendorCompany vendorCompany);
    
    @Query("SELECT SUM(p.totalRevenueGenerated) FROM Partnership p WHERE p.deliveryCompany = :deliveryCompany")
    BigDecimal calculateTotalRevenueByDeliveryCompany(@Param("deliveryCompany") DeliveryCompany deliveryCompany);
    
    @Query("SELECT AVG(p.averageRating) FROM Partnership p WHERE p.vendorCompany = :vendorCompany AND p.averageRating IS NOT NULL")
    Double calculateAverageRatingByVendorCompany(@Param("vendorCompany") VendorCompany vendorCompany);
    
    @Query("SELECT AVG(p.averageRating) FROM Partnership p WHERE p.deliveryCompany = :deliveryCompany AND p.averageRating IS NOT NULL")
    Double calculateAverageRatingByDeliveryCompany(@Param("deliveryCompany") DeliveryCompany deliveryCompany);
    
    @Query("SELECT SUM(p.totalOrdersCompleted) FROM Partnership p WHERE p.vendorCompany = :vendorCompany")
    Long calculateTotalOrdersByVendorCompany(@Param("vendorCompany") VendorCompany vendorCompany);
    
    @Query("SELECT SUM(p.totalOrdersCompleted) FROM Partnership p WHERE p.deliveryCompany = :deliveryCompany")
    Long calculateTotalOrdersByDeliveryCompany(@Param("deliveryCompany") DeliveryCompany deliveryCompany);
    
    // Service area queries
    @Query("SELECT p FROM Partnership p WHERE :serviceArea MEMBER OF p.serviceAreas AND p.status = 'ACTIVE'")
    List<Partnership> findByServiceArea(@Param("serviceArea") String serviceArea);
    
    @Query("SELECT p FROM Partnership p WHERE :serviceArea MEMBER OF p.serviceAreas AND p.status = 'ACTIVE' AND p.vendorCompany = :vendorCompany")
    List<Partnership> findByVendorCompanyAndServiceArea(@Param("vendorCompany") VendorCompany vendorCompany, 
                                                       @Param("serviceArea") String serviceArea);
    
    @Query("SELECT p FROM Partnership p WHERE :serviceArea MEMBER OF p.serviceAreas AND p.status = 'ACTIVE' AND p.deliveryCompany = :deliveryCompany")
    List<Partnership> findByDeliveryCompanyAndServiceArea(@Param("deliveryCompany") DeliveryCompany deliveryCompany, 
                                                         @Param("serviceArea") String serviceArea);
    
    // Minimum order value queries
    @Query("SELECT p FROM Partnership p WHERE p.minimumOrderValue <= :orderValue AND p.status = 'ACTIVE'")
    List<Partnership> findByMinimumOrderValueLessThanEqual(@Param("orderValue") BigDecimal orderValue);
    
    @Query("SELECT p FROM Partnership p WHERE p.vendorCompany = :vendorCompany AND p.minimumOrderValue <= :orderValue AND p.status = 'ACTIVE'")
    List<Partnership> findByVendorCompanyAndMinimumOrderValue(@Param("vendorCompany") VendorCompany vendorCompany, 
                                                             @Param("orderValue") BigDecimal orderValue);
    
    // Commission rate queries
    List<Partnership> findByCommissionRateBetween(BigDecimal minRate, BigDecimal maxRate);
    
    @Query("SELECT p FROM Partnership p WHERE p.commissionRate <= :maxRate AND p.status = 'ACTIVE' ORDER BY p.commissionRate ASC")
    List<Partnership> findByCommissionRateLessThanEqualOrderByCommissionRateAsc(@Param("maxRate") BigDecimal maxRate);
    
    // Distance queries
    @Query("SELECT p FROM Partnership p WHERE p.maximumDeliveryDistanceKm >= :distance AND p.status = 'ACTIVE'")
    List<Partnership> findByMaximumDeliveryDistanceGreaterThanEqual(@Param("distance") Double distance);
    
    // Performance-based queries
    @Query("SELECT p FROM Partnership p WHERE p.totalOrdersCompleted >= :minOrders AND p.status = 'ACTIVE' ORDER BY p.totalOrdersCompleted DESC")
    List<Partnership> findHighPerformingPartnerships(@Param("minOrders") Long minOrders);
    
    @Query("SELECT p FROM Partnership p WHERE p.averageRating >= :minRating AND p.status = 'ACTIVE' ORDER BY p.averageRating DESC")
    List<Partnership> findHighRatedPartnerships(@Param("minRating") BigDecimal minRating);
    
    // Recent partnerships
    @Query("SELECT p FROM Partnership p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Partnership> findRecentPartnerships(@Param("since") LocalDateTime since);
    
    @Query("SELECT p FROM Partnership p WHERE p.activatedAt >= :since AND p.status = 'ACTIVE' ORDER BY p.activatedAt DESC")
    List<Partnership> findRecentlyActivatedPartnerships(@Param("since") LocalDateTime since);
    
    // Existence checks
    boolean existsByVendorCompanyAndDeliveryCompany(VendorCompany vendorCompany, DeliveryCompany deliveryCompany);
    
    boolean existsByVendorCompanyAndDeliveryCompanyAndStatus(VendorCompany vendorCompany, DeliveryCompany deliveryCompany, Partnership.PartnershipStatus status);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Partnership p WHERE p.vendorCompany = :vendorCompany AND p.deliveryCompany = :deliveryCompany AND p.status = 'ACTIVE'")
    boolean existsActivePartnershipBetween(@Param("vendorCompany") VendorCompany vendorCompany, 
                                         @Param("deliveryCompany") DeliveryCompany deliveryCompany);
    
    // Complex filtering for order assignment
    @Query("SELECT p FROM Partnership p WHERE p.vendorCompany = :vendorCompany AND p.status = 'ACTIVE' " +
           "AND (:serviceArea IS NULL OR :serviceArea MEMBER OF p.serviceAreas) " +
           "AND (:orderValue IS NULL OR p.minimumOrderValue IS NULL OR p.minimumOrderValue <= :orderValue) " +
           "AND (:distance IS NULL OR p.maximumDeliveryDistanceKm IS NULL OR p.maximumDeliveryDistanceKm >= :distance)")
    List<Partnership> findEligiblePartnershipsForOrder(@Param("vendorCompany") VendorCompany vendorCompany,
                                                      @Param("serviceArea") String serviceArea,
                                                      @Param("orderValue") BigDecimal orderValue,
                                                      @Param("distance") Double distance);
}