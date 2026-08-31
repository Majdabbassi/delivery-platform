package com.upstart.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRatingDTO {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    // Customer rating for delivery service
    @DecimalMin(value = "1.0", message = "Customer rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Customer rating must be between 1.0 and 5.0")
    @Digits(integer = 1, fraction = 1, message = "Customer rating must have at most 1 integer digit and 1 decimal place")
    private BigDecimal customerRating;
    
    @Size(max = 1000, message = "Customer review must not exceed 1000 characters")
    private String customerReview;
    
    // Vendor rating for delivery service
    @DecimalMin(value = "1.0", message = "Vendor rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Vendor rating must be between 1.0 and 5.0")
    @Digits(integer = 1, fraction = 1, message = "Vendor rating must have at most 1 integer digit and 1 decimal place")
    private BigDecimal vendorRating;
    
    @Size(max = 1000, message = "Vendor review must not exceed 1000 characters")
    private String vendorReview;
    
    // Rating categories for detailed feedback
    @DecimalMin(value = "1.0", message = "Delivery speed rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Delivery speed rating must be between 1.0 and 5.0")
    private BigDecimal deliverySpeedRating;
    
    @DecimalMin(value = "1.0", message = "Communication rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Communication rating must be between 1.0 and 5.0")
    private BigDecimal communicationRating;
    
    @DecimalMin(value = "1.0", message = "Professionalism rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Professionalism rating must be between 1.0 and 5.0")
    private BigDecimal professionalismRating;
    
    @DecimalMin(value = "1.0", message = "Package condition rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Package condition rating must be between 1.0 and 5.0")
    private BigDecimal packageConditionRating;
    
    // Additional feedback
    @Size(max = 500, message = "Additional comments must not exceed 500 characters")
    private String additionalComments;
    
    // Recommendation
    private Boolean wouldRecommend;
    
    // Issues reported
    private Boolean reportedIssue;
    
    @Size(max = 500, message = "Issue description must not exceed 500 characters")
    private String issueDescription;
}