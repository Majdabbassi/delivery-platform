package com.upstart.backend.dto;

import com.upstart.backend.entity.Order;
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
public class OrderDTO {
    
    private Long id;
    private String orderNumber;
    private String trackingNumber;
    
    // Company information
    private Long vendorCompanyId;
    private String vendorCompanyName;
    private Long deliveryCompanyId;
    private String deliveryCompanyName;
    private Long partnershipId;
    
    // Customer and driver information
    private Long customerUserId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Long driverPersonId;
    private String driverName;
    private String driverPhone;
    
    // Address information
    private String pickupAddress;
    private String deliveryAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    
    // Order details
    private String description;
    private BigDecimal orderValue;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private Double estimatedDistance;
    private Integer estimatedDurationMinutes;
    
    // Status and priority
    private Order.OrderStatus status;
    private Order.OrderPriority priority;
    
    // Timestamps
    private LocalDateTime orderDate;
    private LocalDateTime scheduledPickupTime;
    private LocalDateTime actualPickupTime;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Rating and review
    private BigDecimal customerRating;
    private String customerReview;
    private BigDecimal vendorRating;
    private String vendorReview;
    
    // Additional information
    private String specialInstructions;
    private String cancellationReason;
    private String notes;
    
    // Calculated fields
    private boolean isOverdue;
    private boolean isAssigned;
    private boolean isCompleted;
    private boolean isCancelled;
    private Long durationMinutes;
    private String statusDisplayName;
    private String priorityDisplayName;
}