package com.upstart.backend.dto;

import com.upstart.backend.entity.Partnership;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipDTO {
    
    private Long id;
    
    // Company information
    private Long vendorCompanyId;
    private String vendorCompanyName;
    private String vendorCompanyEmail;
    private String vendorCompanyPhone;
    
    private Long deliveryCompanyId;
    private String deliveryCompanyName;
    private String deliveryCompanyEmail;
    private String deliveryCompanyPhone;
    
    // Partnership details
    private Partnership.PartnershipStatus status;
    private String statusDisplayName;
    
    private BigDecimal commissionRate;
    private List<String> serviceAreas;
    private boolean isExclusive;
    
    // Constraints
    private BigDecimal minOrderValue;
    private BigDecimal maxOrderValue;
    private Double maxDeliveryDistance;
    
    // Contract information
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Performance metrics
    private Long totalOrdersCompleted;
    private BigDecimal totalRevenue;
    private BigDecimal averageRating;
    private Long totalRatings;
    
    // Calculated fields
    private boolean isActive;
    private boolean isExpired;
    private boolean isExpiringSoon; // within 30 days
    private Long daysUntilExpiry;
    private Long contractDurationDays;
    private BigDecimal monthlyRevenue;
    private Double ordersPerMonth;
    
    // Additional information
    private String notes;
    private String terms;
    private String specialConditions;
    
    // Recent activity
    private LocalDateTime lastOrderDate;
    private Long recentOrdersCount; // last 30 days
    private BigDecimal recentRevenue; // last 30 days
}