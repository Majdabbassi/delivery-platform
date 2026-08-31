package com.upstart.backend.dto;

import com.upstart.backend.entity.Order;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderDTO {
    
    // Assignment information
    private Long deliveryCompanyId;
    private Long partnershipId;
    private Long driverPersonId;
    
    // Address information (can be updated before pickup)
    @Size(max = 500, message = "Pickup address must not exceed 500 characters")
    private String pickupAddress;
    
    @Size(max = 500, message = "Delivery address must not exceed 500 characters")
    private String deliveryAddress;
    
    @DecimalMin(value = "-90.0", message = "Pickup latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Pickup latitude must be between -90 and 90")
    private Double pickupLatitude;
    
    @DecimalMin(value = "-180.0", message = "Pickup longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Pickup longitude must be between -180 and 180")
    private Double pickupLongitude;
    
    @DecimalMin(value = "-90.0", message = "Delivery latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Delivery latitude must be between -90 and 90")
    private Double deliveryLatitude;
    
    @DecimalMin(value = "-180.0", message = "Delivery longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Delivery longitude must be between -180 and 180")
    private Double deliveryLongitude;
    
    // Order details
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Order value must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Order value must have at most 10 integer digits and 2 decimal places")
    private BigDecimal orderValue;
    
    @DecimalMin(value = "0.0", message = "Delivery fee must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Delivery fee must have at most 8 integer digits and 2 decimal places")
    private BigDecimal deliveryFee;
    
    @DecimalMin(value = "0.0", message = "Estimated distance must be non-negative")
    private Double estimatedDistance;
    
    @Min(value = 1, message = "Estimated duration must be at least 1 minute")
    private Integer estimatedDurationMinutes;
    
    // Status and priority
    private Order.OrderStatus status;
    private Order.OrderPriority priority;
    
    // Scheduling
    private LocalDateTime scheduledPickupTime;
    private LocalDateTime actualPickupTime;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    
    // Additional information
    @Size(max = 500, message = "Special instructions must not exceed 500 characters")
    private String specialInstructions;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String cancellationReason;
    
    // Customer contact information
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{10,15}$", message = "Invalid phone number format")
    private String customerPhone;
    
    @Email(message = "Invalid email format")
    private String customerEmail;
}