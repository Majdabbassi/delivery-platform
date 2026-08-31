package com.upstart.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorCompany {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Company name is required")
    @Column(name = "company_name", nullable = false)
    private String companyName;
    
    @Column(name = "business_address")
    private String businessAddress;
    
    @Column(name = "business_license", unique = true)
    private String businessLicense;
    
    @Column(name = "business_description", columnDefinition = "TEXT")
    private String businessDescription;
    
    @Column(name = "contact_phone")
    private String contactPhone;
    
    @Column(name = "contact_email")
    private String contactEmail;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @DecimalMin(value = "0.0", message = "Rating cannot be negative")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Commission rate cannot be negative")
    @DecimalMax(value = "1.0", message = "Commission rate cannot exceed 100%")
    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate = new BigDecimal("0.1000"); // 10% default
    
    @Column(name = "total_orders")
    private Long totalOrders = 0L;
    
    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    
    @Column(name = "registration_date")
    private LocalDateTime registrationDate;
    
    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate;
    
    // Foreign key to VendorOwner
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private VendorOwner owner;
    
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
    
    public VendorCompany(String companyName, String businessAddress, VendorOwner owner) {
        this.companyName = companyName;
        this.businessAddress = businessAddress;
        this.owner = owner;
        this.isVerified = false;
        this.isActive = true;
        this.rating = BigDecimal.ZERO;
        this.commissionRate = new BigDecimal("0.1000");
        this.totalOrders = 0L;
        this.totalRevenue = BigDecimal.ZERO;
    }
}