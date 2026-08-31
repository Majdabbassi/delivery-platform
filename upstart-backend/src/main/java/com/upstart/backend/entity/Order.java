package com.upstart.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(name = "order_seq", sequenceName = "order_sequence", allocationSize = 1)
    private Long id;
    
    @NotBlank(message = "Order number is required")
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_company_id", nullable = false)
    @NotNull(message = "Vendor company is required")
    private VendorCompany vendorCompany;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_company_id")
    private DeliveryCompany deliveryCompany;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_user_id", nullable = false)
    @NotNull(message = "Customer is required")
    private CustomerUser customerUser;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_person_id")
    private DriverPerson driverPerson;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partnership_id")
    private Partnership partnership;
    
    @NotBlank(message = "Pickup address is required")
    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;
    
    @NotBlank(message = "Delivery address is required")
    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;
    
    @Column(name = "pickup_latitude")
    private Double pickupLatitude;
    
    @Column(name = "pickup_longitude")
    private Double pickupLongitude;
    
    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;
    
    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;
    
    @NotNull(message = "Order amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Order amount must be greater than 0")
    @Column(name = "order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderAmount;
    
    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private OrderPriority priority = OrderPriority.NORMAL;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "special_instructions")
    private String specialInstructions;
    
    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;
    
    @Column(name = "actual_pickup_time")
    private LocalDateTime actualPickupTime;
    
    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;
    
    @Column(name = "scheduled_pickup_time")
    private LocalDateTime scheduledPickupTime;
    
    @Column(name = "scheduled_delivery_time")
    private LocalDateTime scheduledDeliveryTime;
    
    @Column(name = "distance_km")
    private Double distanceKm;
    
    @Column(name = "weight_kg")
    private Double weightKg;
    
    @Column(name = "package_dimensions")
    private String packageDimensions;
    
    @Column(name = "is_fragile")
    private Boolean isFragile = false;
    
    @Column(name = "requires_signature")
    private Boolean requiresSignature = false;
    
    @Column(name = "tracking_number")
    private String trackingNumber;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "rating")
    private Integer rating;
    
    @Column(name = "review")
    private String review;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalAmount == null && orderAmount != null && deliveryFee != null) {
            totalAmount = orderAmount.add(deliveryFee);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (totalAmount == null && orderAmount != null && deliveryFee != null) {
            totalAmount = orderAmount.add(deliveryFee);
        }
    }
    
    // Helper methods
    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }
    
    public boolean isAssigned() {
        return status == OrderStatus.ASSIGNED;
    }
    
    public boolean isInProgress() {
        return status == OrderStatus.IN_PROGRESS;
    }
    
    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }
    
    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }
    
    public boolean isUrgent() {
        return priority == OrderPriority.URGENT;
    }
    
    public boolean isHighPriority() {
        return priority == OrderPriority.HIGH;
    }
    
    public boolean hasPartnership() {
        return partnership != null;
    }
    
    public boolean isOverdue() {
        return estimatedDeliveryTime != null && 
               LocalDateTime.now().isAfter(estimatedDeliveryTime) && 
               !isCompleted() && !isCancelled();
    }
    
    public BigDecimal getCustomerRating() {
        return rating != null ? BigDecimal.valueOf(rating) : null;
    }
    
    public BigDecimal getEstimatedDistance() {
        return distanceKm != null ? BigDecimal.valueOf(distanceKm) : null;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public BigDecimal getBaseFee() {
        // Return a base fee calculation or default value
        return BigDecimal.valueOf(5.00); // Default base fee
    }
    
    public enum OrderStatus {
        PENDING,
        ASSIGNED,
        CONFIRMED,
        IN_PROGRESS,
        PICKED_UP,
        IN_TRANSIT,
        DELIVERED,
        COMPLETED,
        CANCELLED,
        FAILED
    }
    
    public enum OrderPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}