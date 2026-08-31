package com.upstart.backend.specification;

import com.upstart.backend.entity.Partnership;
import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.entity.DeliveryCompany;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Specifications for Partnership entity queries
 * Provides reusable query criteria for complex partnership filtering
 */
public class PartnershipSpecifications {

    /**
     * Filter partnerships by status
     */
    public static Specification<Partnership> hasStatus(Partnership.PartnershipStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Filter partnerships by vendor company
     */
    public static Specification<Partnership> hasVendorCompany(VendorCompany vendorCompany) {
        return (root, query, criteriaBuilder) -> {
            if (vendorCompany == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("vendorCompany"), vendorCompany);
        };
    }

    /**
     * Filter partnerships by vendor company ID
     */
    public static Specification<Partnership> hasVendorCompanyId(Long vendorCompanyId) {
        return (root, query, criteriaBuilder) -> {
            if (vendorCompanyId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("vendorCompany").get("id"), vendorCompanyId);
        };
    }

    /**
     * Filter partnerships by delivery company
     */
    public static Specification<Partnership> hasDeliveryCompany(DeliveryCompany deliveryCompany) {
        return (root, query, criteriaBuilder) -> {
            if (deliveryCompany == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("deliveryCompany"), deliveryCompany);
        };
    }

    /**
     * Filter partnerships by delivery company ID
     */
    public static Specification<Partnership> hasDeliveryCompanyId(Long deliveryCompanyId) {
        return (root, query, criteriaBuilder) -> {
            if (deliveryCompanyId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("deliveryCompany").get("id"), deliveryCompanyId);
        };
    }

    /**
     * Filter partnerships by exclusivity
     */
    public static Specification<Partnership> isExclusive(Boolean isExclusive) {
        return (root, query, criteriaBuilder) -> {
            if (isExclusive == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("isExclusive"), isExclusive);
        };
    }

    /**
     * Filter partnerships by service area (contains)
     */
    public static Specification<Partnership> hasServiceArea(String serviceArea) {
        return (root, query, criteriaBuilder) -> {
            if (serviceArea == null || serviceArea.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("serviceAreas")),
                "%" + serviceArea.toLowerCase() + "%"
            );
        };
    }

    /**
     * Filter partnerships by commission rate range
     */
    public static Specification<Partnership> hasCommissionRateBetween(BigDecimal minRate, BigDecimal maxRate) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            
            if (minRate != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("commissionRate"), minRate));
            }
            
            if (maxRate != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("commissionRate"), maxRate));
            }
            
            return predicate;
        };
    }

    /**
     * Filter partnerships by minimum order value range
     */
    public static Specification<Partnership> hasMinOrderValueBetween(BigDecimal minValue, BigDecimal maxValue) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            
            if (minValue != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("minOrderValue"), minValue));
            }
            
            if (maxValue != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("minOrderValue"), maxValue));
            }
            
            return predicate;
        };
    }

    /**
     * Filter partnerships by maximum order value range
     */
    public static Specification<Partnership> hasMaxOrderValueBetween(BigDecimal minValue, BigDecimal maxValue) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            
            if (minValue != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("maxOrderValue"), minValue));
            }
            
            if (maxValue != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("maxOrderValue"), maxValue));
            }
            
            return predicate;
        };
    }

    /**
     * Filter partnerships by contract start date range
     */
    public static Specification<Partnership> hasContractStartDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            
            if (startDate != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("contractStartDate"), startDate));
            }
            
            if (endDate != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("contractStartDate"), endDate));
            }
            
            return predicate;
        };
    }

    /**
     * Filter partnerships by contract end date range
     */
    public static Specification<Partnership> hasContractEndDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            
            if (startDate != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.greaterThanOrEqualTo(root.get("contractEndDate"), startDate));
            }
            
            if (endDate != null) {
                predicate = criteriaBuilder.and(predicate,
                    criteriaBuilder.lessThanOrEqualTo(root.get("contractEndDate"), endDate));
            }
            
            return predicate;
        };
    }

    /**
     * Filter partnerships that are currently active (within contract period)
     */
    public static Specification<Partnership> isCurrentlyActive() {
        return (root, query, criteriaBuilder) -> {
            LocalDateTime now = LocalDateTime.now();
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("status"), Partnership.PartnershipStatus.ACTIVE),
                criteriaBuilder.lessThanOrEqualTo(root.get("contractStartDate"), now),
                criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("contractEndDate")),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("contractEndDate"), now)
                )
            );
        };
    }

    /**
     * Filter partnerships that are expiring soon (within specified days)
     */
    public static Specification<Partnership> isExpiringSoon(int days) {
        return (root, query, criteriaBuilder) -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime futureDate = now.plusDays(days);
            
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("status"), Partnership.PartnershipStatus.ACTIVE),
                criteriaBuilder.isNotNull(root.get("contractEndDate")),
                criteriaBuilder.between(root.get("contractEndDate"), now, futureDate)
            );
        };
    }

    /**
     * Filter partnerships by minimum total orders
     */
    public static Specification<Partnership> hasTotalOrdersGreaterThan(Long minOrders) {
        return (root, query, criteriaBuilder) -> {
            if (minOrders == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThan(root.get("totalOrders"), minOrders);
        };
    }

    /**
     * Filter partnerships by minimum total revenue
     */
    public static Specification<Partnership> hasTotalRevenueGreaterThan(BigDecimal minRevenue) {
        return (root, query, criteriaBuilder) -> {
            if (minRevenue == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThan(root.get("totalRevenue"), minRevenue);
        };
    }

    /**
     * Filter partnerships by minimum average rating
     */
    public static Specification<Partnership> hasAverageRatingGreaterThan(BigDecimal minRating) {
        return (root, query, criteriaBuilder) -> {
            if (minRating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThan(root.get("averageRating"), minRating);
        };
    }

    /**
     * Filter partnerships that can handle orders within specified value range
     */
    public static Specification<Partnership> canHandleOrderValue(BigDecimal orderValue) {
        return (root, query, criteriaBuilder) -> {
            if (orderValue == null) {
                return criteriaBuilder.conjunction();
            }
            
            return criteriaBuilder.and(
                criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("minOrderValue")),
                    criteriaBuilder.lessThanOrEqualTo(root.get("minOrderValue"), orderValue)
                ),
                criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("maxOrderValue")),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("maxOrderValue"), orderValue)
                )
            );
        };
    }

    /**
     * Filter partnerships that can handle orders within specified distance range
     */
    public static Specification<Partnership> canHandleDeliveryDistance(BigDecimal distance) {
        return (root, query, criteriaBuilder) -> {
            if (distance == null) {
                return criteriaBuilder.conjunction();
            }
            
            return criteriaBuilder.and(
                criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("minDeliveryDistance")),
                    criteriaBuilder.lessThanOrEqualTo(root.get("minDeliveryDistance"), distance)
                ),
                criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("maxDeliveryDistance")),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("maxDeliveryDistance"), distance)
                )
            );
        };
    }

    /**
     * Filter partnerships by multiple service areas
     */
    public static Specification<Partnership> hasAnyServiceArea(List<String> serviceAreas) {
        return (root, query, criteriaBuilder) -> {
            if (serviceAreas == null || serviceAreas.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            
            Predicate predicate = criteriaBuilder.disjunction();
            for (String area : serviceAreas) {
                if (area != null && !area.trim().isEmpty()) {
                    predicate = criteriaBuilder.or(predicate,
                        criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("serviceAreas")),
                            "%" + area.toLowerCase() + "%"
                        )
                    );
                }
            }
            
            return predicate;
        };
    }

    /**
     * Filter partnerships that are eligible for a specific order
     * Combines multiple criteria: status, order value, distance, service area
     */
    public static Specification<Partnership> isEligibleForOrder(
            BigDecimal orderValue, 
            BigDecimal deliveryDistance, 
            String serviceArea) {
        
        return isCurrentlyActive()
                .and(canHandleOrderValue(orderValue))
                .and(canHandleDeliveryDistance(deliveryDistance))
                .and(hasServiceArea(serviceArea));
    }

    /**
     * Filter partnerships by performance criteria
     */
    public static Specification<Partnership> hasGoodPerformance(
            Long minOrders, 
            BigDecimal minRevenue, 
            BigDecimal minRating) {
        
        return hasTotalOrdersGreaterThan(minOrders)
                .and(hasTotalRevenueGreaterThan(minRevenue))
                .and(hasAverageRatingGreaterThan(minRating));
    }

    /**
     * Complex filter for finding the best partnerships for an order
     */
    public static Specification<Partnership> findBestForOrder(
            Long vendorCompanyId,
            BigDecimal orderValue,
            BigDecimal deliveryDistance,
            String serviceArea,
            Boolean preferExclusive) {
        
        Specification<Partnership> spec = hasVendorCompanyId(vendorCompanyId)
                .and(isEligibleForOrder(orderValue, deliveryDistance, serviceArea));
        
        if (preferExclusive != null && preferExclusive) {
            spec = spec.and(isExclusive(true));
        }
        
        return spec;
    }
}