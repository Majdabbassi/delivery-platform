package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.CustomerUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerUserRepository extends JpaRepository<CustomerUser, Long>, JpaSpecificationExecutor<CustomerUser> {
    
    Optional<CustomerUser> findByUsername(String username);
    
    Optional<CustomerUser> findByEmail(String email);
    
    Optional<CustomerUser> findByUsernameOrEmail(String username, String email);
    
    Optional<CustomerUser> findByPhoneNumber(String phoneNumber);
    
    List<CustomerUser> findByIsPremium(Boolean isPremium);
    
    @Query("SELECT c FROM CustomerUser c WHERE c.isEnabled = true")
    List<CustomerUser> findAllActiveCustomerUsers();

    @Query("SELECT c FROM CustomerUser c WHERE c.isEnabled = true")
    Page<CustomerUser> findAllActiveCustomerUsers(Pageable pageable);
    
    @Query("SELECT c FROM CustomerUser c WHERE c.loyaltyPoints >= :minPoints AND c.isEnabled = true ORDER BY c.loyaltyPoints DESC")
    List<CustomerUser> findCustomerUsersByMinLoyaltyPoints(@Param("minPoints") Integer minPoints);
    
    @Query("SELECT c FROM CustomerUser c WHERE c.totalSpent >= :minAmount AND c.isEnabled = true ORDER BY c.totalSpent DESC")
    List<CustomerUser> findHighSpendingCustomerUsers(@Param("minAmount") BigDecimal minAmount);
    
    @Query("SELECT c FROM CustomerUser c WHERE c.totalOrders >= :minOrders AND c.isEnabled = true ORDER BY c.totalOrders DESC")
    List<CustomerUser> findFrequentCustomerUsers(@Param("minOrders") Long minOrders);
    
    @Query("SELECT c FROM CustomerUser c WHERE c.defaultAddress LIKE %:location% AND c.isEnabled = true")
    List<CustomerUser> findCustomerUsersByLocation(@Param("location") String location);
    
    @Query("SELECT c FROM CustomerUser c WHERE c.preferredPaymentMethod = :paymentMethod AND c.isEnabled = true")
    List<CustomerUser> findCustomerUsersByPaymentMethod(@Param("paymentMethod") String paymentMethod);
    
    @Query("SELECT COUNT(c) FROM CustomerUser c WHERE c.isPremium = true")
    Long countPremiumCustomerUsers();

    @Query("SELECT COUNT(c) FROM CustomerUser c WHERE c.isEnabled = true")
    Long countActiveCustomerUsers();

    long countByIsEnabled(Boolean isEnabled);

    long countByTotalOrdersGreaterThan(Long minOrders);

    @Query("SELECT AVG(c.totalSpent) FROM CustomerUser c WHERE c.isEnabled = true")
    BigDecimal getAverageSpending();

    @Query("SELECT SUM(c.loyaltyPoints) FROM CustomerUser c WHERE c.isEnabled = true")
    Long getTotalLoyaltyPoints();
    
    @Query("SELECT c FROM CustomerUser c ORDER BY c.totalSpent DESC")
    List<CustomerUser> findTopSpenders(Pageable pageable);

    @Query("SELECT c FROM CustomerUser c ORDER BY c.loyaltyPoints DESC")
    List<CustomerUser> findTopLoyaltyPointsHolders(Pageable pageable);

    @Query("SELECT c FROM CustomerUser c WHERE c.firstName LIKE %:name% OR c.lastName LIKE %:name% OR c.username LIKE %:name%")
    List<CustomerUser> findCustomerUsersByName(@Param("name") String name);
}