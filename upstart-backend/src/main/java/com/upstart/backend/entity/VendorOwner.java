package com.upstart.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "vendor_owners")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class VendorOwner extends User {
    
    @Column(name = "national_id", unique = true)
    private String nationalId;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "emergency_contact")
    private String emergencyContact;
    
    @Column(name = "business_experience_years")
    private Integer businessExperienceYears = 0;
    
    @Column(name = "preferred_business_category")
    private String preferredBusinessCategory;
    
    @Column(name = "is_verified_owner")
    private Boolean isVerifiedOwner = false;
    
    public VendorOwner(String username, String email, String password, String firstName, String lastName, 
                       String phoneNumber, String nationalId) {
        super();
        setUsername(username);
        setEmail(email);
        setPassword(password);
        setFirstName(firstName);
        setLastName(lastName);
        setRole(Role.VENDOR_OWNER);
        setEnabled(true);
        setPhoneNumber(phoneNumber);
        this.nationalId = nationalId;
        this.businessExperienceYears = 0;
        this.isVerifiedOwner = false;
    }
}