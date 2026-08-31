package com.upstart.backend.specification;

import com.upstart.backend.entity.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> {
            if (username == null || username.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("username"), username);
        };
    }

    public static Specification<User> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("email"), email);
        };
    }

    public static Specification<User> hasUsernameOrEmail(String usernameOrEmail) {
        return (root, query, criteriaBuilder) -> {
            if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(
                criteriaBuilder.equal(root.get("username"), usernameOrEmail),
                criteriaBuilder.equal(root.get("email"), usernameOrEmail)
            );
        };
    }

    public static Specification<User> hasRole(User.Role role) {
        return (root, query, criteriaBuilder) -> {
            if (role == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("role"), role);
        };
    }

    public static Specification<User> isEnabled(Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            if (enabled == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("isEnabled"), enabled);
        };
    }

    public static Specification<User> hasFirstNameContaining(String firstName) {
        return (root, query, criteriaBuilder) -> {
            if (firstName == null || firstName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("firstName")),
                "%" + firstName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> hasLastNameContaining(String lastName) {
        return (root, query, criteriaBuilder) -> {
            if (lastName == null || lastName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("lastName")),
                "%" + lastName.toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> buildSearchSpecification(String searchTerm, User.Role role, Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search term can match username, email, firstName, or lastName
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String searchPattern = "%" + searchTerm.toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            // Filter by role
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            // Filter by enabled status
            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("isEnabled"), enabled));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}