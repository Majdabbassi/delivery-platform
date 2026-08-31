package com.upstart.backend.specification;

import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.entity.VendorCompany_;
import com.upstart.backend.entity.VendorOwner;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VendorCompanySpecifications {

    public static Specification<VendorCompany> hasCompanyName(String companyName) {
        return (root, query, criteriaBuilder) -> {
            if (companyName == null || companyName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(VendorCompany_.companyName)),
                "%" + companyName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorCompany> hasBusinessAddress(String businessAddress) {
        return (root, query, criteriaBuilder) -> {
            if (businessAddress == null || businessAddress.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(VendorCompany_.businessAddress)),
                "%" + businessAddress.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorCompany> hasBusinessLicense(String businessLicense) {
        return (root, query, criteriaBuilder) -> {
            if (businessLicense == null || businessLicense.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(VendorCompany_.businessLicense),
                businessLicense
            );
        };
    }

    public static Specification<VendorCompany> hasContactEmail(String contactEmail) {
        return (root, query, criteriaBuilder) -> {
            if (contactEmail == null || contactEmail.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(VendorCompany_.contactEmail)),
                "%" + contactEmail.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorCompany> hasContactPhone(String contactPhone) {
        return (root, query, criteriaBuilder) -> {
            if (contactPhone == null || contactPhone.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(VendorCompany_.contactPhone),
                "%" + contactPhone + "%"
            );
        };
    }

    public static Specification<VendorCompany> hasBusinessDescription(String businessDescription) {
        return (root, query, criteriaBuilder) -> {
            if (businessDescription == null || businessDescription.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(VendorCompany_.businessDescription)),
                "%" + businessDescription.toLowerCase() + "%"
            );
        };
    }

    // Note: serviceArea field does not exist in VendorCompany entity
    // This method has been removed as it references a non-existent field

    public static Specification<VendorCompany> isActive(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(VendorCompany_.isActive),
                isActive
            );
        };
    }

    public static Specification<VendorCompany> isVerified(Boolean isVerified) {
        return (root, query, criteriaBuilder) -> {
            if (isVerified == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(VendorCompany_.isVerified),
                isVerified
            );
        };
    }

    public static Specification<VendorCompany> hasOwner(VendorOwner owner) {
        return (root, query, criteriaBuilder) -> {
            if (owner == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(VendorCompany_.owner),
                owner
            );
        };
    }

    public static Specification<VendorCompany> hasOwnerId(Long ownerId) {
        return (root, query, criteriaBuilder) -> {
            if (ownerId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(VendorCompany_.owner).get("id"),
                ownerId
            );
        };
    }

    public static Specification<VendorCompany> hasMinRating(BigDecimal minRating) {
        return (root, query, criteriaBuilder) -> {
            if (minRating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(VendorCompany_.rating),
                minRating
            );
        };
    }

    public static Specification<VendorCompany> hasMaxRating(BigDecimal maxRating) {
        return (root, query, criteriaBuilder) -> {
            if (maxRating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(VendorCompany_.rating),
                maxRating
            );
        };
    }

    public static Specification<VendorCompany> hasMinTotalOrders(Long minTotalOrders) {
        return (root, query, criteriaBuilder) -> {
            if (minTotalOrders == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(VendorCompany_.totalOrders),
                minTotalOrders
            );
        };
    }

    public static Specification<VendorCompany> hasMaxTotalOrders(Long maxTotalOrders) {
        return (root, query, criteriaBuilder) -> {
            if (maxTotalOrders == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(VendorCompany_.totalOrders),
                maxTotalOrders
            );
        };
    }

    public static Specification<VendorCompany> hasMinTotalRevenue(BigDecimal minRevenue) {
        return (root, query, criteriaBuilder) -> {
            if (minRevenue == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(VendorCompany_.totalRevenue),
                minRevenue
            );
        };
    }

    public static Specification<VendorCompany> hasMaxTotalRevenue(BigDecimal maxRevenue) {
        return (root, query, criteriaBuilder) -> {
            if (maxRevenue == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(VendorCompany_.totalRevenue),
                maxRevenue
            );
        };
    }

    public static Specification<VendorCompany> hasMinCommissionRate(BigDecimal minRate) {
        return (root, query, criteriaBuilder) -> {
            if (minRate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(VendorCompany_.commissionRate),
                minRate
            );
        };
    }

    public static Specification<VendorCompany> hasMaxCommissionRate(BigDecimal maxRate) {
        return (root, query, criteriaBuilder) -> {
            if (maxRate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(VendorCompany_.commissionRate),
                maxRate
            );
        };
    }

    public static Specification<VendorCompany> registeredAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(VendorCompany_.registrationDate),
                dateTime
            );
        };
    }

    public static Specification<VendorCompany> registeredBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(VendorCompany_.registrationDate),
                dateTime
            );
        };
    }

    public static Specification<VendorCompany> createdAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(VendorCompany_.createdAt),
                dateTime
            );
        };
    }

    public static Specification<VendorCompany> createdBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(VendorCompany_.createdAt),
                dateTime
            );
        };
    }

    // Combined specifications for common use cases
    public static Specification<VendorCompany> isActiveAndVerified() {
        return isActive(true).and(isVerified(true));
    }

    public static Specification<VendorCompany> searchByNameOrDescription(String searchTerm) {
        return hasCompanyName(searchTerm).or(hasBusinessDescription(searchTerm));
    }

    public static Specification<VendorCompany> searchByContact(String searchTerm) {
        return hasContactEmail(searchTerm).or(hasContactPhone(searchTerm));
    }

    public static Specification<VendorCompany> hasHighRating(BigDecimal minRating) {
        return hasMinRating(minRating != null ? minRating : new BigDecimal("4.0"));
    }

    public static Specification<VendorCompany> isPopular(Long minOrders) {
        return hasMinTotalOrders(minOrders != null ? minOrders : 100L);
    }

    public static Specification<VendorCompany> isNewCompany(LocalDateTime since) {
        return createdAfter(since != null ? since : LocalDateTime.now().minusMonths(6));
    }
}