package com.upstart.backend.specification;

import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.DeliveryCompany_;
import com.upstart.backend.entity.DeliveryOwner;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeliveryCompanySpecifications {

    public static Specification<DeliveryCompany> hasCompanyName(String companyName) {
        return (root, query, criteriaBuilder) -> {
            if (companyName == null || companyName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryCompany_.companyName)),
                "%" + companyName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryCompany> hasBusinessAddress(String businessAddress) {
        return hasCompanyAddress(businessAddress);
    }

    public static Specification<DeliveryCompany> hasCompanyAddress(String companyAddress) {
        return (root, query, criteriaBuilder) -> {
            if (companyAddress == null || companyAddress.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryCompany_.companyAddress)),
                "%" + companyAddress.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryCompany> hasBusinessLicense(String businessLicense) {
        return hasOperatingLicense(businessLicense);
    }

    public static Specification<DeliveryCompany> hasOperatingLicense(String operatingLicense) {
        return (root, query, criteriaBuilder) -> {
            if (operatingLicense == null || operatingLicense.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(DeliveryCompany_.operatingLicense),
                operatingLicense
            );
        };
    }

    public static Specification<DeliveryCompany> hasContactEmail(String contactEmail) {
        return (root, query, criteriaBuilder) -> {
            if (contactEmail == null || contactEmail.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryCompany_.contactEmail)),
                "%" + contactEmail.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryCompany> hasContactPhone(String contactPhone) {
        return (root, query, criteriaBuilder) -> {
            if (contactPhone == null || contactPhone.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(DeliveryCompany_.contactPhone),
                "%" + contactPhone + "%"
            );
        };
    }

    public static Specification<DeliveryCompany> hasServiceRegion(String serviceRegion) {
        return (root, query, criteriaBuilder) -> {
            if (serviceRegion == null || serviceRegion.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryCompany_.serviceRegion)),
                "%" + serviceRegion.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryCompany> hasManagedZones(String managedZones) {
        return (root, query, criteriaBuilder) -> {
            if (managedZones == null || managedZones.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryCompany_.managedZones)),
                "%" + managedZones.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryCompany> isActive(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(DeliveryCompany_.isActive),
                isActive
            );
        };
    }

    public static Specification<DeliveryCompany> isLicensed(Boolean isLicensed) {
        return (root, query, criteriaBuilder) -> {
            if (isLicensed == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(DeliveryCompany_.isLicensed),
                isLicensed
            );
        };
    }

    public static Specification<DeliveryCompany> hasOwner(DeliveryOwner owner) {
        return (root, query, criteriaBuilder) -> {
            if (owner == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(DeliveryCompany_.owner),
                owner
            );
        };
    }

    public static Specification<DeliveryCompany> hasOwnerId(Long ownerId) {
        return (root, query, criteriaBuilder) -> {
            if (ownerId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(DeliveryCompany_.owner).get("id"),
                ownerId
            );
        };
    }

    public static Specification<DeliveryCompany> hasMinTotalDeliveries(Long minDeliveries) {
        return (root, query, criteriaBuilder) -> {
            if (minDeliveries == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryCompany_.totalDeliveriesManaged),
                minDeliveries
            );
        };
    }

    public static Specification<DeliveryCompany> hasMaxTotalDeliveries(Long maxDeliveries) {
        return (root, query, criteriaBuilder) -> {
            if (maxDeliveries == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryCompany_.totalDeliveriesManaged),
                maxDeliveries
            );
        };
    }

    public static Specification<DeliveryCompany> hasMinTotalRevenue(BigDecimal minRevenue) {
        return (root, query, criteriaBuilder) -> {
            if (minRevenue == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryCompany_.totalRevenue),
                minRevenue
            );
        };
    }

    public static Specification<DeliveryCompany> hasMaxTotalRevenue(BigDecimal maxRevenue) {
        return (root, query, criteriaBuilder) -> {
            if (maxRevenue == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryCompany_.totalRevenue),
                maxRevenue
            );
        };
    }

    public static Specification<DeliveryCompany> hasMinCommissionRate(BigDecimal minRate) {
        return (root, query, criteriaBuilder) -> {
            if (minRate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryCompany_.commissionRate),
                minRate
            );
        };
    }

    public static Specification<DeliveryCompany> hasMaxCommissionRate(BigDecimal maxRate) {
        return (root, query, criteriaBuilder) -> {
            if (maxRate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryCompany_.commissionRate),
                maxRate
            );
        };
    }

    public static Specification<DeliveryCompany> hasMinActiveDrivers(Integer minDrivers) {
        return (root, query, criteriaBuilder) -> {
            if (minDrivers == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryCompany_.activeDriversCount),
                minDrivers
            );
        };
    }

    public static Specification<DeliveryCompany> hasMaxActiveDrivers(Integer maxDrivers) {
        return (root, query, criteriaBuilder) -> {
            if (maxDrivers == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryCompany_.activeDriversCount),
                maxDrivers
            );
        };
    }

    public static Specification<DeliveryCompany> registeredAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryCompany_.registrationDate),
                dateTime
            );
        };
    }

    public static Specification<DeliveryCompany> registeredBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryCompany_.registrationDate),
                dateTime
            );
        };
    }

    public static Specification<DeliveryCompany> createdAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryCompany_.createdAt),
                dateTime
            );
        };
    }

    public static Specification<DeliveryCompany> createdBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryCompany_.createdAt),
                dateTime
            );
        };
    }

    // Combined specifications for common use cases
    public static Specification<DeliveryCompany> isActiveAndLicensed() {
        return isActive(true).and(isLicensed(true));
    }

    public static Specification<DeliveryCompany> isActiveAndVerified() {
        return isActive(true).and(isLicensed(true));
    }

    public static Specification<DeliveryCompany> searchByNameOrRegion(String searchTerm) {
        return hasCompanyName(searchTerm).or(hasServiceRegion(searchTerm));
    }

    public static Specification<DeliveryCompany> searchByNameOrServiceRegion(String searchTerm) {
        return hasCompanyName(searchTerm).or(hasServiceRegion(searchTerm));
    }

    public static Specification<DeliveryCompany> searchByContact(String searchTerm) {
        return hasContactEmail(searchTerm).or(hasContactPhone(searchTerm));
    }

    public static Specification<DeliveryCompany> hasHighRating(BigDecimal minRating) {
        return hasMinRating(minRating != null ? minRating : new BigDecimal("4.0"));
    }

    public static Specification<DeliveryCompany> isPopular(Long minDeliveries) {
        return hasMinTotalDeliveries(minDeliveries != null ? minDeliveries : 100L);
    }

    public static Specification<DeliveryCompany> hasCapacity(Integer minDrivers) {
        return hasMinActiveDrivers(minDrivers != null ? minDrivers : 5);
    }

    public static Specification<DeliveryCompany> isNewCompany(LocalDateTime since) {
        return createdAfter(since != null ? since : LocalDateTime.now().minusMonths(6));
    }

    public static Specification<DeliveryCompany> servesZones(String zones) {
        return hasManagedZones(zones);
    }

    // Additional methods to match service layer expectations
    public static Specification<DeliveryCompany> hasServiceType(String serviceType) {
        return (root, query, criteriaBuilder) -> {
            if (serviceType == null || serviceType.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryCompany_.vehicleTypesSupported)),
                "%" + serviceType.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryCompany> hasServiceArea(String serviceArea) {
        return hasServiceRegion(serviceArea);
    }

    public static Specification<DeliveryCompany> isVerified(Boolean isVerified) {
        return isLicensed(isVerified);
    }

    public static Specification<DeliveryCompany> hasMinRating(BigDecimal minRating) {
        return (root, query, criteriaBuilder) -> {
            if (minRating == null) {
                return criteriaBuilder.conjunction();
            }
            // Note: Rating field not implemented in entity yet
            // This is a placeholder that always returns true for now
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<DeliveryCompany> hasMaxRating(BigDecimal maxRating) {
        return (root, query, criteriaBuilder) -> {
            if (maxRating == null) {
                return criteriaBuilder.conjunction();
            }
            // Note: Rating field not implemented in entity yet
            // This is a placeholder that always returns true for now
            return criteriaBuilder.conjunction();
        };
    }
}