package com.upstart.backend.specification;

import com.upstart.backend.entity.Admin;
import com.upstart.backend.entity.Admin_;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminSpecifications {

    public static Specification<Admin> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> {
            if (username == null || username.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Admin_.username)),
                "%" + username.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Admin> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Admin_.email)),
                "%" + email.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Admin> hasFirstName(String firstName) {
        return (root, query, criteriaBuilder) -> {
            if (firstName == null || firstName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Admin_.firstName)),
                "%" + firstName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Admin> hasLastName(String lastName) {
        return (root, query, criteriaBuilder) -> {
            if (lastName == null || lastName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Admin_.lastName)),
                "%" + lastName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Admin> hasPhoneNumber(String phoneNumber) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(Admin_.phoneNumber),
                "%" + phoneNumber + "%"
            );
        };
    }

    public static Specification<Admin> hasRole(Admin.AdminRole role) {
        return (root, query, criteriaBuilder) -> {
            if (role == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Admin_.role), role);
        };
    }

    public static Specification<Admin> isEnabled(Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            if (enabled == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Admin_.enabled), enabled);
        };
    }

    public static Specification<Admin> isVerified(Boolean verified) {
        return (root, query, criteriaBuilder) -> {
            if (verified == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Admin_.verified), verified);
        };
    }

    public static Specification<Admin> hasDepartment(String department) {
        return (root, query, criteriaBuilder) -> {
            if (department == null || department.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Admin_.department)),
                "%" + department.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Admin> hasPosition(String position) {
        return (root, query, criteriaBuilder) -> {
            if (position == null || position.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Admin_.position)),
                "%" + position.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Admin> hasNationalId(String nationalId) {
        return (root, query, criteriaBuilder) -> {
            if (nationalId == null || nationalId.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Admin_.nationalId), nationalId);
        };
    }

    public static Specification<Admin> hasAddress(String address) {
        return (root, query, criteriaBuilder) -> {
            if (address == null || address.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Admin_.address)),
                "%" + address.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Admin> hasEmergencyContact(String emergencyContact) {
        return (root, query, criteriaBuilder) -> {
            if (emergencyContact == null || emergencyContact.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(Admin_.emergencyContact),
                "%" + emergencyContact + "%"
            );
        };
    }

    public static Specification<Admin> isActive(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Admin_.isActive), isActive);
        };
    }

    public static Specification<Admin> hasMinSalary(BigDecimal minSalary) {
        return (root, query, criteriaBuilder) -> {
            if (minSalary == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Admin_.salary), minSalary);
        };
    }

    public static Specification<Admin> hasMaxSalary(BigDecimal maxSalary) {
        return (root, query, criteriaBuilder) -> {
            if (maxSalary == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(Admin_.salary), maxSalary);
        };
    }

    public static Specification<Admin> createdAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Admin_.createdAt), dateTime);
        };
    }

    public static Specification<Admin> createdBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(Admin_.createdAt), dateTime);
        };
    }

    public static Specification<Admin> hiredAfter(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Admin_.hireDate), date);
        };
    }

    public static Specification<Admin> hiredBefore(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(Admin_.hireDate), date);
        };
    }

    public static Specification<Admin> bornAfter(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Admin_.dateOfBirth), date);
        };
    }

    public static Specification<Admin> bornBefore(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(Admin_.dateOfBirth), date);
        };
    }

    public static Specification<Admin> isActiveAndVerified() {
        return isActive(true).and(isVerified(true));
    }

    public static Specification<Admin> searchByName(String searchTerm) {
        return hasFirstName(searchTerm).or(hasLastName(searchTerm));
    }

    public static Specification<Admin> searchByContact(String searchTerm) {
        return hasPhoneNumber(searchTerm).or(hasEmail(searchTerm));
    }
}