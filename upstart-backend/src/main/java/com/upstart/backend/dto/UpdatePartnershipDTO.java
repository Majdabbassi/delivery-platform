package com.upstart.backend.dto;

import com.upstart.backend.entity.Partnership;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePartnershipDTO {
    
    // Partnership details
    @DecimalMin(value = "0.0", message = "Commission rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Commission rate must not exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Commission rate must have at most 3 integer digits and 2 decimal places")
    private BigDecimal commissionRate;
    
    @Size(max = 10, message = "Maximum 10 service areas allowed")
    private List<@NotBlank(message = "Service area cannot be blank") 
                 @Size(max = 100, message = "Service area must not exceed 100 characters") String> serviceAreas;
    
    private Boolean isExclusive;
    
    // Status
    private Partnership.PartnershipStatus status;
    
    // Constraints
    @DecimalMin(value = "0.0", message = "Minimum order value must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Minimum order value must have at most 10 integer digits and 2 decimal places")
    private BigDecimal minOrderValue;
    
    @DecimalMin(value = "0.0", message = "Maximum order value must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Maximum order value must have at most 10 integer digits and 2 decimal places")
    private BigDecimal maxOrderValue;
    
    @DecimalMin(value = "0.0", message = "Maximum delivery distance must be non-negative")
    @DecimalMax(value = "1000.0", message = "Maximum delivery distance must not exceed 1000 km")
    private Double maxDeliveryDistance;
    
    // Contract information
    @FutureOrPresent(message = "Contract start date must be today or in the future")
    private LocalDate contractStartDate;
    
    @Future(message = "Contract end date must be in the future")
    private LocalDate contractEndDate;
    
    // Additional information
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    @Size(max = 2000, message = "Terms must not exceed 2000 characters")
    private String terms;
    
    @Size(max = 1000, message = "Special conditions must not exceed 1000 characters")
    private String specialConditions;
    
    // Performance metrics (usually updated by system, but can be manually adjusted)
    @Min(value = 0, message = "Total orders completed must be non-negative")
    private Long totalOrdersCompleted;
    
    @DecimalMin(value = "0.0", message = "Total revenue must be non-negative")
    @Digits(integer = 12, fraction = 2, message = "Total revenue must have at most 12 integer digits and 2 decimal places")
    private BigDecimal totalRevenue;
    
    @DecimalMin(value = "1.0", message = "Average rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Average rating must be between 1.0 and 5.0")
    @Digits(integer = 1, fraction = 2, message = "Average rating must have at most 1 integer digit and 2 decimal places")
    private BigDecimal averageRating;
    
    @Min(value = 0, message = "Total ratings must be non-negative")
    private Long totalRatings;
    
    // Validation method to ensure end date is after start date
    @AssertTrue(message = "Contract end date must be after start date")
    public boolean isValidContractPeriod() {
        if (contractStartDate == null || contractEndDate == null) {
            return true; // Allow null values for partial updates
        }
        return contractEndDate.isAfter(contractStartDate);
    }
    
    // Validation method to ensure max order value is greater than min order value
    @AssertTrue(message = "Maximum order value must be greater than minimum order value")
    public boolean isValidOrderValueRange() {
        if (minOrderValue == null || maxOrderValue == null) {
            return true; // Allow null values for partial updates
        }
        return maxOrderValue.compareTo(minOrderValue) > 0;
    }
    
    // Helper method to check if any field is set (for partial updates)
    public boolean hasAnyFieldSet() {
        return commissionRate != null || serviceAreas != null || isExclusive != null ||
               status != null || minOrderValue != null || maxOrderValue != null ||
               maxDeliveryDistance != null || contractStartDate != null || contractEndDate != null ||
               notes != null || terms != null || specialConditions != null ||
               totalOrdersCompleted != null || totalRevenue != null || averageRating != null ||
               totalRatings != null;
    }
}