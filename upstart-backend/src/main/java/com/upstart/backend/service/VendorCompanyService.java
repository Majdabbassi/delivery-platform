package com.upstart.backend.service;

import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.entity.VendorOwner;
import com.upstart.backend.exception.ResourceNotFoundException;
import com.upstart.backend.repository.ProductRepository;
import com.upstart.backend.repository.VendorCompanyRepository;
import com.upstart.backend.specification.VendorCompanySpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VendorCompanyService {

    @Autowired
    private VendorCompanyRepository vendorCompanyRepository;

    @Autowired
    private VendorOwnerService vendorOwnerService;

    @Autowired
    private ProductRepository productRepository;

    // Create operations
    public VendorCompany createVendorCompany(VendorCompany vendorCompany) {
        vendorCompany.setCreatedAt(LocalDateTime.now());
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        if (vendorCompany.getRegistrationDate() == null) {
            vendorCompany.setRegistrationDate(LocalDateTime.now());
        }
        return vendorCompanyRepository.save(vendorCompany);
    }

    public VendorCompany createVendorCompanyForOwner(Long ownerId, VendorCompany vendorCompany) {
        VendorOwner owner = vendorOwnerService.getVendorOwnerById(ownerId);
        vendorCompany.setOwner(owner);
        return createVendorCompany(vendorCompany);
    }

    public VendorCompany createVendorCompanyWithOwner(VendorCompany vendorCompany, Long ownerId) {
        return createVendorCompanyForOwner(ownerId, vendorCompany);
    }

    // Read operations
    @Transactional(readOnly = true)
    public VendorCompany getVendorCompanyById(Long id) {
        return vendorCompanyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VendorCompany not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<VendorCompany> findVendorCompanyById(Long id) {
        return vendorCompanyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<VendorCompany> findByCompanyName(String companyName) {
        return vendorCompanyRepository.findByCompanyName(companyName);
    }

    @Transactional(readOnly = true)
    public Optional<VendorCompany> findByBusinessLicense(String businessLicense) {
        return vendorCompanyRepository.findByBusinessLicense(businessLicense);
    }

    @Transactional(readOnly = true)
    public Optional<VendorCompany> findByContactEmail(String contactEmail) {
        return vendorCompanyRepository.findByContactEmail(contactEmail);
    }

    @Transactional(readOnly = true)
    public List<VendorCompany> getAllVendorCompanies() {
        return vendorCompanyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> getAllVendorCompanies(Pageable pageable) {
        return vendorCompanyRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<VendorCompany> getVendorCompaniesByOwner(Long ownerId) {
        return vendorCompanyRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> getVendorCompaniesByOwner(Long ownerId, Pageable pageable) {
        return vendorCompanyRepository.findByOwnerId(ownerId, pageable);
    }

    @Transactional(readOnly = true)
    public List<VendorCompany> findByOwnerId(Long ownerId) {
        return vendorCompanyRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> findByOwner(Long ownerId, Pageable pageable) {
        return vendorCompanyRepository.findByOwnerId(ownerId, pageable);
    }

    // Search with criteria
    @Transactional(readOnly = true)
    public Page<VendorCompany> searchVendorCompanies(
            String companyName, String businessAddress, String businessLicense,
            String contactEmail, String contactPhone, String businessDescription,
            String serviceArea, Boolean isActive, Boolean isVerified,
            Long ownerId, BigDecimal minRating, Integer minTotalOrders,
            BigDecimal minTotalRevenue, BigDecimal maxCommissionRate,
            LocalDate registeredAfter, LocalDate registeredBefore,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            Pageable pageable) {

        Specification<VendorCompany> spec = null;

        if (companyName != null && !companyName.trim().isEmpty()) {
            spec = (spec == null) ? VendorCompanySpecifications.hasCompanyName(companyName) : spec.and(VendorCompanySpecifications.hasCompanyName(companyName));
        }
        if (businessAddress != null && !businessAddress.trim().isEmpty()) {
            spec = (spec == null) ? VendorCompanySpecifications.hasBusinessAddress(businessAddress) : spec.and(VendorCompanySpecifications.hasBusinessAddress(businessAddress));
        }
        if (businessLicense != null && !businessLicense.trim().isEmpty()) {
            spec = (spec == null) ? VendorCompanySpecifications.hasBusinessLicense(businessLicense) : spec.and(VendorCompanySpecifications.hasBusinessLicense(businessLicense));
        }
        if (contactEmail != null && !contactEmail.trim().isEmpty()) {
            spec = (spec == null) ? VendorCompanySpecifications.hasContactEmail(contactEmail) : spec.and(VendorCompanySpecifications.hasContactEmail(contactEmail));
        }
        if (contactPhone != null && !contactPhone.trim().isEmpty()) {
            spec = (spec == null) ? VendorCompanySpecifications.hasContactPhone(contactPhone) : spec.and(VendorCompanySpecifications.hasContactPhone(contactPhone));
        }
        if (businessDescription != null && !businessDescription.trim().isEmpty()) {
            spec = (spec == null) ? VendorCompanySpecifications.hasBusinessDescription(businessDescription) : spec.and(VendorCompanySpecifications.hasBusinessDescription(businessDescription));
        }
        if (serviceArea != null && !serviceArea.trim().isEmpty()) {
            // serviceArea field does not exist in VendorCompany entity - removing this filter
        }
        if (isActive != null) {
            spec = (spec == null) ? VendorCompanySpecifications.isActive(isActive) : spec.and(VendorCompanySpecifications.isActive(isActive));
        }
        if (isVerified != null) {
            spec = (spec == null) ? VendorCompanySpecifications.isVerified(isVerified) : spec.and(VendorCompanySpecifications.isVerified(isVerified));
        }
        if (ownerId != null) {
            spec = (spec == null) ? VendorCompanySpecifications.hasOwnerId(ownerId) : spec.and(VendorCompanySpecifications.hasOwnerId(ownerId));
        }
        if (minRating != null) {
            spec = (spec == null) ? VendorCompanySpecifications.hasMinRating(minRating) : spec.and(VendorCompanySpecifications.hasMinRating(minRating));
        }
        if (minTotalOrders != null) {
            spec = (spec == null) ? VendorCompanySpecifications.hasMinTotalOrders(Long.valueOf(minTotalOrders)) : spec.and(VendorCompanySpecifications.hasMinTotalOrders(Long.valueOf(minTotalOrders)));
        }
        if (minTotalRevenue != null) {
            spec = (spec == null) ? VendorCompanySpecifications.hasMinTotalRevenue(minTotalRevenue) : spec.and(VendorCompanySpecifications.hasMinTotalRevenue(minTotalRevenue));
        }
        if (maxCommissionRate != null) {
            spec = (spec == null) ? VendorCompanySpecifications.hasMaxCommissionRate(maxCommissionRate) : spec.and(VendorCompanySpecifications.hasMaxCommissionRate(maxCommissionRate));
        }
        if (registeredAfter != null) {
            spec = (spec == null) ? VendorCompanySpecifications.registeredAfter(registeredAfter.atStartOfDay()) : spec.and(VendorCompanySpecifications.registeredAfter(registeredAfter.atStartOfDay()));
        }
        if (registeredBefore != null) {
            spec = (spec == null) ? VendorCompanySpecifications.registeredBefore(registeredBefore.atTime(23, 59, 59)) : spec.and(VendorCompanySpecifications.registeredBefore(registeredBefore.atTime(23, 59, 59)));
        }
        if (createdAfter != null) {
            spec = (spec == null) ? VendorCompanySpecifications.createdAfter(createdAfter) : spec.and(VendorCompanySpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? VendorCompanySpecifications.createdBefore(createdBefore) : spec.and(VendorCompanySpecifications.createdBefore(createdBefore));
        }

        return vendorCompanyRepository.findAll(spec, pageable);
    }

    // Quick search methods
    @Transactional(readOnly = true)
    public Page<VendorCompany> searchByNameOrDescription(String searchTerm, Pageable pageable) {
        return vendorCompanyRepository.findAll(
                VendorCompanySpecifications.searchByNameOrDescription(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> searchByContact(String searchTerm, Pageable pageable) {
        return vendorCompanyRepository.findAll(
                VendorCompanySpecifications.searchByContact(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> getActiveAndVerifiedVendorCompanies(Pageable pageable) {
        return vendorCompanyRepository.findAll(
                VendorCompanySpecifications.isActiveAndVerified(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> getHighRatedVendorCompanies(BigDecimal minRating, Pageable pageable) {
        return vendorCompanyRepository.findAll(
                VendorCompanySpecifications.hasHighRating(minRating),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> getHighRatedVendorCompanies(Pageable pageable) {
        return getHighRatedVendorCompanies(new BigDecimal("4.0"), pageable);
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> getPopularVendorCompanies(Pageable pageable) {
        return vendorCompanyRepository.findAll(
                VendorCompanySpecifications.isPopular(100L),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> getPopularVendorCompanies(Long minOrders, Pageable pageable) {
        return vendorCompanyRepository.findAll(
                VendorCompanySpecifications.isPopular(minOrders),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<VendorCompany> getVendorCompaniesByCategory(String category, Pageable pageable) {
        return vendorCompanyRepository.findAll(
                VendorCompanySpecifications.hasBusinessDescription(category),
                pageable
        );
    }

    // Note: getVendorCompaniesByServiceArea method removed as serviceArea field does not exist in VendorCompany entity
    // Consider using getVendorCompaniesByDescription or another appropriate filter method

    @Transactional(readOnly = true)
    public Page<VendorCompany> getNewVendorCompanies(LocalDateTime since, Pageable pageable) {
        return vendorCompanyRepository.findAll(
                VendorCompanySpecifications.isNewCompany(since),
                pageable
        );
    }

    // Update operations
    public VendorCompany updateVendorCompany(Long id, VendorCompany updatedVendorCompany) {
        VendorCompany existingVendorCompany = getVendorCompanyById(id);
        
        // Update fields
        if (updatedVendorCompany.getCompanyName() != null) {
            existingVendorCompany.setCompanyName(updatedVendorCompany.getCompanyName());
        }
        if (updatedVendorCompany.getBusinessAddress() != null) {
            existingVendorCompany.setBusinessAddress(updatedVendorCompany.getBusinessAddress());
        }
        if (updatedVendorCompany.getBusinessLicense() != null) {
            existingVendorCompany.setBusinessLicense(updatedVendorCompany.getBusinessLicense());
        }
        if (updatedVendorCompany.getContactEmail() != null) {
            existingVendorCompany.setContactEmail(updatedVendorCompany.getContactEmail());
        }
        if (updatedVendorCompany.getContactPhone() != null) {
            existingVendorCompany.setContactPhone(updatedVendorCompany.getContactPhone());
        }
        if (updatedVendorCompany.getBusinessDescription() != null) {
            existingVendorCompany.setBusinessDescription(updatedVendorCompany.getBusinessDescription());
        }
        // Note: serviceArea field does not exist in VendorCompany entity - removed
        if (updatedVendorCompany.getCommissionRate() != null) {
            existingVendorCompany.setCommissionRate(updatedVendorCompany.getCommissionRate());
        }
        if (updatedVendorCompany.getRegistrationDate() != null) {
            existingVendorCompany.setRegistrationDate(updatedVendorCompany.getRegistrationDate());
        }
        
        existingVendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(existingVendorCompany);
    }

    // Status update operations
    public VendorCompany updateActiveStatus(Long id, boolean isActive) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        vendorCompany.setIsActive(isActive);
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(vendorCompany);
    }

    public VendorCompany updateVerifiedStatus(Long id, boolean isVerified) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        vendorCompany.setIsVerified(isVerified);
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(vendorCompany);
    }

    // Business metrics update operations
    public VendorCompany updateRating(Long id, BigDecimal rating) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        vendorCompany.setRating(rating);
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(vendorCompany);
    }

    public VendorCompany updateTotalOrders(Long id, Long totalOrders) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        vendorCompany.setTotalOrders(totalOrders);
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(vendorCompany);
    }

    public VendorCompany updateTotalRevenue(Long id, BigDecimal totalRevenue) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        vendorCompany.setTotalRevenue(totalRevenue);
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(vendorCompany);
    }

    public VendorCompany incrementTotalOrders(Long id) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        Long currentOrders = vendorCompany.getTotalOrders() != null ? vendorCompany.getTotalOrders() : 0L;
        vendorCompany.setTotalOrders(currentOrders + 1);
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(vendorCompany);
    }

    public VendorCompany addToTotalRevenue(Long id, BigDecimal additionalRevenue) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        BigDecimal currentRevenue = vendorCompany.getTotalRevenue() != null ? vendorCompany.getTotalRevenue() : BigDecimal.ZERO;
        vendorCompany.setTotalRevenue(currentRevenue.add(additionalRevenue));
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(vendorCompany);
    }

    public VendorCompany updateCommissionRate(Long id, BigDecimal commissionRate) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        vendorCompany.setCommissionRate(commissionRate);
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        return vendorCompanyRepository.save(vendorCompany);
    }

    public VendorCompany updateTotalOrders(Long id, Integer totalOrders) {
        return updateTotalOrders(id, totalOrders != null ? totalOrders.longValue() : null);
    }

    // Delete operations
    public void deleteVendorCompany(Long id) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        vendorCompanyRepository.delete(vendorCompany);
    }

    public void softDeleteVendorCompany(Long id) {
        VendorCompany vendorCompany = getVendorCompanyById(id);
        vendorCompany.setIsActive(false);
        vendorCompany.setUpdatedAt(LocalDateTime.now());
        vendorCompanyRepository.save(vendorCompany);
    }

    // Statistical operations
    @Transactional(readOnly = true)
    public long countAllVendorCompanies() {
        return vendorCompanyRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveVendorCompanies() {
        return vendorCompanyRepository.count(VendorCompanySpecifications.isActive(true));
    }

    @Transactional(readOnly = true)
    public long countVerifiedVendorCompanies() {
        return vendorCompanyRepository.count(VendorCompanySpecifications.isVerified(true));
    }

    @Transactional(readOnly = true)
    public long countActiveAndVerifiedVendorCompanies() {
        return vendorCompanyRepository.count(VendorCompanySpecifications.isActiveAndVerified());
    }

    @Transactional(readOnly = true)
    public long countVendorCompaniesByCategory(String category) {
        return vendorCompanyRepository.count(VendorCompanySpecifications.hasBusinessDescription(category));
    }

    @Transactional(readOnly = true)
    public long countVendorCompaniesByOwner(Long ownerId) {
        return vendorCompanyRepository.count(VendorCompanySpecifications.hasOwnerId(ownerId));
    }

    @Transactional(readOnly = true)
    public long countHighRatedVendorCompanies() {
        return vendorCompanyRepository.count(VendorCompanySpecifications.hasHighRating(new BigDecimal("4.0")));
    }

    @Transactional(readOnly = true)
    public long countPopularVendorCompanies() {
        return vendorCompanyRepository.count(VendorCompanySpecifications.isPopular(100L));
    }

    @Transactional(readOnly = true)
    public long countVendorCompaniesWithProducts() {
        return productRepository.countVendorCompaniesWithProducts();
    }

    @Transactional(readOnly = true)
    public long countRecentlyEstablishedVendorCompanies() {
        return vendorCompanyRepository.countByRegistrationDateGreaterThanEqual(LocalDateTime.now().minusYears(1));
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageRating() {
        Double avgRating = vendorCompanyRepository.getAverageRating();
        return avgRating != null ? BigDecimal.valueOf(avgRating) : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        return vendorCompanyRepository.findTotalRevenue();
    }

    @Transactional(readOnly = true)
    public Long getTotalOrders() {
        return vendorCompanyRepository.findTotalOrders();
    }

    // Validation methods
    @Transactional(readOnly = true)
    public boolean existsByCompanyName(String companyName) {
        return vendorCompanyRepository.existsByCompanyName(companyName);
    }

    @Transactional(readOnly = true)
    public boolean existsByBusinessLicense(String businessLicense) {
        return vendorCompanyRepository.existsByBusinessLicense(businessLicense);
    }

    @Transactional(readOnly = true)
    public boolean existsByContactEmail(String contactEmail) {
        return vendorCompanyRepository.existsByContactEmail(contactEmail);
    }

    @Transactional(readOnly = true)
    public boolean existsByCompanyNameAndIdNot(String companyName, Long id) {
        return vendorCompanyRepository.existsByCompanyNameAndIdNot(companyName, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByBusinessLicenseAndIdNot(String businessLicense, Long id) {
        return vendorCompanyRepository.existsByBusinessLicenseAndIdNot(businessLicense, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByContactEmailAndIdNot(String contactEmail, Long id) {
        return vendorCompanyRepository.existsByContactEmailAndIdNot(contactEmail, id);
    }
}