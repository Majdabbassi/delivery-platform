package com.upstart.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCompany {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Company name is required")
    @Column(name = "company_name", nullable = false)
    private String companyName;
    
    @Column(name = "company_address")
    private String companyAddress;
    
    @Column(name = "operating_license", unique = true)
    private String operatingLicense;
    
    @Column(name = "contact_phone")
    private String contactPhone;
    
    @Column(name = "contact_email")
    private String contactEmail;
    
    @Column(name = "service_region")
    private String serviceRegion;
    
    @Column(name = "managed_zones", columnDefinition = "TEXT")
    private String managedZones;
    
    @Column(name = "is_licensed")
    private Boolean isLicensed = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "max_drivers")
    private Integer maxDrivers = 50;
    
    @Column(name = "active_drivers_count")
    private Integer activeDriversCount = 0;
    
    @Column(name = "total_deliveries_managed")
    private Long totalDeliveriesManaged = 0L;
    
    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    
    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate = new BigDecimal("0.0500"); // 5% default
    
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;
    
    @Column(name = "emergency_contact")
    private String emergencyContact;
    
    @Column(name = "operating_hours")
    private String operatingHours; // e.g., "06:00-22:00"
    
    @Column(name = "vehicle_types_supported")
    private String vehicleTypesSupported; // JSON or comma-separated
    
    @Column(name = "registration_date")
    private LocalDateTime registrationDate;
    
    @Column(name = "last_delivery_date")
    private LocalDateTime lastDeliveryDate;
    
    // Foreign key to DeliveryOwner
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private DeliveryOwner owner;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (registrationDate == null) {
            registrationDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public DeliveryCompany(String companyName, String serviceRegion, DeliveryOwner owner) {
        this.companyName = companyName;
        this.serviceRegion = serviceRegion;
        this.owner = owner;
        this.isLicensed = false;
        this.isActive = true;
        this.maxDrivers = 50;
        this.activeDriversCount = 0;
        this.totalDeliveriesManaged = 0L;
        this.totalRevenue = BigDecimal.ZERO;
        this.commissionRate = new BigDecimal("0.0500");
        this.rating = BigDecimal.ZERO;
    }
}