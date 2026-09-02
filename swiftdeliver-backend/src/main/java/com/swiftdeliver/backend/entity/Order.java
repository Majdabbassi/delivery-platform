package com.swiftdeliver.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 30)
    private OrderType orderType = OrderType.MARKETPLACE;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_mode", nullable = false, length = 30)
    private RoutingMode routingMode = RoutingMode.OPEN_BID;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false, length = 30)
    private PricingMode pricingMode = PricingMode.FIXED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendor_company_id", nullable = true)
    private VendorCompany vendorCompany;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_company_id")
    private DeliveryCompany deliveryCompany;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_user_id", nullable = true)
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

    @Column(name = "sender_name", length = 150)
    private String senderName;

    @Column(name = "sender_phone", length = 30)
    private String senderPhone;

    @Column(name = "recipient_name", length = 150)
    private String recipientName;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
    
    @Column(name = "pickup_latitude")
    private Double pickupLatitude;
    
    @Column(name = "pickup_longitude")
    private Double pickupLongitude;
    
    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;
    
    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Order amount must be greater than 0")
    @Column(name = "order_amount", nullable = true, precision = 10, scale = 2)
    private BigDecimal orderAmount;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Proposed minimum amount must be greater than 0")
    @Column(name = "proposed_min_amount", nullable = true, precision = 10, scale = 2)
    private BigDecimal proposedMinAmount;

    @DecimalMin(value = "0.0", inclusive = false, message = "Proposed maximum amount must be greater than 0")
    @Column(name = "proposed_max_amount", nullable = true, precision = 10, scale = 2)
    private BigDecimal proposedMaxAmount;
    
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
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
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
        recalcTotalAmount();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalcTotalAmount();
    }

    private void recalcTotalAmount() {
        if (orderAmount != null && deliveryFee != null) {
            totalAmount = orderAmount.add(deliveryFee);
        } else if (totalAmount == null && orderAmount != null) {
            totalAmount = orderAmount;
        } else if (totalAmount == null && deliveryFee != null) {
            totalAmount = deliveryFee;
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
        // DELIVERED is the terminal "done" state; COMPLETED was removed.
        return status == OrderStatus.DELIVERED;
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
        OPEN_FOR_BID,
        ASSIGNED,
        CONFIRMED,
        IN_PROGRESS,
        PICKED_UP,
        IN_TRANSIT,
        DELIVERED,
        CANCELLED,
        FAILED
    }
    
    public enum OrderPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum OrderType {
        MARKETPLACE,
        GENERAL_DELIVERY
    }

    public enum RoutingMode {
        DIRECT,
        OPEN_BID
    }

    public enum PricingMode {
        FIXED,
        MIN_MAX
    }
}