package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.DeliveryOwner;
import com.swiftdeliver.backend.exception.ResourceNotFoundException;
import com.swiftdeliver.backend.repository.DeliveryCompanyRepository;
import com.swiftdeliver.backend.repository.DeliveryOwnerRepository;
import com.swiftdeliver.backend.specification.DeliveryOwnerSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
@Transactional
public class DeliveryOwnerService {

    @Autowired
    private DeliveryOwnerRepository deliveryOwnerRepository;

    @Autowired
    private DeliveryCompanyRepository deliveryCompanyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Create operations
    public DeliveryOwner createDeliveryOwner(DeliveryOwner deliveryOwner) {
        if (deliveryOwner.getPassword() != null) {
            deliveryOwner.setPassword(passwordEncoder.encode(deliveryOwner.getPassword()));
        }
        deliveryOwner.setCreatedAt(LocalDateTime.now());
        deliveryOwner.setUpdatedAt(LocalDateTime.now());
        return deliveryOwnerRepository.save(deliveryOwner);
    }

    // Read operations
    @Transactional(readOnly = true)
    public DeliveryOwner getDeliveryOwnerById(Long id) {
        return deliveryOwnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryOwner not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryOwner> findDeliveryOwnerById(Long id) {
        return deliveryOwnerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryOwner> findByUsername(String username) {
        return deliveryOwnerRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryOwner> findByEmail(String email) {
        return deliveryOwnerRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryOwner> findByNationalId(String nationalId) {
        return deliveryOwnerRepository.findByNationalId(nationalId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryOwner> getAllDeliveryOwners() {
        return deliveryOwnerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOwner> getAllDeliveryOwners(Pageable pageable) {
        return deliveryOwnerRepository.findAll(pageable);
    }

    // Search with criteria
    @Transactional(readOnly = true)
    public Page<DeliveryOwner> searchDeliveryOwners(
            String username, String email, String firstName, String lastName,
            String phoneNumber, String nationalId, Boolean enabled, Boolean verified,
            String preferredBusinessCategory, Integer minBusinessExperience,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDate bornAfter, LocalDate bornBefore,
            String address, String emergencyContact,
            Pageable pageable) {

        Specification<DeliveryOwner> spec = null;

        if (username != null && !username.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasUsername(username) : spec.and(DeliveryOwnerSpecifications.hasUsername(username));
        }
        if (email != null && !email.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasEmail(email) : spec.and(DeliveryOwnerSpecifications.hasEmail(email));
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasFirstName(firstName) : spec.and(DeliveryOwnerSpecifications.hasFirstName(firstName));
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasLastName(lastName) : spec.and(DeliveryOwnerSpecifications.hasLastName(lastName));
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasPhoneNumber(phoneNumber) : spec.and(DeliveryOwnerSpecifications.hasPhoneNumber(phoneNumber));
        }
        if (nationalId != null && !nationalId.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasNationalId(nationalId) : spec.and(DeliveryOwnerSpecifications.hasNationalId(nationalId));
        }
        if (enabled != null) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.isEnabled(enabled) : spec.and(DeliveryOwnerSpecifications.isEnabled(enabled));
        }
        if (verified != null) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.isVerified(verified) : spec.and(DeliveryOwnerSpecifications.isVerified(verified));
        }
        if (preferredBusinessCategory != null && !preferredBusinessCategory.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasPreferredServiceRegions(preferredBusinessCategory) : spec.and(DeliveryOwnerSpecifications.hasPreferredServiceRegions(preferredBusinessCategory));
        }
        if (minBusinessExperience != null) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasMinLogisticsExperience(minBusinessExperience) : spec.and(DeliveryOwnerSpecifications.hasMinLogisticsExperience(minBusinessExperience));
        }
        if (createdAfter != null) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.createdAfter(createdAfter) : spec.and(DeliveryOwnerSpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.createdBefore(createdBefore) : spec.and(DeliveryOwnerSpecifications.createdBefore(createdBefore));
        }
        if (bornAfter != null) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.bornAfter(bornAfter) : spec.and(DeliveryOwnerSpecifications.bornAfter(bornAfter));
        }
        if (bornBefore != null) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.bornBefore(bornBefore) : spec.and(DeliveryOwnerSpecifications.bornBefore(bornBefore));
        }

        if (address != null && !address.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasAddress(address) : spec.and(DeliveryOwnerSpecifications.hasAddress(address));
        }
        if (emergencyContact != null && !emergencyContact.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryOwnerSpecifications.hasEmergencyContact(emergencyContact) : spec.and(DeliveryOwnerSpecifications.hasEmergencyContact(emergencyContact));
        }

        return deliveryOwnerRepository.findAll(spec, pageable);
    }

    // Quick search methods
    @Transactional(readOnly = true)
    public Page<DeliveryOwner> searchByName(String searchTerm, Pageable pageable) {
        return deliveryOwnerRepository.findAll(
                DeliveryOwnerSpecifications.searchByName(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOwner> searchByContact(String searchTerm, Pageable pageable) {
        return deliveryOwnerRepository.findAll(
                DeliveryOwnerSpecifications.searchByContact(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOwner> getActiveAndVerifiedDeliveryOwners(Pageable pageable) {
        return deliveryOwnerRepository.findAll(
                DeliveryOwnerSpecifications.isActiveAndVerified(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOwner> getExperiencedDeliveryOwners(Integer minYears, Pageable pageable) {
        return deliveryOwnerRepository.findAll(
                DeliveryOwnerSpecifications.hasExperience(minYears),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOwner> getExperiencedDeliveryOwners(Pageable pageable) {
        // Default to 2 years of experience as "experienced"
        return getExperiencedDeliveryOwners(2, pageable);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOwner> getNewDeliveryOwners(LocalDateTime since, Pageable pageable) {
        return deliveryOwnerRepository.findAll(
                DeliveryOwnerSpecifications.isNewOwner(since),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOwner> getDeliveryOwnersWithMultipleCompanies(Pageable pageable) {
        return deliveryOwnerRepository.findAll(
                DeliveryOwnerSpecifications.hasMultipleCompanies(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryOwner> getDeliveryOwnersByCategory(String category, Pageable pageable) {
        return deliveryOwnerRepository.findAll(
                DeliveryOwnerSpecifications.hasPreferredServiceRegions(category),
                pageable
        );
    }

    // Update operations
    public DeliveryOwner updateDeliveryOwner(Long id, DeliveryOwner updatedDeliveryOwner) {
        DeliveryOwner existingDeliveryOwner = getDeliveryOwnerById(id);
        
        // Update fields
        if (updatedDeliveryOwner.getUsername() != null) {
            existingDeliveryOwner.setUsername(updatedDeliveryOwner.getUsername());
        }
        if (updatedDeliveryOwner.getEmail() != null) {
            existingDeliveryOwner.setEmail(updatedDeliveryOwner.getEmail());
        }
        if (updatedDeliveryOwner.getPassword() != null) {
            existingDeliveryOwner.setPassword(passwordEncoder.encode(updatedDeliveryOwner.getPassword()));
        }
        if (updatedDeliveryOwner.getFirstName() != null) {
            existingDeliveryOwner.setFirstName(updatedDeliveryOwner.getFirstName());
        }
        if (updatedDeliveryOwner.getLastName() != null) {
            existingDeliveryOwner.setLastName(updatedDeliveryOwner.getLastName());
        }
        if (updatedDeliveryOwner.getPhoneNumber() != null) {
            existingDeliveryOwner.setPhoneNumber(updatedDeliveryOwner.getPhoneNumber());
        }
        if (updatedDeliveryOwner.getNationalId() != null) {
            existingDeliveryOwner.setNationalId(updatedDeliveryOwner.getNationalId());
        }
        if (updatedDeliveryOwner.getDateOfBirth() != null) {
            existingDeliveryOwner.setDateOfBirth(updatedDeliveryOwner.getDateOfBirth());
        }
        if (updatedDeliveryOwner.getAddress() != null) {
            existingDeliveryOwner.setAddress(updatedDeliveryOwner.getAddress());
        }
        if (updatedDeliveryOwner.getEmergencyContact() != null) {
            existingDeliveryOwner.setEmergencyContact(updatedDeliveryOwner.getEmergencyContact());
        }
        if (updatedDeliveryOwner.getLogisticsExperienceYears() != null) {
            existingDeliveryOwner.setLogisticsExperienceYears(updatedDeliveryOwner.getLogisticsExperienceYears());
        }
        if (updatedDeliveryOwner.getPreferredServiceRegions() != null) {
            existingDeliveryOwner.setPreferredServiceRegions(updatedDeliveryOwner.getPreferredServiceRegions());
        }
        if (updatedDeliveryOwner.getTransportLicenseNumber() != null) {
            existingDeliveryOwner.setTransportLicenseNumber(updatedDeliveryOwner.getTransportLicenseNumber());
        }
        
        existingDeliveryOwner.setUpdatedAt(LocalDateTime.now());
        return deliveryOwnerRepository.save(existingDeliveryOwner);
    }

    // Status update operations
    public DeliveryOwner updateEnabledStatus(Long id, boolean enabled) {
        DeliveryOwner deliveryOwner = getDeliveryOwnerById(id);
        deliveryOwner.setEnabled(enabled);
        deliveryOwner.setUpdatedAt(LocalDateTime.now());
        return deliveryOwnerRepository.save(deliveryOwner);
    }

    public DeliveryOwner updateVerifiedStatus(Long id, boolean verified) {
        DeliveryOwner deliveryOwner = getDeliveryOwnerById(id);
        deliveryOwner.setIsVerifiedOwner(verified);
        deliveryOwner.setUpdatedAt(LocalDateTime.now());
        return deliveryOwnerRepository.save(deliveryOwner);
    }

    // Delete operations
    public void deleteDeliveryOwner(Long id) {
        DeliveryOwner deliveryOwner = getDeliveryOwnerById(id);
        deliveryOwnerRepository.delete(deliveryOwner);
    }

    public void softDeleteDeliveryOwner(Long id) {
        DeliveryOwner deliveryOwner = getDeliveryOwnerById(id);
        deliveryOwner.setEnabled(false);
        deliveryOwner.setUpdatedAt(LocalDateTime.now());
        deliveryOwnerRepository.save(deliveryOwner);
    }

    // Statistical operations
    @Transactional(readOnly = true)
    public long countAllDeliveryOwners() {
        return deliveryOwnerRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveDeliveryOwners() {
        return deliveryOwnerRepository.count(DeliveryOwnerSpecifications.isEnabled(true));
    }

    @Transactional(readOnly = true)
    public long countVerifiedDeliveryOwners() {
        return deliveryOwnerRepository.count(DeliveryOwnerSpecifications.isVerified(true));
    }

    @Transactional(readOnly = true)
    public long countActiveAndVerifiedDeliveryOwners() {
        return deliveryOwnerRepository.count(DeliveryOwnerSpecifications.isActiveAndVerified());
    }

    @Transactional(readOnly = true)
    public long countExperiencedDeliveryOwners() {
        return deliveryOwnerRepository.count(DeliveryOwnerSpecifications.isExperienced());
    }

    @Transactional(readOnly = true)
    public long countDeliveryOwnersWithMultipleCompanies() {
        return deliveryOwnerRepository.count(DeliveryOwnerSpecifications.hasMultipleCompanies());
    }

    @Transactional(readOnly = true)
    public long countHighRatedDeliveryOwners() {
        return deliveryCompanyRepository.countOwnersWithHighRatedCompany(new BigDecimal("4.5"));
    }

    @Transactional(readOnly = true)
    public long countDeliveryOwnersByCategory(String category) {
        return deliveryOwnerRepository.count(DeliveryOwnerSpecifications.hasPreferredServiceRegions(category));
    }

    @Transactional(readOnly = true)
    public long countNewDeliveryOwners(LocalDateTime since) {
        return deliveryOwnerRepository.count(DeliveryOwnerSpecifications.isNewOwner(since));
    }

    // Validation methods
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return deliveryOwnerRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return deliveryOwnerRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByNationalId(String nationalId) {
        return deliveryOwnerRepository.existsByNationalId(nationalId);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsernameAndIdNot(String username, Long id) {
        return deliveryOwnerRepository.existsByUsernameAndIdNot(username, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return deliveryOwnerRepository.existsByEmailAndIdNot(email, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByNationalIdAndIdNot(String nationalId, Long id) {
        return deliveryOwnerRepository.existsByNationalIdAndIdNot(nationalId, id);
    }
}