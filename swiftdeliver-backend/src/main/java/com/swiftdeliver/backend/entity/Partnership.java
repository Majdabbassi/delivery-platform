package com.swiftdeliver.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "partnerships")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Partnership {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "partnership_seq")
    @SequenceGenerator(name = "partnership_seq", sequenceName = "partnership_sequence", allocationSize = 1)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_company_id", nullable = false)
    @NotNull(message = "Vendor company is required")
    private VendorCompany vendorCompany;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_company_id", nullable = false)
    @NotNull(message = "Delivery company is required")
    private DeliveryCompany deliveryCompany;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PartnershipStatus status = PartnershipStatus.PENDING;
    
    @DecimalMin(value = "0.0", message = "Commission rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Commission rate cannot exceed 100%")
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "partnership_service_areas", joinColumns = @JoinColumn(name = "partnership_id"))
    @Column(name = "service_area")
    private List<String> serviceAreas;
    
    @DecimalMin(value = "0.0", message = "Minimum order value must be non-negative")
    @Column(name = "minimum_order_value", precision = 10, scale = 2)
    private BigDecimal minimumOrderValue;
    
    @DecimalMin(value = "0.0", message = "Maximum delivery distance must be non-negative")
    @Column(name = "maximum_delivery_distance_km")
    private Double maximumDeliveryDistanceKm;
    
    @Column(name = "estimated_delivery_time_hours")
    private Integer estimatedDeliveryTimeHours;
    
    @Column(name = "partnership_terms", columnDefinition = "TEXT")
    private String partnershipTerms;
    
    @Column(name = "is_exclusive")
    private Boolean isExclusive = false;
    
    @Column(name = "contract_start_date")
    private LocalDateTime contractStartDate;
    
    @Column(name = "contract_end_date")
    private LocalDateTime contractEndDate;
    
    @Column(name = "total_orders_completed")
    private Long totalOrdersCompleted = 0L;
    
    @Column(name = "total_revenue_generated", precision = 15, scale = 2)
    private BigDecimal totalRevenueGenerated = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Average rating must be non-negative")
    @DecimalMax(value = "5.0", message = "Average rating cannot exceed 5.0")
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;
    
    @Column(name = "total_ratings_count")
    private Long totalRatingsCount = 0L;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
    
    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public boolean isActive() {
        return status == PartnershipStatus.ACTIVE;
    }
    
    public boolean isPending() {
        return status == PartnershipStatus.PENDING;
    }
    
    public boolean isSuspended() {
        return status == PartnershipStatus.SUSPENDED;
    }
    
    public boolean isTerminated() {
        return status == PartnershipStatus.TERMINATED;
    }
    
    public boolean isExpired() {
        return contractEndDate != null && LocalDateTime.now().isAfter(contractEndDate);
    }
    
    public boolean canAcceptOrders() {
        return isActive() && !isExpired();
    }
    
    public boolean isWithinServiceArea(String area) {
        return serviceAreas != null && serviceAreas.contains(area);
    }
    
    public boolean meetsMinimumOrderValue(BigDecimal orderValue) {
        return minimumOrderValue == null || orderValue.compareTo(minimumOrderValue) >= 0;
    }
    
    public boolean isWithinDeliveryDistance(Double distanceKm) {
        return maximumDeliveryDistanceKm == null || distanceKm <= maximumDeliveryDistanceKm;
    }
    
    public void incrementOrderCount() {
        this.totalOrdersCompleted = (this.totalOrdersCompleted == null ? 0L : this.totalOrdersCompleted) + 1;
    }
    
    public void addRevenue(BigDecimal revenue) {
        if (this.totalRevenueGenerated == null) {
            this.totalRevenueGenerated = BigDecimal.ZERO;
        }
        this.totalRevenueGenerated = this.totalRevenueGenerated.add(revenue);
    }
    
    public void updateRating(BigDecimal newRating) {
        if (this.totalRatingsCount == null) {
            this.totalRatingsCount = 0L;
        }
        if (this.averageRating == null) {
            this.averageRating = BigDecimal.ZERO;
        }
        
        BigDecimal totalRating = this.averageRating.multiply(BigDecimal.valueOf(this.totalRatingsCount));
        totalRating = totalRating.add(newRating);
        this.totalRatingsCount++;
        this.averageRating = totalRating.divide(BigDecimal.valueOf(this.totalRatingsCount), 2, RoundingMode.HALF_UP);
    }
    
    public boolean isExclusive() {
        return Boolean.TRUE.equals(isExclusive);
    }
    
    // Additional getter methods for OrderAssignmentService compatibility
    public BigDecimal getMinOrderValue() {
        return minimumOrderValue;
    }
    
    public BigDecimal getMaxOrderValue() {
        // If no maximum is set, return a very large value to indicate no limit
        return null; // No maximum order value constraint
    }
    
    public Double getMinDeliveryDistance() {
        // No minimum delivery distance constraint
        return 0.0;
    }
    
    public Double getMaxDeliveryDistance() {
        return maximumDeliveryDistanceKm;
    }
    
    public enum PartnershipStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        TERMINATED,
        EXPIRED
    }
}