package com.swiftdeliver.backend.specification;

import com.swiftdeliver.backend.entity.CustomerUser;
import com.swiftdeliver.backend.entity.CustomerUser_;
import com.swiftdeliver.backend.entity.User_;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerUserSpecifications {

    public static Specification<CustomerUser> hasUsername(String username) {
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

    public static Specification<CustomerUser> hasEmail(String email) {
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

    public static Specification<CustomerUser> hasFirstName(String firstName) {
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

    public static Specification<CustomerUser> hasLastName(String lastName) {
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

    public static Specification<CustomerUser> hasPhoneNumber(String phoneNumber) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(CustomerUser_.phoneNumber),
                "%" + phoneNumber + "%"
            );
        };
    }

    public static Specification<CustomerUser> isPremium(Boolean isPremium) {
        return (root, query, criteriaBuilder) -> {
            if (isPremium == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(CustomerUser_.isPremium), isPremium);
        };
    }

    public static Specification<CustomerUser> isEnabled(Boolean isEnabled) {
        return (root, query, criteriaBuilder) -> {
            if (isEnabled == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(User_.isEnabled), isEnabled);
        };
    }

    public static Specification<CustomerUser> hasMinLoyaltyPoints(Integer minLoyaltyPoints) {
        return (root, query, criteriaBuilder) -> {
            if (minLoyaltyPoints == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.<Integer>get(CustomerUser_.loyaltyPoints), minLoyaltyPoints
            );
        };
    }

    public static Specification<CustomerUser> hasMaxLoyaltyPoints(Integer maxLoyaltyPoints) {
        return (root, query, criteriaBuilder) -> {
            if (maxLoyaltyPoints == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.<Integer>get(CustomerUser_.loyaltyPoints), maxLoyaltyPoints
            );
        };
    }

    public static Specification<CustomerUser> hasMinTotalSpent(BigDecimal minTotalSpent) {
        return (root, query, criteriaBuilder) -> {
            if (minTotalSpent == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.<BigDecimal>get(CustomerUser_.totalSpent), minTotalSpent
            );
        };
    }

    public static Specification<CustomerUser> hasMaxTotalSpent(BigDecimal maxTotalSpent) {
        return (root, query, criteriaBuilder) -> {
            if (maxTotalSpent == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.<BigDecimal>get(CustomerUser_.totalSpent), maxTotalSpent
            );
        };
    }

    public static Specification<CustomerUser> hasMinTotalOrders(Integer minTotalOrders) {
        return (root, query, criteriaBuilder) -> {
            if (minTotalOrders == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.<Long>get(CustomerUser_.totalOrders), minTotalOrders.longValue()
            );
        };
    }

    public static Specification<CustomerUser> hasMaxTotalOrders(Integer maxTotalOrders) {
        return (root, query, criteriaBuilder) -> {
            if (maxTotalOrders == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.<Long>get(CustomerUser_.totalOrders), maxTotalOrders.longValue()
            );
        };
    }

    public static Specification<CustomerUser> hasGender(String gender) {
        return (root, query, criteriaBuilder) -> {
            if (gender == null || gender.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                criteriaBuilder.lower(root.get(CustomerUser_.gender)),
                gender.toLowerCase()
            );
        };
    }

    public static Specification<CustomerUser> hasPreferredPaymentMethod(String paymentMethod) {
        return (root, query, criteriaBuilder) -> {
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                criteriaBuilder.lower(root.get(CustomerUser_.preferredPaymentMethod)),
                paymentMethod.toLowerCase()
            );
        };
    }

    public static Specification<CustomerUser> createdAfter(LocalDateTime createdAfter) {
        return (root, query, criteriaBuilder) -> {
            if (createdAfter == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.<LocalDateTime>get(User_.createdAt), createdAfter
            );
        };
    }

    public static Specification<CustomerUser> createdBefore(LocalDateTime createdBefore) {
        return (root, query, criteriaBuilder) -> {
            if (createdBefore == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.<LocalDateTime>get(User_.createdAt), createdBefore
            );
        };
    }

    public static Specification<CustomerUser> bornAfter(LocalDate bornAfter) {
        return (root, query, criteriaBuilder) -> {
            if (bornAfter == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.<LocalDate>get(CustomerUser_.dateOfBirth), bornAfter
            );
        };
    }

    public static Specification<CustomerUser> bornBefore(LocalDate bornBefore) {
        return (root, query, criteriaBuilder) -> {
            if (bornBefore == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.<LocalDate>get(CustomerUser_.dateOfBirth), bornBefore
            );
        };
    }

    public static Specification<CustomerUser> hasDefaultAddress(String address) {
        return (root, query, criteriaBuilder) -> {
            if (address == null || address.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(CustomerUser_.defaultAddress)),
                "%" + address.toLowerCase() + "%"
            );
        };
    }

    public static Specification<CustomerUser> birthDateAfter(LocalDate birthDateAfter) {
        return (root, query, criteriaBuilder) -> {
            if (birthDateAfter == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.<LocalDate>get(CustomerUser_.dateOfBirth), birthDateAfter
            );
        };
    }

    public static Specification<CustomerUser> birthDateBefore(LocalDate birthDateBefore) {
        return (root, query, criteriaBuilder) -> {
            if (birthDateBefore == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.<LocalDate>get(CustomerUser_.dateOfBirth), birthDateBefore
            );
        };
    }
}