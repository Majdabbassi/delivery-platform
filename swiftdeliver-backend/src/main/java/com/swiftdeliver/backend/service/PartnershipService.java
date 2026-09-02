package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.Partnership;
import com.swiftdeliver.backend.entity.VendorCompany;
import com.swiftdeliver.backend.repository.PartnershipRepository;
import com.swiftdeliver.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PartnershipService {
    
    private final PartnershipRepository partnershipRepository;
    
    // Create operations
    public Partnership createPartnership(Partnership partnership) {
        log.info("Creating new partnership between vendor {} and delivery company {}", 
                partnership.getVendorCompany().getId(), partnership.getDeliveryCompany().getId());
        
        // Check if partnership already exists
        Optional<Partnership> existingPartnership = partnershipRepository
                .findByVendorCompanyAndDeliveryCompany(partnership.getVendorCompany(), partnership.getDeliveryCompany());
        
        if (existingPartnership.isPresent()) {
            throw new IllegalStateException("Partnership already exists between these companies");
        }
        
        // Set default values
        if (partnership.getStatus() == null) {
            partnership.setStatus(Partnership.PartnershipStatus.PENDING);
        }
        
        if (partnership.getIsExclusive() == null) {
            partnership.setIsExclusive(false);
        }
        
        if (partnership.getTotalOrdersCompleted() == null) {
            partnership.setTotalOrdersCompleted(0L);
        }
        
        if (partnership.getTotalRevenueGenerated() == null) {
            partnership.setTotalRevenueGenerated(BigDecimal.ZERO);
        }
        
        if (partnership.getTotalRatingsCount() == null) {
            partnership.setTotalRatingsCount(0L);
        }
        
        Partnership savedPartnership = partnershipRepository.save(partnership);
        log.info("Partnership created successfully with ID: {}", savedPartnership.getId());
        
        return savedPartnership;
    }
    
    // Read operations
    public Partnership getPartnershipById(Long id) {
        log.debug("Fetching partnership with ID: {}", id);
        return partnershipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partnership not found with ID: " + id));
    }
    
    public Optional<Partnership> getPartnershipByCompanies(VendorCompany vendorCompany, DeliveryCompany deliveryCompany) {
        log.debug("Fetching partnership between vendor {} and delivery company {}", 
                vendorCompany.getId(), deliveryCompany.getId());
        return partnershipRepository.findByVendorCompanyAndDeliveryCompany(vendorCompany, deliveryCompany);
    }
    
    public Page<Partnership> getAllPartnerships(Pageable pageable) {
        log.debug("Fetching all partnerships with pagination");
        return partnershipRepository.findAll(pageable);
    }
    
    public Page<Partnership> getPartnershipsWithSpecification(Specification<Partnership> specification, Pageable pageable) {
        log.debug("Fetching partnerships with custom specification");
        return partnershipRepository.findAll(specification, pageable);
    }
    
    // Update operations
    public Partnership updatePartnership(Long id, Partnership partnershipDetails) {
        log.info("Updating partnership with ID: {}", id);
        
        Partnership existingPartnership = getPartnershipById(id);
        
        // Update fields
        if (partnershipDetails.getCommissionRate() != null) {
            existingPartnership.setCommissionRate(partnershipDetails.getCommissionRate());
        }
        if (partnershipDetails.getServiceAreas() != null) {
            existingPartnership.setServiceAreas(partnershipDetails.getServiceAreas());
        }
        if (partnershipDetails.getMinimumOrderValue() != null) {
            existingPartnership.setMinimumOrderValue(partnershipDetails.getMinimumOrderValue());
        }
        if (partnershipDetails.getMaximumDeliveryDistanceKm() != null) {
            existingPartnership.setMaximumDeliveryDistanceKm(partnershipDetails.getMaximumDeliveryDistanceKm());
        }
        if (partnershipDetails.getEstimatedDeliveryTimeHours() != null) {
            existingPartnership.setEstimatedDeliveryTimeHours(partnershipDetails.getEstimatedDeliveryTimeHours());
        }
        if (partnershipDetails.getPartnershipTerms() != null) {
            existingPartnership.setPartnershipTerms(partnershipDetails.getPartnershipTerms());
        }
        if (partnershipDetails.getIsExclusive() != null) {
            existingPartnership.setIsExclusive(partnershipDetails.getIsExclusive());
        }
        if (partnershipDetails.getContractStartDate() != null) {
            existingPartnership.setContractStartDate(partnershipDetails.getContractStartDate());
        }
        if (partnershipDetails.getContractEndDate() != null) {
            existingPartnership.setContractEndDate(partnershipDetails.getContractEndDate());
        }
        if (partnershipDetails.getNotes() != null) {
            existingPartnership.setNotes(partnershipDetails.getNotes());
        }
        
        Partnership updatedPartnership = partnershipRepository.save(existingPartnership);
        log.info("Partnership updated successfully with ID: {}", updatedPartnership.getId());
        
        return updatedPartnership;
    }
    
    public Partnership updatePartnershipStatus(Long id, Partnership.PartnershipStatus status) {
        log.info("Updating partnership status for ID: {} to: {}", id, status);
        
        Partnership partnership = getPartnershipById(id);
        Partnership.PartnershipStatus oldStatus = partnership.getStatus();
        partnership.setStatus(status);
        
        // Set timestamps based on status changes
        LocalDateTime now = LocalDateTime.now();
        if (status == Partnership.PartnershipStatus.ACTIVE && oldStatus != Partnership.PartnershipStatus.ACTIVE) {
            partnership.setActivatedAt(now);
        } else if (status == Partnership.PartnershipStatus.TERMINATED) {
            partnership.setTerminatedAt(now);
        }
        
        Partnership updatedPartnership = partnershipRepository.save(partnership);
        log.info("Partnership status updated successfully for ID: {}", updatedPartnership.getId());
        
        return updatedPartnership;
    }
    
    public Partnership activatePartnership(Long id) {
        return updatePartnershipStatus(id, Partnership.PartnershipStatus.ACTIVE);
    }
    
    public Partnership suspendPartnership(Long id) {
        return updatePartnershipStatus(id, Partnership.PartnershipStatus.SUSPENDED);
    }
    
    public Partnership terminatePartnership(Long id) {
        return updatePartnershipStatus(id, Partnership.PartnershipStatus.TERMINATED);
    }
    
    public Partnership updatePerformanceMetrics(Long id, Long ordersCompleted, BigDecimal revenue, BigDecimal rating) {
        log.info("Updating performance metrics for partnership ID: {}", id);
        
        Partnership partnership = getPartnershipById(id);
        
        if (ordersCompleted != null) {
            partnership.setTotalOrdersCompleted(ordersCompleted);
        }
        
        if (revenue != null) {
            partnership.setTotalRevenueGenerated(revenue);
        }
        
        if (rating != null) {
            partnership.updateRating(rating);
        }
        
        Partnership updatedPartnership = partnershipRepository.save(partnership);
        log.info("Performance metrics updated successfully for partnership ID: {}", updatedPartnership.getId());
        
        return updatedPartnership;
    }
    
    public Partnership incrementOrderCount(Long id) {
        log.debug("Incrementing order count for partnership ID: {}", id);
        
        Partnership partnership = getPartnershipById(id);
        partnership.incrementOrderCount();
        
        return partnershipRepository.save(partnership);
    }
    
    public Partnership addRevenue(Long id, BigDecimal revenue) {
        log.debug("Adding revenue {} to partnership ID: {}", revenue, id);
        
        Partnership partnership = getPartnershipById(id);
        partnership.addRevenue(revenue);
        
        return partnershipRepository.save(partnership);
    }
    
    public Partnership addRating(Long id, BigDecimal rating) {
        log.debug("Adding rating {} to partnership ID: {}", rating, id);
        
        Partnership partnership = getPartnershipById(id);
        partnership.updateRating(rating);
        
        return partnershipRepository.save(partnership);
    }
    
    // Delete operations
    public void deletePartnership(Long id) {
        log.info("Deleting partnership with ID: {}", id);
        
        Partnership partnership = getPartnershipById(id);
        partnershipRepository.delete(partnership);
        
        log.info("Partnership deleted successfully with ID: {}", id);
    }
    
    // Query operations
    public List<Partnership> getPartnershipsByVendorCompany(VendorCompany vendorCompany) {
        log.debug("Fetching partnerships for vendor company: {}", vendorCompany.getId());
        return partnershipRepository.findByVendorCompany(vendorCompany);
    }
    
    public List<Partnership> getPartnershipsByDeliveryCompany(DeliveryCompany deliveryCompany) {
        log.debug("Fetching partnerships for delivery company: {}", deliveryCompany.getId());
        return partnershipRepository.findByDeliveryCompany(deliveryCompany);
    }
    
    public List<Partnership> getPartnershipsByStatus(Partnership.PartnershipStatus status) {
        log.debug("Fetching partnerships with status: {}", status);
        return partnershipRepository.findByStatus(status);
    }
    
    public List<Partnership> getActivePartnerships() {
        log.debug("Fetching all active partnerships");
        return partnershipRepository.findAllActivePartnerships();
    }
    
    public List<Partnership> getActivePartnershipsByVendorCompany(VendorCompany vendorCompany) {
        log.debug("Fetching active partnerships for vendor company: {}", vendorCompany.getId());
        return partnershipRepository.findByVendorCompanyAndStatus(vendorCompany, Partnership.PartnershipStatus.ACTIVE);
    }
    
    public List<Partnership> getActivePartnershipsByDeliveryCompany(DeliveryCompany deliveryCompany) {
        log.debug("Fetching active partnerships for delivery company: {}", deliveryCompany.getId());
        return partnershipRepository.findByDeliveryCompanyAndStatus(deliveryCompany, Partnership.PartnershipStatus.ACTIVE);
    }
    
    public Optional<Partnership> getExclusivePartnershipByVendorCompany(VendorCompany vendorCompany) {
        log.debug("Fetching exclusive partnership for vendor company: {}", vendorCompany.getId());
        return partnershipRepository.findExclusivePartnershipByVendorCompany(vendorCompany);
    }
    
    public List<Partnership> getExpiredActivePartnerships() {
        log.debug("Fetching expired active partnerships");
        return partnershipRepository.findExpiredActivePartnerships(LocalDateTime.now());
    }
    
    public List<Partnership> getPartnershipsExpiringWithin(int days) {
        log.debug("Fetching partnerships expiring within {} days", days);
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(days);
        return partnershipRepository.findPartnershipsExpiringBetween(startDate, endDate);
    }
    
    public List<Partnership> getPartnershipsByServiceArea(String serviceArea) {
        log.debug("Fetching partnerships for service area: {}", serviceArea);
        return partnershipRepository.findByServiceArea(serviceArea);
    }
    
    public List<Partnership> getEligiblePartnershipsForOrder(VendorCompany vendorCompany, String serviceArea, 
                                                            BigDecimal orderValue, Double distance) {
        log.debug("Fetching eligible partnerships for order from vendor: {}", vendorCompany.getId());
        return partnershipRepository.findEligiblePartnershipsForOrder(vendorCompany, serviceArea, orderValue, distance);
    }
    
    public List<Partnership> getHighPerformingPartnerships(Long minOrders) {
        log.debug("Fetching high performing partnerships with minimum {} orders", minOrders);
        return partnershipRepository.findHighPerformingPartnerships(minOrders);
    }
    
    public List<Partnership> getHighRatedPartnerships(BigDecimal minRating) {
        log.debug("Fetching high rated partnerships with minimum rating {}", minRating);
        return partnershipRepository.findHighRatedPartnerships(minRating);
    }
    
    // Statistics operations
    public long countPartnershipsByVendorCompany(VendorCompany vendorCompany) {
        return partnershipRepository.countByVendorCompany(vendorCompany);
    }
    
    public long countPartnershipsByDeliveryCompany(DeliveryCompany deliveryCompany) {
        return partnershipRepository.countByDeliveryCompany(deliveryCompany);
    }
    
    public long countPartnershipsByStatus(Partnership.PartnershipStatus status) {
        return partnershipRepository.countByStatus(status);
    }
    
    public long countActivePartnershipsByVendorCompany(VendorCompany vendorCompany) {
        return partnershipRepository.countByVendorCompanyAndStatus(vendorCompany, Partnership.PartnershipStatus.ACTIVE);
    }
    
    public long countActivePartnershipsByDeliveryCompany(DeliveryCompany deliveryCompany) {
        return partnershipRepository.countByDeliveryCompanyAndStatus(deliveryCompany, Partnership.PartnershipStatus.ACTIVE);
    }
    
    public BigDecimal calculateTotalRevenueByVendorCompany(VendorCompany vendorCompany) {
        BigDecimal revenue = partnershipRepository.calculateTotalRevenueByVendorCompany(vendorCompany);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public BigDecimal calculateTotalRevenueByDeliveryCompany(DeliveryCompany deliveryCompany) {
        BigDecimal revenue = partnershipRepository.calculateTotalRevenueByDeliveryCompany(deliveryCompany);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public Double calculateAverageRatingByVendorCompany(VendorCompany vendorCompany) {
        return partnershipRepository.calculateAverageRatingByVendorCompany(vendorCompany);
    }
    
    public Double calculateAverageRatingByDeliveryCompany(DeliveryCompany deliveryCompany) {
        return partnershipRepository.calculateAverageRatingByDeliveryCompany(deliveryCompany);
    }
    
    public Long calculateTotalOrdersByVendorCompany(VendorCompany vendorCompany) {
        Long orders = partnershipRepository.calculateTotalOrdersByVendorCompany(vendorCompany);
        return orders != null ? orders : 0L;
    }
    
    public Long calculateTotalOrdersByDeliveryCompany(DeliveryCompany deliveryCompany) {
        Long orders = partnershipRepository.calculateTotalOrdersByDeliveryCompany(deliveryCompany);
        return orders != null ? orders : 0L;
    }
    
    // Utility methods
    public boolean existsPartnershipBetween(VendorCompany vendorCompany, DeliveryCompany deliveryCompany) {
        return partnershipRepository.existsByVendorCompanyAndDeliveryCompany(vendorCompany, deliveryCompany);
    }
    
    public boolean existsActivePartnershipBetween(VendorCompany vendorCompany, DeliveryCompany deliveryCompany) {
        return partnershipRepository.existsActivePartnershipBetween(vendorCompany, deliveryCompany);
    }
    
    public boolean hasExclusivePartnership(VendorCompany vendorCompany) {
        return getExclusivePartnershipByVendorCompany(vendorCompany).isPresent();
    }
    
    public boolean canAcceptOrder(Partnership partnership, String serviceArea, BigDecimal orderValue, Double distance) {
        if (!partnership.canAcceptOrders()) {
            return false;
        }
        
        if (serviceArea != null && !partnership.isWithinServiceArea(serviceArea)) {
            return false;
        }
        
        if (orderValue != null && !partnership.meetsMinimumOrderValue(orderValue)) {
            return false;
        }
        
        if (distance != null && !partnership.isWithinDeliveryDistance(distance)) {
            return false;
        }
        
        return true;
    }
    
    // Maintenance operations
    public void updateExpiredPartnerships() {
        log.info("Updating expired partnerships");
        
        List<Partnership> expiredPartnerships = getExpiredActivePartnerships();
        
        for (Partnership partnership : expiredPartnerships) {
            partnership.setStatus(Partnership.PartnershipStatus.EXPIRED);
            partnershipRepository.save(partnership);
        }
        
        log.info("Updated {} expired partnerships", expiredPartnerships.size());
    }
}