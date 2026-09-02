package com.swiftdeliver.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    public enum ProductStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED
    }

    public enum DiscountType {
        PERCENTAGE, FIXED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Cost price cannot be negative")
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "category")
    private String category;

    @Column(name = "subcategory")
    private String subcategory;

    @Column(name = "brand")
    private String brand;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "barcode", unique = true)
    private String barcode;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "dimensions")
    private String dimensions;

    @Column(name = "color")
    private String color;

    @Column(name = "size")
    private String size;

    @Column(name = "material")
    private String material;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(name = "min_stock_level")
    private Integer minStockLevel = 0;

    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_image_urls", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_specifications", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "spec_key")
    @Column(name = "spec_value")
    private Map<String, String> specifications = new HashMap<>();

    @Column(name = "warranty")
    private String warranty;

    @Column(name = "manufacturer_date")
    private LocalDate manufacturerDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Foreign key to VendorCompany (serialized as vendorCompanyId)
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_company_id")
    private VendorCompany vendorCompany;

    @Column(name = "total_sold")
    private Integer totalSold = 0;

    @Column(name = "total_revenue", precision = 14, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "discount", precision = 8, scale = 2)
    private BigDecimal discount;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "discount_start_date")
    private LocalDate discountStartDate;

    @Column(name = "discount_end_date")
    private LocalDate discountEndDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
        syncStatusFromStock();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
        syncStatusFromStock();
    }

    private void syncStatusFromStock() {
        if (stockQuantity != null && stockQuantity <= 0 && status == ProductStatus.ACTIVE) {
            status = ProductStatus.OUT_OF_STOCK;
            isAvailable = false;
        }
    }

    @Transient
    private Long vendorCompanyIdInput;

    public Long getVendorCompanyId() {
        return vendorCompany != null ? vendorCompany.getId() : null;
    }

    @JsonProperty("vendorCompanyId")
    public void setVendorCompanyId(Long vendorCompanyId) {
        this.vendorCompanyIdInput = vendorCompanyId;
    }

    @JsonIgnore
    public Long getVendorCompanyIdInput() {
        return vendorCompanyIdInput;
    }
}