package com.swiftdeliver.backend.specification;

import com.swiftdeliver.backend.entity.DeliveryOwner;
import com.swiftdeliver.backend.entity.DeliveryOwner_;
import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.DeliveryCompany_;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DeliveryOwnerSpecifications {

    public static Specification<DeliveryOwner> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> {
            if (username == null || username.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryOwner_.username)),
                "%" + username.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryOwner> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryOwner_.email)),
                "%" + email.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryOwner> hasFirstName(String firstName) {
        return (root, query, criteriaBuilder) -> {
            if (firstName == null || firstName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryOwner_.firstName)),
                "%" + firstName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryOwner> hasLastName(String lastName) {
        return (root, query, criteriaBuilder) -> {
            if (lastName == null || lastName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryOwner_.lastName)),
                "%" + lastName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryOwner> hasPhoneNumber(String phoneNumber) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(DeliveryOwner_.phoneNumber),
                "%" + phoneNumber + "%"
            );
        };
    }

    public static Specification<DeliveryOwner> hasNationalId(String nationalId) {
        return (root, query, criteriaBuilder) -> {
            if (nationalId == null || nationalId.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(DeliveryOwner_.nationalId),
                nationalId
            );
        };
    }

    public static Specification<DeliveryOwner> isEnabled(Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            if (enabled == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(DeliveryOwner_.isEnabled),
                enabled
            );
        };
    }

    public static Specification<DeliveryOwner> isVerified(Boolean verified) {
        return (root, query, criteriaBuilder) -> {
            if (verified == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(DeliveryOwner_.isVerifiedOwner),
                verified
            );
        };
    }

    public static Specification<DeliveryOwner> hasPreferredServiceRegions(String region) {
        return (root, query, criteriaBuilder) -> {
            if (region == null || region.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryOwner_.preferredServiceRegions)),
                "%" + region.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryOwner> hasMinLogisticsExperience(Integer minExperience) {
        return (root, query, criteriaBuilder) -> {
            if (minExperience == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryOwner_.logisticsExperienceYears),
                minExperience
            );
        };
    }

    public static Specification<DeliveryOwner> hasMaxLogisticsExperience(Integer maxExperience) {
        return (root, query, criteriaBuilder) -> {
            if (maxExperience == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryOwner_.logisticsExperienceYears),
                maxExperience
            );
        };
    }

    public static Specification<DeliveryOwner> createdAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryOwner_.createdAt),
                dateTime
            );
        };
    }

    public static Specification<DeliveryOwner> createdBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryOwner_.createdAt),
                dateTime
            );
        };
    }

    public static Specification<DeliveryOwner> bornAfter(LocalDate birthDate) {
        return (root, query, criteriaBuilder) -> {
            if (birthDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DeliveryOwner_.dateOfBirth),
                birthDate
            );
        };
    }

    public static Specification<DeliveryOwner> bornBefore(LocalDate birthDate) {
        return (root, query, criteriaBuilder) -> {
            if (birthDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DeliveryOwner_.dateOfBirth),
                birthDate
            );
        };
    }

    public static Specification<DeliveryOwner> hasMinCompanies(Long minCompanies) {
        return (root, query, criteriaBuilder) -> {
            if (minCompanies == null) {
                return criteriaBuilder.conjunction();
            }
            Subquery<Long> sub = query.subquery(Long.class);
            Root<DeliveryCompany> company = sub.from(DeliveryCompany.class);
            sub.select(criteriaBuilder.count(company));
            sub.where(criteriaBuilder.equal(company.get(DeliveryCompany_.owner), root));
            return criteriaBuilder.greaterThanOrEqualTo(sub, minCompanies);
        };
    }

    public static Specification<DeliveryOwner> hasMaxCompanies(Long maxCompanies) {
        return (root, query, criteriaBuilder) -> {
            if (maxCompanies == null) {
                return criteriaBuilder.conjunction();
            }
            Subquery<Long> sub = query.subquery(Long.class);
            Root<DeliveryCompany> company = sub.from(DeliveryCompany.class);
            sub.select(criteriaBuilder.count(company));
            sub.where(criteriaBuilder.equal(company.get(DeliveryCompany_.owner), root));
            return criteriaBuilder.lessThanOrEqualTo(sub, maxCompanies);
        };
    }

    public static Specification<DeliveryOwner> hasAddress(String address) {
        return (root, query, criteriaBuilder) -> {
            if (address == null || address.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DeliveryOwner_.address)),
                "%" + address.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DeliveryOwner> hasEmergencyContact(String emergencyContact) {
        return (root, query, criteriaBuilder) -> {
            if (emergencyContact == null || emergencyContact.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(DeliveryOwner_.emergencyContact),
                "%" + emergencyContact + "%"
            );
        };
    }

    // Combined specifications for common use cases
    public static Specification<DeliveryOwner> isActiveAndVerified() {
        return isEnabled(true).and(isVerified(true));
    }

    public static Specification<DeliveryOwner> searchByName(String searchTerm) {
        return hasFirstName(searchTerm).or(hasLastName(searchTerm));
    }

    public static Specification<DeliveryOwner> searchByContact(String searchTerm) {
        return hasEmail(searchTerm).or(hasPhoneNumber(searchTerm)).or(hasEmergencyContact(searchTerm));
    }

    public static Specification<DeliveryOwner> hasExperience(Integer minYears) {
        return hasMinLogisticsExperience(minYears != null ? minYears : 1);
    }

    public static Specification<DeliveryOwner> isExperienced() {
        return hasMinLogisticsExperience(3);
    }

    public static Specification<DeliveryOwner> hasMultipleCompanies() {
        return hasMinCompanies(2L);
    }

    public static Specification<DeliveryOwner> isNewOwner(LocalDateTime since) {
        return createdAfter(since != null ? since : LocalDateTime.now().minusMonths(3));
    }
}