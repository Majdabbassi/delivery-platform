package com.swiftdeliver.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "delivery_owners")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOwner extends User {
    
    @Column(name = "national_id", unique = true)
    private String nationalId;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "emergency_contact")
    private String emergencyContact;
    
    @Column(name = "logistics_experience_years")
    private Integer logisticsExperienceYears = 0;
    
    @Column(name = "preferred_service_regions")
    private String preferredServiceRegions; // JSON or comma-separated
    
    @Column(name = "transport_license_number")
    private String transportLicenseNumber;
    
    @Column(name = "is_verified_owner")
    private Boolean isVerifiedOwner = false;
    
    @Column(name = "max_companies_allowed")
    private Integer maxCompaniesAllowed = 3; // Limit per owner
    
    public DeliveryOwner(String username, String email, String password, String firstName, String lastName, 
                         String phoneNumber, String nationalId) {
        super();
        setUsername(username);
        setEmail(email);
        setPassword(password);
        setFirstName(firstName);
        setLastName(lastName);
        setRole(Role.DELIVERY_OWNER);
        setEnabled(true);
        setPhoneNumber(phoneNumber);
        this.nationalId = nationalId;
        this.logisticsExperienceYears = 0;
        this.isVerifiedOwner = false;
        this.maxCompaniesAllowed = 3;
    }
}