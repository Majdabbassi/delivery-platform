package com.swiftdeliver.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_persons")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class DriverPerson extends User {
    
    @Column(name = "license_number", nullable = true, unique = true)  // Change to true
    private String licenseNumber;

    @Column(name = "vehicle_type", nullable = true)  // Change to true
    private VehicleType vehicleType;

    @Column(name = "vehicle_plate", nullable = true)  // Change to true
    private String vehiclePlate;
    
    @Column(name = "is_available")
    private Boolean isAvailable = true;
    
    @Column(name = "current_location")
    private String currentLocation; // Could be coordinates or address
    
    @DecimalMin(value = "0.0", message = "Rating cannot be negative")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;
    
    @Column(name = "total_deliveries")
    private Long totalDeliveries = 0L;
    
    @Column(name = "total_earnings", precision = 10, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    @Column(name = "delivery_zone")
    private String deliveryZone; // Assigned delivery zone
    
    @Column(name = "emergency_contact")
    private String emergencyContact;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Column(name = "vehicle_model")
    private String vehicleModel;
    
    @Column(name = "vehicle_color")
    private String vehicleColor;
    
    // Foreign key to DeliveryCompany
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "delivery_company_id")
    private DeliveryCompany deliveryCompany;
    
    public DriverPerson(String username, String email, String password, String firstName, String lastName,
                        String licenseNumber, VehicleType vehicleType, String vehiclePlate, String phoneNumber) {
        super();
        setUsername(username);
        setEmail(email);
        setPassword(password);
        setFirstName(firstName);
        setLastName(lastName);
        setRole(Role.DRIVER);
        setEnabled(true);
        this.licenseNumber = licenseNumber;
        this.vehicleType = vehicleType;
        this.vehiclePlate = vehiclePlate;
        setPhoneNumber(phoneNumber);
        this.isAvailable = true;
        this.rating = BigDecimal.ZERO;
        this.totalDeliveries = 0L;
        this.totalEarnings = BigDecimal.ZERO;
        this.isVerified = false;
        this.lastActive = LocalDateTime.now();
    }
    
    public enum VehicleType {
        BIKE, SCOOTER, CAR, VAN, TRUCK
    }
}