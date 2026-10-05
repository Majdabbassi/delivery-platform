package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.*;
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

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    
    // Basic finders
    Optional<Order> findByOrderNumber(String orderNumber);
    
    // Find by companies
    List<Order> findByVendorCompany(VendorCompany vendorCompany);
    List<Order> findByDeliveryCompany(DeliveryCompany deliveryCompany);
    Page<Order> findByVendorCompany(VendorCompany vendorCompany, Pageable pageable);
    Page<Order> findByDeliveryCompany(DeliveryCompany deliveryCompany, Pageable pageable);
    
    // Find by customer and driver
    List<Order> findByCustomerUser(CustomerUser customerUser);
    List<Order> findByDriverPerson(DriverPerson driverPerson);
    Page<Order> findByCustomerUser(CustomerUser customerUser, Pageable pageable);
    Page<Order> findByDriverPerson(DriverPerson driverPerson, Pageable pageable);
    List<Order> findByCreatedByUserId(Long userId);
    
    // Find by partnership
    List<Order> findByPartnership(Partnership partnership);
    Page<Order> findByPartnership(Partnership partnership, Pageable pageable);

    // Find by company owner (tenant scoping)
    List<Order> findByVendorCompanyOwner(VendorOwner owner);
    List<Order> findByDeliveryCompanyOwner(DeliveryOwner owner);
    List<Order> findByVendorCompanyOwnerAndStatus(VendorOwner owner, Order.OrderStatus status);
    List<Order> findByDeliveryCompanyOwnerAndStatus(DeliveryOwner owner, Order.OrderStatus status);
    
    // Find by status
    List<Order> findByStatus(Order.OrderStatus status);
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    
    // Find by priority
    List<Order> findByPriority(Order.OrderPriority priority);
    Page<Order> findByPriority(Order.OrderPriority priority, Pageable pageable);
    
    // Find by date ranges
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    List<Order> findByScheduledPickupTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<Order> findByScheduledDeliveryTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // Find by amount ranges
    List<Order> findByOrderAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
    List<Order> findByTotalAmountGreaterThanEqual(BigDecimal amount);
    
    // Complex queries
    @Query("SELECT o FROM Order o WHERE o.vendorCompany = :vendorCompany AND o.status = :status")
    List<Order> findByVendorCompanyAndStatus(@Param("vendorCompany") VendorCompany vendorCompany, 
                                           @Param("status") Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.deliveryCompany = :deliveryCompany AND o.status = :status")
    List<Order> findByDeliveryCompanyAndStatus(@Param("deliveryCompany") DeliveryCompany deliveryCompany, 
                                             @Param("status") Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.partnership = :partnership AND o.status = :status")
    List<Order> findByPartnershipAndStatus(@Param("partnership") Partnership partnership, 
                                         @Param("status") Order.OrderStatus status);
    
    // Count queries
    long countByVendorCompany(VendorCompany vendorCompany);
    long countByDeliveryCompany(DeliveryCompany deliveryCompany);
    long countByPartnership(Partnership partnership);
    long countByStatus(Order.OrderStatus status);
    long countByPriority(Order.OrderPriority priority);

    // Derived counts by company id (replaces entity back-collection)
    long countByDeliveryCompanyId(Long deliveryCompanyId);

    long countByDeliveryCompanyIdAndStatus(Long deliveryCompanyId, Order.OrderStatus status);

    long countByDeliveryCompanyIdAndStatusIn(Long deliveryCompanyId, java.util.Collection<Order.OrderStatus> statuses);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.vendorCompany = :vendorCompany AND o.status = :status")
    long countByVendorCompanyAndStatus(@Param("vendorCompany") VendorCompany vendorCompany, 
                                     @Param("status") Order.OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryCompany = :deliveryCompany AND o.status = :status")
    long countByDeliveryCompanyAndStatus(@Param("deliveryCompany") DeliveryCompany deliveryCompany, 
                                       @Param("status") Order.OrderStatus status);
    
    // Revenue calculations
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.vendorCompany = :vendorCompany AND o.status = 'DELIVERED'")
    BigDecimal calculateTotalRevenueByVendorCompany(@Param("vendorCompany") VendorCompany vendorCompany);
    
    @Query("SELECT SUM(o.deliveryFee) FROM Order o WHERE o.deliveryCompany = :deliveryCompany AND o.status = 'DELIVERED'")
    BigDecimal calculateTotalDeliveryRevenueByDeliveryCompany(@Param("deliveryCompany") DeliveryCompany deliveryCompany);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.partnership = :partnership AND o.status = 'DELIVERED'")
    BigDecimal calculateTotalRevenueByPartnership(@Param("partnership") Partnership partnership);
    
    // Special status queries
    @Query("SELECT o FROM Order o WHERE o.estimatedDeliveryTime < :currentTime AND o.status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED')")
    List<Order> findOverdueOrders(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.deliveryCompany IS NULL")
    List<Order> findPendingUnassignedOrders();
    
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.partnership IS NULL")
    List<Order> findPendingOrdersWithoutPartnership();
    
    @Query("SELECT o FROM Order o WHERE o.status IN ('DELIVERED') AND o.createdAt >= :startDate")
    List<Order> findCompletedOrdersSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT o FROM Order o WHERE o.priority = 'URGENT' AND o.status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED')")
    List<Order> findActiveUrgentOrders();
    
    // Geographic queries
    @Query("SELECT o FROM Order o WHERE o.pickupLatitude BETWEEN :minLat AND :maxLat AND o.pickupLongitude BETWEEN :minLng AND :maxLng")
    List<Order> findOrdersInPickupArea(@Param("minLat") Double minLatitude, 
                                     @Param("maxLat") Double maxLatitude,
                                     @Param("minLng") Double minLongitude, 
                                     @Param("maxLng") Double maxLongitude);
    
    @Query("SELECT o FROM Order o WHERE o.deliveryLatitude BETWEEN :minLat AND :maxLat AND o.deliveryLongitude BETWEEN :minLng AND :maxLng")
    List<Order> findOrdersInDeliveryArea(@Param("minLat") Double minLatitude, 
                                       @Param("maxLat") Double maxLatitude,
                                       @Param("minLng") Double minLongitude, 
                                       @Param("maxLng") Double maxLongitude);
    
    // Rating and review queries
    @Query("SELECT AVG(o.rating) FROM Order o WHERE o.deliveryCompany = :deliveryCompany AND o.rating IS NOT NULL")
    Double calculateAverageRatingByDeliveryCompany(@Param("deliveryCompany") DeliveryCompany deliveryCompany);
    
    @Query("SELECT AVG(o.rating) FROM Order o WHERE o.partnership = :partnership AND o.rating IS NOT NULL")
    Double calculateAverageRatingByPartnership(@Param("partnership") Partnership partnership);
    
    // Tracking queries
    Optional<Order> findByTrackingNumber(String trackingNumber);
    
    // Existence checks
    boolean existsByOrderNumber(String orderNumber);
    boolean existsByTrackingNumber(String trackingNumber);
    
    // Recent orders
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(@Param("since") LocalDateTime since);
    
    @Query("SELECT o FROM Order o WHERE o.vendorCompany = :vendorCompany AND o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByVendorCompany(@Param("vendorCompany") VendorCompany vendorCompany, 
                                              @Param("since") LocalDateTime since);
    
    // Additional methods for OrderPoolService
    @Query("SELECT o FROM Order o WHERE o.id IN :orderIds AND o.status = :status")
    Page<Order> findByIdInAndStatus(@Param("orderIds") java.util.Set<Long> orderIds, 
                                   @Param("status") Order.OrderStatus status, 
                                   Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.id IN :orderIds AND o.status = :status")
    List<Order> findByIdInAndStatus(@Param("orderIds") java.util.Set<Long> orderIds, 
                                   @Param("status") Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.status = 'OPEN_FOR_BID' AND o.estimatedDeliveryTime < :currentTime")
    List<Order> findExpiredOpenForBidOrders(@Param("currentTime") LocalDateTime currentTime);
    
    // Additional count methods for OrderAssignmentService
    @Query("SELECT COUNT(o) FROM Order o WHERE o.partnership IS NOT NULL")
    long countByPartnershipIsNotNull();
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.partnership IS NULL AND o.deliveryCompany IS NOT NULL")
    long countByPartnershipIsNullAndDeliveryCompanyIsNotNull();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal getTotalCompletedRevenue();
}