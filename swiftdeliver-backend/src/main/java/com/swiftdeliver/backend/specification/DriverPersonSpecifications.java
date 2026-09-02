package com.swiftdeliver.backend.specification;

import com.swiftdeliver.backend.entity.DriverPerson;
import com.swiftdeliver.backend.entity.DriverPerson_;
import com.swiftdeliver.backend.entity.User_;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DriverPersonSpecifications {

    public static Specification<DriverPerson> hasUsername(String username) {
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

    public static Specification<DriverPerson> hasEmail(String email) {
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

    public static Specification<DriverPerson> hasFirstName(String firstName) {
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

    public static Specification<DriverPerson> hasLastName(String lastName) {
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

    public static Specification<DriverPerson> hasLicenseNumber(String licenseNumber) {
        return (root, query, criteriaBuilder) -> {
            if (licenseNumber == null || licenseNumber.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DriverPerson_.licenseNumber)),
                "%" + licenseNumber.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DriverPerson> hasVehiclePlate(String vehiclePlate) {
        return (root, query, criteriaBuilder) -> {
            if (vehiclePlate == null || vehiclePlate.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DriverPerson_.vehiclePlate)),
                "%" + vehiclePlate.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DriverPerson> hasPhoneNumber(String phoneNumber) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                root.get(DriverPerson_.phoneNumber),
                "%" + phoneNumber + "%"
            );
        };
    }

    public static Specification<DriverPerson> isAvailable(Boolean isAvailable) {
        return (root, query, criteriaBuilder) -> {
            if (isAvailable == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(DriverPerson_.isAvailable), isAvailable);
        };
    }

    public static Specification<DriverPerson> isVerified(Boolean isVerified) {
        return (root, query, criteriaBuilder) -> {
            if (isVerified == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(DriverPerson_.isVerified), isVerified);
        };
    }

    public static Specification<DriverPerson> isEnabled(Boolean isEnabled) {
        return (root, query, criteriaBuilder) -> {
            if (isEnabled == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(User_.isEnabled), isEnabled);
        };
    }

    public static Specification<DriverPerson> hasVehicleType(DriverPerson.VehicleType vehicleType) {
        return (root, query, criteriaBuilder) -> {
            if (vehicleType == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(DriverPerson_.vehicleType), vehicleType);
        };
    }

    public static Specification<DriverPerson> hasDeliveryZone(String deliveryZone) {
        return (root, query, criteriaBuilder) -> {
            if (deliveryZone == null || deliveryZone.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DriverPerson_.deliveryZone)),
                "%" + deliveryZone.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DriverPerson> hasCurrentLocation(String currentLocation) {
        return (root, query, criteriaBuilder) -> {
            if (currentLocation == null || currentLocation.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DriverPerson_.currentLocation)),
                "%" + currentLocation.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DriverPerson> hasMinRating(BigDecimal minRating) {
        return (root, query, criteriaBuilder) -> {
            if (minRating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DriverPerson_.rating), minRating
            );
        };
    }

    public static Specification<DriverPerson> hasMaxRating(BigDecimal maxRating) {
        return (root, query, criteriaBuilder) -> {
            if (maxRating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DriverPerson_.rating), maxRating
            );
        };
    }

    public static Specification<DriverPerson> hasMinTotalDeliveries(Long minTotalDeliveries) {
        return (root, query, criteriaBuilder) -> {
            if (minTotalDeliveries == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DriverPerson_.totalDeliveries), minTotalDeliveries
            );
        };
    }

    public static Specification<DriverPerson> hasMaxTotalDeliveries(Long maxTotalDeliveries) {
        return (root, query, criteriaBuilder) -> {
            if (maxTotalDeliveries == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DriverPerson_.totalDeliveries), maxTotalDeliveries
            );
        };
    }

    public static Specification<DriverPerson> hasMinTotalEarnings(BigDecimal minTotalEarnings) {
        return (root, query, criteriaBuilder) -> {
            if (minTotalEarnings == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DriverPerson_.totalEarnings), minTotalEarnings
            );
        };
    }

    public static Specification<DriverPerson> hasMaxTotalEarnings(BigDecimal maxTotalEarnings) {
        return (root, query, criteriaBuilder) -> {
            if (maxTotalEarnings == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DriverPerson_.totalEarnings), maxTotalEarnings
            );
        };
    }

    public static Specification<DriverPerson> hasVehicleModel(String vehicleModel) {
        return (root, query, criteriaBuilder) -> {
            if (vehicleModel == null || vehicleModel.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DriverPerson_.vehicleModel)),
                "%" + vehicleModel.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DriverPerson> hasVehicleColor(String vehicleColor) {
        return (root, query, criteriaBuilder) -> {
            if (vehicleColor == null || vehicleColor.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(DriverPerson_.vehicleColor)),
                "%" + vehicleColor.toLowerCase() + "%"
            );
        };
    }

    public static Specification<DriverPerson> lastActiveAfter(LocalDateTime lastActiveAfter) {
        return (root, query, criteriaBuilder) -> {
            if (lastActiveAfter == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(DriverPerson_.lastActive), lastActiveAfter
            );
        };
    }

    public static Specification<DriverPerson> lastActiveBefore(LocalDateTime lastActiveBefore) {
        return (root, query, criteriaBuilder) -> {
            if (lastActiveBefore == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(DriverPerson_.lastActive), lastActiveBefore
            );
        };
    }

    public static Specification<DriverPerson> createdAfter(LocalDateTime createdAfter) {
        return (root, query, criteriaBuilder) -> {
            if (createdAfter == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                root.get(User_.createdAt), createdAfter
            );
        };
    }

    public static Specification<DriverPerson> createdBefore(LocalDateTime createdBefore) {
        return (root, query, criteriaBuilder) -> {
            if (createdBefore == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(
                root.get(User_.createdAt), createdBefore
            );
        };
    }

    public static Specification<DriverPerson> isAvailableAndVerified() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get(User_.isEnabled), true),
                criteriaBuilder.equal(root.get(DriverPerson_.isVerified), true),
                criteriaBuilder.equal(root.get(DriverPerson_.isAvailable), true)
            );
        };
    }

    public static Specification<DriverPerson> isPendingVerification() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get(User_.isEnabled), true),
                criteriaBuilder.equal(root.get(DriverPerson_.isVerified), false)
            );
        };
    }

    public static Specification<DriverPerson> isAvailableInZone(String deliveryZone) {
        return (root, query, criteriaBuilder) -> {
            if (deliveryZone == null || deliveryZone.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get(User_.isEnabled), true),
                criteriaBuilder.equal(root.get(DriverPerson_.isVerified), true),
                criteriaBuilder.equal(root.get(DriverPerson_.isAvailable), true),
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(DriverPerson_.deliveryZone)),
                    "%" + deliveryZone.toLowerCase() + "%"
                )
            );
        };
    }

    public static Specification<DriverPerson> hasNameContaining(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            String searchTerm = "%" + name.toLowerCase() + "%";
            return criteriaBuilder.or(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(User_.firstName)),
                    searchTerm
                ),
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(User_.lastName)),
                    searchTerm
                ),
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(User_.username)),
                    searchTerm
                )
            );
        };
    }

    public static Specification<DriverPerson> orderByRatingDesc() {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get(DriverPerson_.rating)));
            }
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<DriverPerson> orderByTotalDeliveriesDesc() {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get(DriverPerson_.totalDeliveries)));
            }
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<DriverPerson> isActiveDriverSince(LocalDateTime since) {
        return (root, query, criteriaBuilder) -> {
            if (since == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get(User_.isEnabled), true),
                criteriaBuilder.greaterThanOrEqualTo(root.get(DriverPerson_.lastActive), since)
            );
        };
    }

    public static Specification<DriverPerson> isActiveDriverPersonSince(LocalDateTime since) {
        return isActiveDriverSince(since);
    }
}