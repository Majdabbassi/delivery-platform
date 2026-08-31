package com.upstart.backend.specification;

import com.upstart.backend.entity.SuperAdmin;
import com.upstart.backend.entity.User_;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class SuperAdminSpecifications {

    public static Specification<SuperAdmin> hasUsername(String username) {
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

    public static Specification<SuperAdmin> hasEmail(String email) {
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

    public static Specification<SuperAdmin> hasFirstName(String firstName) {
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

    public static Specification<SuperAdmin> hasLastName(String lastName) {
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

    public static Specification<SuperAdmin> hasPhoneNumber(String phoneNumber) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(User_.phoneNumber)),
                "%" + phoneNumber.toLowerCase() + "%"
            );
        };
    }

    public static Specification<SuperAdmin> isEnabled(Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            if (enabled == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(User_.isEnabled), enabled);
        };
    }

    public static Specification<SuperAdmin> hasMinSecurityLevel(Integer minSecurityLevel) {
        return (root, query, criteriaBuilder) -> {
            if (minSecurityLevel == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("securityLevel"), minSecurityLevel);
        };
    }

    public static Specification<SuperAdmin> hasMaxSecurityLevel(Integer maxSecurityLevel) {
        return (root, query, criteriaBuilder) -> {
            if (maxSecurityLevel == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("securityLevel"), maxSecurityLevel);
        };
    }

    public static Specification<SuperAdmin> hasSystemPermissions(String systemPermissions) {
        return (root, query, criteriaBuilder) -> {
            if (systemPermissions == null || systemPermissions.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("systemPermissions")),
                "%" + systemPermissions.toLowerCase() + "%"
            );
        };
    }

    public static Specification<SuperAdmin> lastAccessAfter(LocalDateTime lastAccessAfter) {
        return (root, query, criteriaBuilder) -> {
            if (lastAccessAfter == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("lastSystemAccess"), lastAccessAfter);
        };
    }

    public static Specification<SuperAdmin> lastAccessBefore(LocalDateTime lastAccessBefore) {
        return (root, query, criteriaBuilder) -> {
            if (lastAccessBefore == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("lastSystemAccess"), lastAccessBefore);
        };
    }

    public static Specification<SuperAdmin> createdAfter(LocalDateTime createdAfter) {
        return (root, query, criteriaBuilder) -> {
            if (createdAfter == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(User_.createdAt), createdAfter);
        };
    }

    public static Specification<SuperAdmin> createdBefore(LocalDateTime createdBefore) {
        return (root, query, criteriaBuilder) -> {
            if (createdBefore == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(User_.createdAt), createdBefore);
        };
    }

    public static Specification<SuperAdmin> hasSecurityLevel(Integer securityLevel) {
        return (root, query, criteriaBuilder) -> {
            if (securityLevel == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("securityLevel"), securityLevel);
        };
    }

    public static Specification<SuperAdmin> hasRecentAccess(int days) {
        return (root, query, criteriaBuilder) -> {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            return criteriaBuilder.greaterThanOrEqualTo(root.get("lastSystemAccess"), cutoffDate);
        };
    }

    public static Specification<SuperAdmin> isActive() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get(User_.isEnabled), true),
                criteriaBuilder.isNotNull(root.get("lastSystemAccess"))
            );
        };
    }

    public static Specification<SuperAdmin> hasHighSecurityLevel() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.greaterThanOrEqualTo(root.get("securityLevel"), 8);
        };
    }

    public static Specification<SuperAdmin> hasPermission(String permission) {
        return (root, query, criteriaBuilder) -> {
            if (permission == null || permission.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("systemPermissions")),
                "%" + permission.toLowerCase() + "%"
            );
        };
    }
}