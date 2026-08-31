package com.upstart.backend.dto;

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
public class CreatePartnershipDTO {
    
    @NotNull(message = "Vendor company ID is required")
    private Long vendorCompanyId;
    
    @NotNull(message = "Delivery company ID is required")
    private Long deliveryCompanyId;
    
    // Partnership details
    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.0", message = "Commission rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Commission rate must not exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Commission rate must have at most 3 integer digits and 2 decimal places")
    private BigDecimal commissionRate;
    
    @NotEmpty(message = "At least one service area is required")
    @Size(max = 10, message = "Maximum 10 service areas allowed")
    private List<@NotBlank(message = "Service area cannot be blank") 
                 @Size(max = 100, message = "Service area must not exceed 100 characters") String> serviceAreas;
    
    @Builder.Default
    private boolean isExclusive = false;
    
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
    @NotNull(message = "Contract start date is required")
    @FutureOrPresent(message = "Contract start date must be today or in the future")
    private LocalDate contractStartDate;
    
    @NotNull(message = "Contract end date is required")
    @Future(message = "Contract end date must be in the future")
    private LocalDate contractEndDate;
    
    // Additional information
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    @Size(max = 2000, message = "Terms must not exceed 2000 characters")
    private String terms;
    
    @Size(max = 1000, message = "Special conditions must not exceed 1000 characters")
    private String specialConditions;
    
    // Validation method to ensure end date is after start date
    @AssertTrue(message = "Contract end date must be after start date")
    public boolean isValidContractPeriod() {
        if (contractStartDate == null || contractEndDate == null) {
            return true; // Let @NotNull handle null validation
        }
        return contractEndDate.isAfter(contractStartDate);
    }
    
    // Validation method to ensure max order value is greater than min order value
    @AssertTrue(message = "Maximum order value must be greater than minimum order value")
    public boolean isValidOrderValueRange() {
        if (minOrderValue == null || maxOrderValue == null) {
            return true; // Allow null values
        }
        return maxOrderValue.compareTo(minOrderValue) > 0;
    }
}