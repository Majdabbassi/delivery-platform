package com.upstart.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorOwnerDTO {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String nationalId;
    private LocalDate dateOfBirth;
    private String address;
    private String emergencyContact;
    private Integer businessExperienceYears;
    private String preferredBusinessCategory;
    private Boolean isVerifiedOwner;
}