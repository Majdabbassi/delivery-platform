package com.upstart.backend.specification;

import com.upstart.backend.entity.VendorOwner;
import com.upstart.backend.entity.VendorOwner_;
import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.entity.VendorCompany_;
import com.upstart.backend.entity.User_;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VendorOwnerSpecifications {

    public static Specification<VendorOwner> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> {
            if (username == null || username.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(User_.username)),
                "%" + username.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorOwner> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(User_.email)),
                "%" + email.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorOwner> hasFirstName(String firstName) {
        return (root, query, criteriaBuilder) -> {
            if (firstName == null || firstName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(User_.firstName)),
                "%" + firstName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorOwner> hasLastName(String lastName) {
        return (root, query, criteriaBuilder) -> {
            if (lastName == null || lastName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(User_.lastName)),
                "%" + lastName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorOwner> hasPhoneNumber(String phoneNumber) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(VendorOwner_.phoneNumber),
                "%" + phoneNumber + "%"
            );
        };
    }

    public static Specification<VendorOwner> hasNationalId(String nationalId) {
        return (root, query, criteriaBuilder) -> {
            if (nationalId == null || nationalId.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(VendorOwner_.nationalId),
                nationalId
            );
        };
    }

    public static Specification<VendorOwner> isEnabled(Boolean isEnabled) {
        return (root, query, criteriaBuilder) -> {
            if (isEnabled == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(User_.isEnabled),
                isEnabled
            );
        };
    }

    public static Specification<VendorOwner> isVerifiedOwner(Boolean isVerified) {
        return (root, query, criteriaBuilder) -> {
            if (isVerified == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(VendorOwner_.isVerifiedOwner),
                isVerified
            );
        };
    }

    public static Specification<VendorOwner> hasPreferredBusinessCategory(String category) {
        return (root, query, criteriaBuilder) -> {
            if (category == null || category.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(VendorOwner_.preferredBusinessCategory)),
                "%" + category.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorOwner> hasMinBusinessExperience(Integer minYears) {
        return (root, query, criteriaBuilder) -> {
            if (minYears == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(VendorOwner_.businessExperienceYears),
                minYears
            );
        };
    }

    public static Specification<VendorOwner> hasMaxBusinessExperience(Integer maxYears) {
        return (root, query, criteriaBuilder) -> {
            if (maxYears == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(VendorOwner_.businessExperienceYears),
                maxYears
            );
        };
    }

    public static Specification<VendorOwner> createdAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(User_.createdAt),
                dateTime
            );
        };
    }

    public static Specification<VendorOwner> createdBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(User_.createdAt),
                dateTime
            );
        };
    }

    public static Specification<VendorOwner> bornAfter(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(VendorOwner_.dateOfBirth),
                date
            );
        };
    }

    public static Specification<VendorOwner> bornBefore(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(VendorOwner_.dateOfBirth),
                date
            );
        };
    }

    public static Specification<VendorOwner> hasMinCompanies(Integer minCompanies) {
        return (root, query, criteriaBuilder) -> {
            if (minCompanies == null) {
                return criteriaBuilder.conjunction();
            }
            Subquery<Long> sub = query.subquery(Long.class);
            Root<VendorCompany> company = sub.from(VendorCompany.class);
            sub.select(criteriaBuilder.count(company));
            sub.where(criteriaBuilder.equal(company.get(VendorCompany_.owner), root));
            return criteriaBuilder.greaterThanOrEqualTo(sub, minCompanies.longValue());
        };
    }

    public static Specification<VendorOwner> hasMaxCompanies(Integer maxCompanies) {
        return (root, query, criteriaBuilder) -> {
            if (maxCompanies == null) {
                return criteriaBuilder.conjunction();
            }
            Subquery<Long> sub = query.subquery(Long.class);
            Root<VendorCompany> company = sub.from(VendorCompany.class);
            sub.select(criteriaBuilder.count(company));
            sub.where(criteriaBuilder.equal(company.get(VendorCompany_.owner), root));
            return criteriaBuilder.lessThanOrEqualTo(sub, maxCompanies.longValue());
        };
    }

    public static Specification<VendorOwner> hasAddress(String address) {
        return (root, query, criteriaBuilder) -> {
            if (address == null || address.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(VendorOwner_.address)),
                "%" + address.toLowerCase() + "%"
            );
        };
    }

    public static Specification<VendorOwner> hasEmergencyContact(String emergencyContact) {
        return (root, query, criteriaBuilder) -> {
            if (emergencyContact == null || emergencyContact.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(VendorOwner_.emergencyContact),
                "%" + emergencyContact + "%"
            );
        };
    }

    // Combined specifications for common use cases
    public static Specification<VendorOwner> isActiveAndVerified() {
        return isEnabled(true).and(isVerifiedOwner(true));
    }

    public static Specification<VendorOwner> isVerified(Boolean isVerified) {
        return isVerifiedOwner(isVerified);
    }

    public static Specification<VendorOwner> isExperienced() {
        return hasMinBusinessExperience(2); // Default to 2+ years as experienced
    }

    public static Specification<VendorOwner> isNewOwner(LocalDateTime since) {
        return createdAfter(since);
    }

    public static Specification<VendorOwner> hasMultipleCompanies() {
        return hasMinCompanies(2);
    }

    public static Specification<VendorOwner> searchByName(String searchTerm) {
        return hasFirstName(searchTerm).or(hasLastName(searchTerm));
    }

    public static Specification<VendorOwner> searchByContact(String searchTerm) {
        return hasPhoneNumber(searchTerm).or(hasEmail(searchTerm));
    }
}