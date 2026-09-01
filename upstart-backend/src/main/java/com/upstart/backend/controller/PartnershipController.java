package com.upstart.backend.controller;

import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.Partnership;
import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.service.DeliveryCompanyService;
import com.upstart.backend.service.PartnershipService;
import com.upstart.backend.service.VendorCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/partnerships")
@RequiredArgsConstructor
@Slf4j
public class PartnershipController {
    
    private final PartnershipService partnershipService;
    private final VendorCompanyService vendorCompanyService;
    private final DeliveryCompanyService deliveryCompanyService;
    
    // Create operations
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Partnership> createPartnership(@Valid @RequestBody Partnership partnership) {
        log.info("Creating new partnership between vendor {} and delivery company {}", 
                partnership.getVendorCompany().getId(), partnership.getDeliveryCompany().getId());
        Partnership createdPartnership = partnershipService.createPartnership(partnership);
        return new ResponseEntity<>(createdPartnership, HttpStatus.CREATED);
    }
    
    // Read operations
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Partnership> getPartnershipById(@PathVariable Long id) {
        log.debug("Fetching partnership with ID: {}", id);
        Partnership partnership = partnershipService.getPartnershipById(id);
        return ResponseEntity.ok(partnership);
    }
    
    @GetMapping("/companies")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Partnership> getPartnershipByCompanies(@RequestParam Long vendorCompanyId, 
                                                               @RequestParam Long deliveryCompanyId) {
        log.debug("Fetching partnership between vendor {} and delivery company {}", vendorCompanyId, deliveryCompanyId);
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        
        Optional<Partnership> partnership = partnershipService.getPartnershipByCompanies(vendorCompany, deliveryCompany);
        return partnership.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<Partnership>> getAllPartnerships(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Fetching all partnerships with pagination");
        Page<Partnership> partnerships = partnershipService.getAllPartnerships(pageable);
        return ResponseEntity.ok(partnerships);
    }
    
    // Update operations
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Partnership> updatePartnership(@PathVariable Long id, @Valid @RequestBody Partnership partnershipDetails) {
        log.info("Updating partnership with ID: {}", id);
        Partnership updatedPartnership = partnershipService.updatePartnership(id, partnershipDetails);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Partnership> updatePartnershipStatus(@PathVariable Long id, @RequestParam Partnership.PartnershipStatus status) {
        log.info("Updating partnership status for ID: {} to: {}", id, status);
        Partnership updatedPartnership = partnershipService.updatePartnershipStatus(id, status);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Partnership> activatePartnership(@PathVariable Long id) {
        log.info("Activating partnership with ID: {}", id);
        Partnership updatedPartnership = partnershipService.activatePartnership(id);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Partnership> suspendPartnership(@PathVariable Long id) {
        log.info("Suspending partnership with ID: {}", id);
        Partnership updatedPartnership = partnershipService.suspendPartnership(id);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    @PatchMapping("/{id}/terminate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Partnership> terminatePartnership(@PathVariable Long id) {
        log.info("Terminating partnership with ID: {}", id);
        Partnership updatedPartnership = partnershipService.terminatePartnership(id);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    @PatchMapping("/{id}/performance")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Partnership> updatePerformanceMetrics(@PathVariable Long id,
                                                              @RequestParam(required = false) Long ordersCompleted,
                                                              @RequestParam(required = false) BigDecimal revenue,
                                                              @RequestParam(required = false) BigDecimal rating) {
        log.info("Updating performance metrics for partnership ID: {}", id);
        Partnership updatedPartnership = partnershipService.updatePerformanceMetrics(id, ordersCompleted, revenue, rating);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    @PatchMapping("/{id}/increment-orders")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Partnership> incrementOrderCount(@PathVariable Long id) {
        log.debug("Incrementing order count for partnership ID: {}", id);
        Partnership updatedPartnership = partnershipService.incrementOrderCount(id);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    @PatchMapping("/{id}/add-revenue")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Partnership> addRevenue(@PathVariable Long id, @RequestParam BigDecimal revenue) {
        log.debug("Adding revenue {} to partnership ID: {}", revenue, id);
        Partnership updatedPartnership = partnershipService.addRevenue(id, revenue);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    @PatchMapping("/{id}/add-rating")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CLIENT')")
    public ResponseEntity<Partnership> addRating(@PathVariable Long id, @RequestParam BigDecimal rating) {
        log.debug("Adding rating {} to partnership ID: {}", rating, id);
        Partnership updatedPartnership = partnershipService.addRating(id, rating);
        return ResponseEntity.ok(updatedPartnership);
    }
    
    // Delete operations
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deletePartnership(@PathVariable Long id) {
        log.info("Deleting partnership with ID: {}", id);
        partnershipService.deletePartnership(id);
        return ResponseEntity.noContent().build();
    }
    
    // Query operations by company
    @GetMapping("/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<List<Partnership>> getPartnershipsByVendorCompany(@PathVariable Long vendorCompanyId) {
        log.debug("Fetching partnerships for vendor company: {}", vendorCompanyId);
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        List<Partnership> partnerships = partnershipService.getPartnershipsByVendorCompany(vendorCompany);
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Partnership>> getPartnershipsByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        log.debug("Fetching partnerships for delivery company: {}", deliveryCompanyId);
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        List<Partnership> partnerships = partnershipService.getPartnershipsByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/vendor-company/{vendorCompanyId}/active")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<List<Partnership>> getActivePartnershipsByVendorCompany(@PathVariable Long vendorCompanyId) {
        log.debug("Fetching active partnerships for vendor company: {}", vendorCompanyId);
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        List<Partnership> partnerships = partnershipService.getActivePartnershipsByVendorCompany(vendorCompany);
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/delivery-company/{deliveryCompanyId}/active")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Partnership>> getActivePartnershipsByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        log.debug("Fetching active partnerships for delivery company: {}", deliveryCompanyId);
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        List<Partnership> partnerships = partnershipService.getActivePartnershipsByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/vendor-company/{vendorCompanyId}/exclusive")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Partnership> getExclusivePartnershipByVendorCompany(@PathVariable Long vendorCompanyId) {
        log.debug("Fetching exclusive partnership for vendor company: {}", vendorCompanyId);
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        Optional<Partnership> partnership = partnershipService.getExclusivePartnershipByVendorCompany(vendorCompany);
        return partnership.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
    
    // Query operations by status
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Partnership>> getPartnershipsByStatus(@PathVariable Partnership.PartnershipStatus status) {
        log.debug("Fetching partnerships with status: {}", status);
        List<Partnership> partnerships = partnershipService.getPartnershipsByStatus(status);
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/active")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Partnership>> getActivePartnerships() {
        log.debug("Fetching all active partnerships");
        List<Partnership> partnerships = partnershipService.getActivePartnerships();
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/expired")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Partnership>> getExpiredActivePartnerships() {
        log.debug("Fetching expired active partnerships");
        List<Partnership> partnerships = partnershipService.getExpiredActivePartnerships();
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/expiring")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Partnership>> getPartnershipsExpiringWithin(@RequestParam(defaultValue = "30") int days) {
        log.debug("Fetching partnerships expiring within {} days", days);
        List<Partnership> partnerships = partnershipService.getPartnershipsExpiringWithin(days);
        return ResponseEntity.ok(partnerships);
    }
    
    // Query operations by criteria
    @GetMapping("/service-area/{serviceArea}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<List<Partnership>> getPartnershipsByServiceArea(@PathVariable String serviceArea) {
        log.debug("Fetching partnerships for service area: {}", serviceArea);
        List<Partnership> partnerships = partnershipService.getPartnershipsByServiceArea(serviceArea);
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/eligible-for-order")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<List<Partnership>> getEligiblePartnershipsForOrder(@RequestParam Long vendorCompanyId,
                                                                           @RequestParam(required = false) String serviceArea,
                                                                           @RequestParam(required = false) BigDecimal orderValue,
                                                                           @RequestParam(required = false) Double distance) {
        log.debug("Fetching eligible partnerships for order from vendor: {}", vendorCompanyId);
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        List<Partnership> partnerships = partnershipService.getEligiblePartnershipsForOrder(vendorCompany, serviceArea, orderValue, distance);
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/high-performing")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Partnership>> getHighPerformingPartnerships(@RequestParam(defaultValue = "100") Long minOrders) {
        log.debug("Fetching high performing partnerships with minimum {} orders", minOrders);
        List<Partnership> partnerships = partnershipService.getHighPerformingPartnerships(minOrders);
        return ResponseEntity.ok(partnerships);
    }
    
    @GetMapping("/high-rated")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Partnership>> getHighRatedPartnerships(@RequestParam(defaultValue = "4.0") BigDecimal minRating) {
        log.debug("Fetching high rated partnerships with minimum rating {}", minRating);
        List<Partnership> partnerships = partnershipService.getHighRatedPartnerships(minRating);
        return ResponseEntity.ok(partnerships);
    }
    
    // Statistics operations
    @GetMapping("/stats/count/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Long> countPartnershipsByVendorCompany(@PathVariable Long vendorCompanyId) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        long count = partnershipService.countPartnershipsByVendorCompany(vendorCompany);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/count/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Long> countPartnershipsByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        long count = partnershipService.countPartnershipsByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/count/status/{status}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countPartnershipsByStatus(@PathVariable Partnership.PartnershipStatus status) {
        long count = partnershipService.countPartnershipsByStatus(status);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/count/active/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Long> countActivePartnershipsByVendorCompany(@PathVariable Long vendorCompanyId) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        long count = partnershipService.countActivePartnershipsByVendorCompany(vendorCompany);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/count/active/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Long> countActivePartnershipsByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        long count = partnershipService.countActivePartnershipsByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/revenue/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<BigDecimal> getTotalRevenueByVendorCompany(@PathVariable Long vendorCompanyId) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        BigDecimal revenue = partnershipService.calculateTotalRevenueByVendorCompany(vendorCompany);
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/stats/revenue/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<BigDecimal> getTotalRevenueByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        BigDecimal revenue = partnershipService.calculateTotalRevenueByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(revenue);
    }
    
    @GetMapping("/stats/rating/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Double> getAverageRatingByVendorCompany(@PathVariable Long vendorCompanyId) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        Double rating = partnershipService.calculateAverageRatingByVendorCompany(vendorCompany);
        return ResponseEntity.ok(rating);
    }
    
    @GetMapping("/stats/rating/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Double> getAverageRatingByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        Double rating = partnershipService.calculateAverageRatingByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(rating);
    }
    
    @GetMapping("/stats/orders/vendor-company/{vendorCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Long> getTotalOrdersByVendorCompany(@PathVariable Long vendorCompanyId) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        Long orders = partnershipService.calculateTotalOrdersByVendorCompany(vendorCompany);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/stats/orders/delivery-company/{deliveryCompanyId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Long> getTotalOrdersByDeliveryCompany(@PathVariable Long deliveryCompanyId) {
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        Long orders = partnershipService.calculateTotalOrdersByDeliveryCompany(deliveryCompany);
        return ResponseEntity.ok(orders);
    }
    
    // Utility operations
    @GetMapping("/exists")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Boolean> existsPartnershipBetween(@RequestParam Long vendorCompanyId, @RequestParam Long deliveryCompanyId) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        boolean exists = partnershipService.existsPartnershipBetween(vendorCompany, deliveryCompany);
        return ResponseEntity.ok(exists);
    }
    
    @GetMapping("/exists/active")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Boolean> existsActivePartnershipBetween(@RequestParam Long vendorCompanyId, @RequestParam Long deliveryCompanyId) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        DeliveryCompany deliveryCompany = deliveryCompanyService.getDeliveryCompanyById(deliveryCompanyId);
        boolean exists = partnershipService.existsActivePartnershipBetween(vendorCompany, deliveryCompany);
        return ResponseEntity.ok(exists);
    }
    
    @GetMapping("/vendor-company/{vendorCompanyId}/has-exclusive")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Boolean> hasExclusivePartnership(@PathVariable Long vendorCompanyId) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        boolean hasExclusive = partnershipService.hasExclusivePartnership(vendorCompany);
        return ResponseEntity.ok(hasExclusive);
    }
    
    // Maintenance operations
    @PostMapping("/maintenance/update-expired")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> updateExpiredPartnerships() {
        log.info("Updating expired partnerships");
        partnershipService.updateExpiredPartnerships();
        return ResponseEntity.ok().build();
    }
}