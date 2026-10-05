package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.VendorOwner;
import com.swiftdeliver.backend.exception.ResourceNotFoundException;
import com.swiftdeliver.backend.repository.VendorCompanyRepository;
import com.swiftdeliver.backend.repository.VendorOwnerRepository;
import com.swiftdeliver.backend.specification.VendorOwnerSpecifications;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.swiftdeliver.backend.dto.VendorOwnerDTO;

@Service
@Transactional
public class VendorOwnerService {

    @Resource
    private VendorOwnerRepository vendorOwnerRepository;

    @Resource
    private VendorCompanyRepository vendorCompanyRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    // Create operations
    public VendorOwner createVendorOwner(VendorOwner vendorOwner) {
        if (vendorOwner.getPassword() != null) {
            vendorOwner.setPassword(passwordEncoder.encode(vendorOwner.getPassword()));
        }
        vendorOwner.setCreatedAt(LocalDateTime.now());
        vendorOwner.setUpdatedAt(LocalDateTime.now());
        return vendorOwnerRepository.save(vendorOwner);
    }

    // Read operations
    @Transactional(readOnly = true)
    public VendorOwner getVendorOwnerById(Long id) {
        return vendorOwnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VendorOwner not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<VendorOwner> findVendorOwnerById(Long id) {
        return vendorOwnerRepository.findById(id);
    }



    @Transactional(readOnly = true)
    public Optional<VendorOwner> findByNationalId(String nationalId) {
        return vendorOwnerRepository.findByNationalId(nationalId);
    }

    @Transactional(readOnly = true)
    public List<VendorOwner> getAllVendorOwners() {
        return vendorOwnerRepository.findAll();
    }

    // Search with criteria
    @Transactional(readOnly = true)
    public Page<VendorOwner> searchVendorOwners(
            String username, String email, String firstName, String lastName,
            String phoneNumber, String nationalId, Boolean enabled, Boolean verified,
            String preferredBusinessCategory, Integer minBusinessExperience,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDate bornAfter, LocalDate bornBefore,
            String address, String emergencyContact,
            Pageable pageable) {

        Specification<VendorOwner> spec = null;

        if (username != null && !username.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasUsername(username) : spec.and(VendorOwnerSpecifications.hasUsername(username));
        }
        if (email != null && !email.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasEmail(email) : spec.and(VendorOwnerSpecifications.hasEmail(email));
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasFirstName(firstName) : spec.and(VendorOwnerSpecifications.hasFirstName(firstName));
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasLastName(lastName) : spec.and(VendorOwnerSpecifications.hasLastName(lastName));
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasPhoneNumber(phoneNumber) : spec.and(VendorOwnerSpecifications.hasPhoneNumber(phoneNumber));
        }
        if (nationalId != null && !nationalId.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasNationalId(nationalId) : spec.and(VendorOwnerSpecifications.hasNationalId(nationalId));
        }
        if (enabled != null) {
            spec = (spec == null) ? VendorOwnerSpecifications.isEnabled(enabled) : spec.and(VendorOwnerSpecifications.isEnabled(enabled));
        }
        if (verified != null) {
            spec = (spec == null) ? VendorOwnerSpecifications.isVerified(verified) : spec.and(VendorOwnerSpecifications.isVerified(verified));
        }
        if (preferredBusinessCategory != null && !preferredBusinessCategory.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasPreferredBusinessCategory(preferredBusinessCategory) : spec.and(VendorOwnerSpecifications.hasPreferredBusinessCategory(preferredBusinessCategory));
        }
        if (minBusinessExperience != null) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasMinBusinessExperience(minBusinessExperience) : spec.and(VendorOwnerSpecifications.hasMinBusinessExperience(minBusinessExperience));
        }
        if (createdAfter != null) {
            spec = (spec == null) ? VendorOwnerSpecifications.createdAfter(createdAfter) : spec.and(VendorOwnerSpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? VendorOwnerSpecifications.createdBefore(createdBefore) : spec.and(VendorOwnerSpecifications.createdBefore(createdBefore));
        }
        if (bornAfter != null) {
            spec = (spec == null) ? VendorOwnerSpecifications.bornAfter(bornAfter) : spec.and(VendorOwnerSpecifications.bornAfter(bornAfter));
        }
        if (bornBefore != null) {
            spec = (spec == null) ? VendorOwnerSpecifications.bornBefore(bornBefore) : spec.and(VendorOwnerSpecifications.bornBefore(bornBefore));
        }
        if (address != null && !address.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasAddress(address) : spec.and(VendorOwnerSpecifications.hasAddress(address));
        }
        if (emergencyContact != null && !emergencyContact.trim().isEmpty()) {
            spec = (spec == null) ? VendorOwnerSpecifications.hasEmergencyContact(emergencyContact) : spec.and(VendorOwnerSpecifications.hasEmergencyContact(emergencyContact));
        }

        return vendorOwnerRepository.findAll(spec, pageable);
    }

    // Quick search methods
    @Transactional(readOnly = true)
    public Page<VendorOwner> getActiveAndVerifiedVendorOwners(Pageable pageable) {
        return vendorOwnerRepository.findAll(
                VendorOwnerSpecifications.isActiveAndVerified(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<VendorOwner> getExperiencedVendorOwners(Pageable pageable) {
        return vendorOwnerRepository.findAll(
                VendorOwnerSpecifications.isExperienced(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<VendorOwner> getNewVendorOwners(LocalDateTime since, Pageable pageable) {
        return vendorOwnerRepository.findAll(
                VendorOwnerSpecifications.isNewOwner(since),
                pageable
        );
    }

    // Update operations
    public VendorOwner updateVendorOwner(Long id, VendorOwnerDTO dto) {
        VendorOwner existingVendorOwner = getVendorOwnerById(id);
        if (dto.getUsername() != null) {
            existingVendorOwner.setUsername(dto.getUsername());
        }
        if (dto.getEmail() != null) {
            existingVendorOwner.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null) {
            existingVendorOwner.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getFirstName() != null) {
            existingVendorOwner.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            existingVendorOwner.setLastName(dto.getLastName());
        }
        if (dto.getPhoneNumber() != null) {
            existingVendorOwner.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getNationalId() != null) {
            existingVendorOwner.setNationalId(dto.getNationalId());
        }
        if (dto.getDateOfBirth() != null) {
            existingVendorOwner.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getAddress() != null) {
            existingVendorOwner.setAddress(dto.getAddress());
        }
        if (dto.getEmergencyContact() != null) {
            existingVendorOwner.setEmergencyContact(dto.getEmergencyContact());
        }
        if (dto.getPreferredBusinessCategory() != null) {
            existingVendorOwner.setPreferredBusinessCategory(dto.getPreferredBusinessCategory());
        }
        if (dto.getBusinessExperienceYears() != null) {
            existingVendorOwner.setBusinessExperienceYears(dto.getBusinessExperienceYears());
        }
        if (dto.getIsVerifiedOwner() != null) {
            existingVendorOwner.setIsVerifiedOwner(dto.getIsVerifiedOwner());
        }
        existingVendorOwner.setUpdatedAt(java.time.LocalDateTime.now());
        return vendorOwnerRepository.save(existingVendorOwner);
    }

    // Status update operations
    public VendorOwner updateEnabledStatus(Long id, boolean enabled) {
        VendorOwner vendorOwner = getVendorOwnerById(id);
        vendorOwner.setEnabled(enabled);
        vendorOwner.setUpdatedAt(LocalDateTime.now());
        return vendorOwnerRepository.save(vendorOwner);
    }

    public VendorOwner updateVerifiedStatus(Long id, boolean verified) {
        VendorOwner vendorOwner = getVendorOwnerById(id);
        vendorOwner.setIsVerifiedOwner(verified);
        vendorOwner.setUpdatedAt(LocalDateTime.now());
        return vendorOwnerRepository.save(vendorOwner);
    }

    // Delete operations
    public void deleteVendorOwner(Long id) {
        VendorOwner vendorOwner = getVendorOwnerById(id);
        vendorOwnerRepository.delete(vendorOwner);
    }



    // Statistical operations
    @Transactional(readOnly = true)
    public long countAllVendorOwners() {
        return vendorOwnerRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveVendorOwners() {
        return vendorOwnerRepository.count(VendorOwnerSpecifications.isEnabled(true));
    }

    @Transactional(readOnly = true)
    public long countVerifiedVendorOwners() {
        return vendorOwnerRepository.count(VendorOwnerSpecifications.isVerified(true));
    }

    @Transactional(readOnly = true)
    public long countActiveAndVerifiedVendorOwners() {
        return vendorOwnerRepository.count(VendorOwnerSpecifications.isActiveAndVerified());
    }

    @Transactional(readOnly = true)
    public long countExperiencedVendorOwners() {
        return vendorOwnerRepository.count(VendorOwnerSpecifications.isExperienced());
    }

    @Transactional(readOnly = true)
    public long countVendorOwnersWithMultipleCompanies() {
        return vendorOwnerRepository.count(VendorOwnerSpecifications.hasMultipleCompanies());
    }

    @Transactional(readOnly = true)
    public long countTotalVendorCompanies() {
        return vendorCompanyRepository.count();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalVendorCompaniesRevenue() {
        BigDecimal revenue = vendorCompanyRepository.getTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public Double getAverageVendorOwnerExperience() {
        Double average = vendorOwnerRepository.getAverageExperience();
        return average != null ? average : 0.0;
    }

    // Validation methods
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return vendorOwnerRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return vendorOwnerRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByNationalId(String nationalId) {
        return vendorOwnerRepository.existsByNationalId(nationalId);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsernameAndIdNot(String username, Long id) {
        return vendorOwnerRepository.existsByUsernameAndIdNot(username, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return vendorOwnerRepository.existsByEmailAndIdNot(email, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByNationalIdAndIdNot(String nationalId, Long id) {
        return vendorOwnerRepository.existsByNationalIdAndIdNot(nationalId, id);
    }
}