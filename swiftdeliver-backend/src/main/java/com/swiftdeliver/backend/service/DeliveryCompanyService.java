package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.DeliveryOwner;
import com.swiftdeliver.backend.entity.DriverPerson;
import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.exception.ResourceNotFoundException;
import com.swiftdeliver.backend.repository.DeliveryCompanyRepository;
import com.swiftdeliver.backend.repository.DriverPersonRepository;
import com.swiftdeliver.backend.repository.OrderRepository;
import com.swiftdeliver.backend.specification.DeliveryCompanySpecifications;
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
public class DeliveryCompanyService {

    @Autowired
    private DeliveryCompanyRepository deliveryCompanyRepository;

    @Autowired
    private DeliveryOwnerService deliveryOwnerService;

    @Autowired
    private DriverPersonRepository driverPersonRepository;

    @Autowired
    private OrderRepository orderRepository;

    // Create operations
    public DeliveryCompany createDeliveryCompany(DeliveryCompany deliveryCompany) {
        deliveryCompany.setCreatedAt(LocalDateTime.now());
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        if (deliveryCompany.getRegistrationDate() == null) {
            deliveryCompany.setRegistrationDate(LocalDateTime.now());
        }
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany createDeliveryCompanyForOwner(Long ownerId, DeliveryCompany deliveryCompany) {
        DeliveryOwner owner = deliveryOwnerService.getDeliveryOwnerById(ownerId);
        deliveryCompany.setOwner(owner);
        return createDeliveryCompany(deliveryCompany);
    }

    public DeliveryCompany createDeliveryCompanyWithOwner(DeliveryCompany deliveryCompany, Long ownerId) {
        return createDeliveryCompanyForOwner(ownerId, deliveryCompany);
    }

    // Read operations
    @Transactional(readOnly = true)
    public DeliveryCompany getDeliveryCompanyById(Long id) {
        return deliveryCompanyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryCompany not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryCompany> findDeliveryCompanyById(Long id) {
        return deliveryCompanyRepository.findById(id);
    }


    @Transactional(readOnly = true)
    public Optional<DeliveryCompany> findByOperatingLicense(String operatingLicense) {
        return deliveryCompanyRepository.findByOperatingLicense(operatingLicense);
    }


    @Transactional(readOnly = true)
    public List<DeliveryCompany> getAllDeliveryCompanies() {
        return deliveryCompanyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getAllDeliveryCompanies(Pageable pageable) {
        return deliveryCompanyRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<DeliveryCompany> getDeliveryCompaniesByOwner(Long ownerId) {
        return deliveryCompanyRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getDeliveryCompaniesByOwner(Long ownerId, Pageable pageable) {
        return deliveryCompanyRepository.findByOwnerIdPageable(ownerId, pageable);
    }

    // Search with criteria
    @Transactional(readOnly = true)
    public Page<DeliveryCompany> searchDeliveryCompanies(
            String companyName, String businessAddress, String businessLicense,
            String contactEmail, String contactPhone, String serviceType,
            String serviceArea, Boolean isActive, Boolean isVerified,
            Long ownerId, BigDecimal minRating, Integer minTotalDeliveries,
            BigDecimal minTotalRevenue, BigDecimal maxCommissionRate, Integer minCapacity,
            LocalDate registeredAfter, LocalDate registeredBefore,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            Pageable pageable) {

        Specification<DeliveryCompany> spec = null;

        if (companyName != null && !companyName.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasCompanyName(companyName) : spec.and(DeliveryCompanySpecifications.hasCompanyName(companyName));
        }
        if (businessAddress != null && !businessAddress.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasCompanyAddress(businessAddress) : spec.and(DeliveryCompanySpecifications.hasCompanyAddress(businessAddress));
        }
        if (businessLicense != null && !businessLicense.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasOperatingLicense(businessLicense) : spec.and(DeliveryCompanySpecifications.hasOperatingLicense(businessLicense));
        }
        if (contactEmail != null && !contactEmail.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasContactEmail(contactEmail) : spec.and(DeliveryCompanySpecifications.hasContactEmail(contactEmail));
        }
        if (contactPhone != null && !contactPhone.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasContactPhone(contactPhone) : spec.and(DeliveryCompanySpecifications.hasContactPhone(contactPhone));
        }
        if (serviceType != null && !serviceType.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasServiceType(serviceType) : spec.and(DeliveryCompanySpecifications.hasServiceType(serviceType));
        }
        if (serviceArea != null && !serviceArea.trim().isEmpty()) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasServiceArea(serviceArea) : spec.and(DeliveryCompanySpecifications.hasServiceArea(serviceArea));
        }
        if (isActive != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.isActive(isActive) : spec.and(DeliveryCompanySpecifications.isActive(isActive));
        }
        if (isVerified != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.isVerified(isVerified) : spec.and(DeliveryCompanySpecifications.isVerified(isVerified));
        }
        if (ownerId != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasOwnerId(ownerId) : spec.and(DeliveryCompanySpecifications.hasOwnerId(ownerId));
        }
        if (minRating != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasMinRating(minRating) : spec.and(DeliveryCompanySpecifications.hasMinRating(minRating));
        }
        if (minTotalDeliveries != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasMinTotalDeliveries(Long.valueOf(minTotalDeliveries)) : spec.and(DeliveryCompanySpecifications.hasMinTotalDeliveries(Long.valueOf(minTotalDeliveries)));
        }
        if (minTotalRevenue != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasMinTotalRevenue(minTotalRevenue) : spec.and(DeliveryCompanySpecifications.hasMinTotalRevenue(minTotalRevenue));
        }
        if (maxCommissionRate != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasMaxCommissionRate(maxCommissionRate) : spec.and(DeliveryCompanySpecifications.hasMaxCommissionRate(maxCommissionRate));
        }
        if (minCapacity != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.hasMinActiveDrivers(minCapacity) : spec.and(DeliveryCompanySpecifications.hasMinActiveDrivers(minCapacity));
        }
        if (registeredAfter != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.registeredAfter(registeredAfter.atStartOfDay()) : spec.and(DeliveryCompanySpecifications.registeredAfter(registeredAfter.atStartOfDay()));
        }
        if (registeredBefore != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.registeredBefore(registeredBefore.atTime(23, 59, 59)) : spec.and(DeliveryCompanySpecifications.registeredBefore(registeredBefore.atTime(23, 59, 59)));
        }
        if (createdAfter != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.createdAfter(createdAfter) : spec.and(DeliveryCompanySpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? DeliveryCompanySpecifications.createdBefore(createdBefore) : spec.and(DeliveryCompanySpecifications.createdBefore(createdBefore));
        }

        return deliveryCompanyRepository.findAll(spec, pageable);
    }

    // Quick search methods
    @Transactional(readOnly = true)
    public Page<DeliveryCompany> searchByNameOrType(String searchTerm, Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.searchByNameOrServiceRegion(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> searchByNameOrRegion(String searchTerm, Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.searchByNameOrServiceRegion(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> searchByContact(String searchTerm, Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.searchByContact(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getActiveAndLicensedDeliveryCompanies(Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.isActiveAndVerified(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getPopularDeliveryCompanies(Pageable pageable) {
        // Default to companies with at least 100 total deliveries as "popular"
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.hasMinTotalDeliveries(100L),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getPopularDeliveryCompanies(Long minDeliveries, Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.hasMinTotalDeliveries(minDeliveries),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getDeliveryCompaniesWithCapacity(Integer minDrivers, Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.hasMinActiveDrivers(minDrivers),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getDeliveryCompaniesByServiceRegion(String serviceRegion, Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.hasServiceArea(serviceRegion),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getDeliveryCompaniesByManagedZones(String managedZones, Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.hasManagedZones(managedZones),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getNewDeliveryCompanies(LocalDateTime since, Pageable pageable) {
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.registeredAfter(since),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> getHighCapacityDeliveryCompanies(Pageable pageable) {
        // Default to companies with at least 10 active drivers as "high capacity"
        return deliveryCompanyRepository.findAll(
                DeliveryCompanySpecifications.hasMinActiveDrivers(10),
                pageable
        );
    }

    // Update operations
    public DeliveryCompany updateDeliveryCompany(Long id, DeliveryCompany updatedDeliveryCompany) {
        DeliveryCompany existingDeliveryCompany = getDeliveryCompanyById(id);
        
        // Update fields
        if (updatedDeliveryCompany.getCompanyName() != null) {
            existingDeliveryCompany.setCompanyName(updatedDeliveryCompany.getCompanyName());
        }
        if (updatedDeliveryCompany.getCompanyAddress() != null) {
            existingDeliveryCompany.setCompanyAddress(updatedDeliveryCompany.getCompanyAddress());
        }
        if (updatedDeliveryCompany.getOperatingLicense() != null) {
            existingDeliveryCompany.setOperatingLicense(updatedDeliveryCompany.getOperatingLicense());
        }
        if (updatedDeliveryCompany.getContactEmail() != null) {
            existingDeliveryCompany.setContactEmail(updatedDeliveryCompany.getContactEmail());
        }
        if (updatedDeliveryCompany.getContactPhone() != null) {
            existingDeliveryCompany.setContactPhone(updatedDeliveryCompany.getContactPhone());
        }
        if (updatedDeliveryCompany.getServiceRegion() != null) {
            existingDeliveryCompany.setServiceRegion(updatedDeliveryCompany.getServiceRegion());
        }
        if (updatedDeliveryCompany.getManagedZones() != null) {
            existingDeliveryCompany.setManagedZones(updatedDeliveryCompany.getManagedZones());
        }
        if (updatedDeliveryCompany.getCommissionRate() != null) {
            existingDeliveryCompany.setCommissionRate(updatedDeliveryCompany.getCommissionRate());
        }
        if (updatedDeliveryCompany.getRegistrationDate() != null) {
            existingDeliveryCompany.setRegistrationDate(updatedDeliveryCompany.getRegistrationDate());
        }
        
        existingDeliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(existingDeliveryCompany);
    }

    // Status update operations
    public DeliveryCompany updateActiveStatus(Long id, boolean isActive) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompany.setIsActive(isActive);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany updateVerifiedStatus(Long id, boolean isVerified) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompany.setIsLicensed(isVerified);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany updateLicensedStatus(Long id, boolean isLicensed) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompany.setIsLicensed(isLicensed);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany updateTotalDeliveries(Long id, Long totalDeliveries) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompany.setTotalDeliveriesManaged(totalDeliveries);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany updateTotalRevenue(Long id, BigDecimal totalRevenue) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompany.setTotalRevenue(totalRevenue);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany incrementTotalDeliveries(Long id) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        Long currentDeliveries = deliveryCompany.getTotalDeliveriesManaged() != null ? deliveryCompany.getTotalDeliveriesManaged() : 0L;
        deliveryCompany.setTotalDeliveriesManaged(currentDeliveries + 1);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany addToTotalRevenue(Long id, BigDecimal additionalRevenue) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        BigDecimal currentRevenue = deliveryCompany.getTotalRevenue() != null ? deliveryCompany.getTotalRevenue() : BigDecimal.ZERO;
        deliveryCompany.setTotalRevenue(currentRevenue.add(additionalRevenue));
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany updateCommissionRate(Long id, BigDecimal commissionRate) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompany.setCommissionRate(commissionRate);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    public DeliveryCompany updateMaxDrivers(Long id, Integer maxDrivers) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompany.setMaxDrivers(maxDrivers);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        return deliveryCompanyRepository.save(deliveryCompany);
    }

    // Delete operations
    public void deleteDeliveryCompany(Long id) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompanyRepository.delete(deliveryCompany);
    }

    public void softDeleteDeliveryCompany(Long id) {
        DeliveryCompany deliveryCompany = getDeliveryCompanyById(id);
        deliveryCompany.setIsActive(false);
        deliveryCompany.setUpdatedAt(LocalDateTime.now());
        deliveryCompanyRepository.save(deliveryCompany);
    }

    // Lookup operations
    @Transactional(readOnly = true)
    public Optional<DeliveryCompany> findByCompanyName(String companyName) {
        return deliveryCompanyRepository.findByCompanyName(companyName);
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryCompany> findByContactEmail(String contactEmail) {
        return deliveryCompanyRepository.findByContactEmail(contactEmail);
    }

    @Transactional(readOnly = true)
    public List<DeliveryCompany> findByOwnerId(Long ownerId) {
        return deliveryCompanyRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryCompany> findByOwner(Long ownerId, Pageable pageable) {
        return deliveryCompanyRepository.findByOwnerIdPageable(ownerId, pageable);
    }

    // Validation operations
    @Transactional(readOnly = true)
    public boolean existsByCompanyName(String companyName) {
        return deliveryCompanyRepository.existsByCompanyName(companyName);
    }

    @Transactional(readOnly = true)
    public boolean existsByOperatingLicense(String operatingLicense) {
        return deliveryCompanyRepository.existsByOperatingLicense(operatingLicense);
    }

    @Transactional(readOnly = true)
    public boolean existsByContactEmail(String contactEmail) {
        return deliveryCompanyRepository.existsByContactEmail(contactEmail);
    }

    // Statistical operations
    @Transactional(readOnly = true)
    public long countAllDeliveryCompanies() {
        return deliveryCompanyRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveDeliveryCompanies() {
        return deliveryCompanyRepository.count(DeliveryCompanySpecifications.isActive(true));
    }

    @Transactional(readOnly = true)
    public long countLicensedDeliveryCompanies() {
        return deliveryCompanyRepository.count(DeliveryCompanySpecifications.isLicensed(true));
    }

    @Transactional(readOnly = true)
    public long countActiveAndLicensedDeliveryCompanies() {
        return deliveryCompanyRepository.count(DeliveryCompanySpecifications.isActiveAndLicensed());
    }

    @Transactional(readOnly = true)
    public long countPopularDeliveryCompanies() {
        // Default to companies with at least 100 total deliveries as "popular"
        return deliveryCompanyRepository.count(DeliveryCompanySpecifications.hasMinTotalDeliveries(100L));
    }

    @Transactional(readOnly = true)
    public long countHighCapacityDeliveryCompanies() {
        // Default to companies with at least 10 active drivers as "high capacity"
        return deliveryCompanyRepository.count(DeliveryCompanySpecifications.hasMinActiveDrivers(10));
    }

    @Transactional(readOnly = true)
    public long countHighRatedDeliveryCompanies() {
        return deliveryCompanyRepository.countByRatingGreaterThanEqual(new BigDecimal("4.5"));
    }

    @Transactional(readOnly = true)
    public long countWithMultipleVehicleTypes() {
        return deliveryCompanyRepository.countWithMultipleVehicleTypes();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = deliveryCompanyRepository.findTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public long countDeliveryCompaniesByServiceRegion(String serviceRegion) {
        return deliveryCompanyRepository.count(DeliveryCompanySpecifications.hasServiceArea(serviceRegion));
    }

    @Transactional(readOnly = true)
    public long countDeliveryCompaniesByOwner(Long ownerId) {
        return deliveryCompanyRepository.count(DeliveryCompanySpecifications.hasOwnerId(ownerId));
    }

    @Transactional(readOnly = true)
    public long countDeliveryCompaniesByManagedZones(String managedZones) {
        return deliveryCompanyRepository.count(DeliveryCompanySpecifications.hasManagedZones(managedZones));
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageRating() {
        return deliveryCompanyRepository.findAverageRating();
    }

    @Transactional(readOnly = true)
    public Long getTotalDeliveries() {
        return deliveryCompanyRepository.findTotalDeliveries();
    }

    @Transactional(readOnly = true)
    public Long getTotalActiveDrivers() {
        return deliveryCompanyRepository.findTotalActiveDrivers();
    }

    // Validation methods for exclusion checks
    @Transactional(readOnly = true)
    public boolean existsByBusinessLicense(String businessLicense) {
        return deliveryCompanyRepository.existsByOperatingLicense(businessLicense);
    }

    @Transactional(readOnly = true)
    public boolean existsByCompanyNameAndIdNot(String companyName, Long id) {
        Optional<DeliveryCompany> company = deliveryCompanyRepository.findByCompanyName(companyName);
        return company.isPresent() && !company.get().getId().equals(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByBusinessLicenseAndIdNot(String businessLicense, Long id) {
        Optional<DeliveryCompany> company = deliveryCompanyRepository.findByOperatingLicense(businessLicense);
        return company.isPresent() && !company.get().getId().equals(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByContactEmailAndIdNot(String contactEmail, Long id) {
        Optional<DeliveryCompany> company = deliveryCompanyRepository.findByContactEmail(contactEmail);
        return company.isPresent() && !company.get().getId().equals(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByOperatingLicenseAndIdNot(String operatingLicense, Long id) {
        Optional<DeliveryCompany> company = deliveryCompanyRepository.findByOperatingLicense(operatingLicense);
        return company.isPresent() && !company.get().getId().equals(id);
    }

    // Analytics helpers (previously entity methods backed by one-to-many collections)

    @Transactional(readOnly = true)
    public List<DriverPerson> getDrivers(Long companyId) {
        return driverPersonRepository.findByDeliveryCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<DriverPerson> getDrivers(DeliveryCompany company) {
        if (company == null || company.getId() == null) {
            return List.of();
        }
        return getDrivers(company.getId());
    }

    @Transactional(readOnly = true)
    public List<DriverPerson> getAvailableDrivers(Long companyId) {
        return driverPersonRepository.findByDeliveryCompanyIdAndIsAvailable(companyId, true);
    }

    @Transactional(readOnly = true)
    public List<DriverPerson> getAvailableDrivers(DeliveryCompany company) {
        if (company == null || company.getId() == null) {
            return List.of();
        }
        return getAvailableDrivers(company.getId());
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableDrivers(Long companyId) {
        return driverPersonRepository.countByDeliveryCompanyIdAndIsAvailable(companyId, true) > 0;
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableDrivers(DeliveryCompany company) {
        return company != null && company.getId() != null && hasAvailableDrivers(company.getId());
    }

    @Transactional(readOnly = true)
    public boolean isAvailableForOrders(DeliveryCompany company) {
        return company != null
                && Boolean.TRUE.equals(company.getIsActive())
                && Boolean.TRUE.equals(company.getIsLicensed())
                && hasAvailableDrivers(company);
    }

    @Transactional(readOnly = true)
    public boolean canAcceptOrder(DeliveryCompany company, Order order) {
        if (company == null || order == null) {
            return false;
        }
        // Only unassigned, pending orders can be accepted.
        if (order.getStatus() != Order.OrderStatus.PENDING || order.getDeliveryCompany() != null) {
            return false;
        }
        return isAvailableForOrders(company)
                && isInServiceArea(company, order.getPickupAddress(), order.getDeliveryAddress());
    }

    @Transactional(readOnly = true)
    public boolean isInServiceArea(DeliveryCompany company, String pickupAddress, String deliveryAddress) {
        if (company == null) {
            return false;
        }
        String region = company.getServiceRegion();
        if (region == null || region.isBlank()) {
            return false;
        }
        // Free-text service region (comma-separated): satisfy if either endpoint
        // mentions any configured region term. Pure geographic containment would
        // require structured polygons, which are not present in this model.
        String normalizedRegion = region.toLowerCase();
        if (regionMatches(normalizedRegion, pickupAddress) || regionMatches(normalizedRegion, deliveryAddress)) {
            return true;
        }
        // Fall back to simple non-empty check when no address is available.
        return (pickupAddress == null || pickupAddress.isBlank())
                && (deliveryAddress == null || deliveryAddress.isBlank());
    }

    private boolean regionMatches(String normalizedRegion, String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        String normalizedAddress = address.toLowerCase();
        for (String term : normalizedRegion.split(",")) {
            String t = term.trim();
            if (!t.isEmpty() && normalizedAddress.contains(t)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public long getActiveOrdersCount(Long companyId) {
        return orderRepository.countByDeliveryCompanyIdAndStatusIn(
                companyId, List.of(Order.OrderStatus.PICKED_UP, Order.OrderStatus.IN_TRANSIT));
    }

    @Transactional(readOnly = true)
    public long getActiveOrdersCount(DeliveryCompany company) {
        if (company == null || company.getId() == null) {
            return 0L;
        }
        return getActiveOrdersCount(company.getId());
    }

    @Transactional(readOnly = true)
    public long getTotalOrders(Long companyId) {
        return orderRepository.countByDeliveryCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public long getTotalDrivers(Long companyId) {
        return driverPersonRepository.countByDeliveryCompanyId(companyId);
    }

    @Transactional
    public void refreshActiveDriversCount(Long companyId) {
        deliveryCompanyRepository.findById(companyId).ifPresent(company -> {
            company.setActiveDriversCount(
                    (int) driverPersonRepository.countByDeliveryCompanyIdAndIsAvailable(companyId, true));
            deliveryCompanyRepository.save(company);
        });
    }

}