package com.swiftdeliver.backend.specification;

import com.swiftdeliver.backend.entity.Product;
import com.swiftdeliver.backend.entity.Product_;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductSpecifications {

    public static Specification<Product> hasName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Product_.name)),
                "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasDescription(String description) {
        return (root, query, criteriaBuilder) -> {
            if (description == null || description.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Product_.description)),
                "%" + description.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> {
            if (category == null || category.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Product_.category)),
                "%" + category.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasSubcategory(String subcategory) {
        return (root, query, criteriaBuilder) -> {
            if (subcategory == null || subcategory.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Product_.subcategory)),
                "%" + subcategory.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasBrand(String brand) {
        return (root, query, criteriaBuilder) -> {
            if (brand == null || brand.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Product_.brand)),
                "%" + brand.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasSku(String sku) {
        return (root, query, criteriaBuilder) -> {
            if (sku == null || sku.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Product_.sku), sku);
        };
    }

    public static Specification<Product> hasBarcode(String barcode) {
        return (root, query, criteriaBuilder) -> {
            if (barcode == null || barcode.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Product_.barcode), barcode);
        };
    }

    public static Specification<Product> hasStatus(Product.ProductStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Product_.status), status);
        };
    }

    public static Specification<Product> isAvailable(Boolean available) {
        return (root, query, criteriaBuilder) -> {
            if (available == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Product_.isAvailable), available);
        };
    }

    public static Specification<Product> isFeatured(Boolean featured) {
        return (root, query, criteriaBuilder) -> {
            if (featured == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(Product_.isFeatured), featured);
        };
    }

    public static Specification<Product> belongsToVendor(Long vendorCompanyId) {
        return (root, query, criteriaBuilder) -> {
            if (vendorCompanyId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(Product_.vendorCompany).get("id"),
                vendorCompanyId
            );
        };
    }

    public static Specification<Product> hasMinPrice(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Product_.price), minPrice);
        };
    }

    public static Specification<Product> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (maxPrice == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(Product_.price), maxPrice);
        };
    }

    public static Specification<Product> hasMinRating(BigDecimal minRating) {
        return (root, query, criteriaBuilder) -> {
            if (minRating == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Product_.rating), minRating);
        };
    }

    public static Specification<Product> hasMinStock(Integer minStock) {
        return (root, query, criteriaBuilder) -> {
            if (minStock == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Product_.stockQuantity), minStock);
        };
    }

    public static Specification<Product> hasMaxStock(Integer maxStock) {
        return (root, query, criteriaBuilder) -> {
            if (maxStock == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(Product_.stockQuantity), maxStock);
        };
    }

    public static Specification<Product> hasColor(String color) {
        return (root, query, criteriaBuilder) -> {
            if (color == null || color.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Product_.color)),
                "%" + color.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasProductSize(String size) {
        return (root, query, criteriaBuilder) -> {
            if (size == null || size.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Product_.size)),
                "%" + size.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasMaterial(String material) {
        return (root, query, criteriaBuilder) -> {
            if (material == null || material.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get(Product_.material)),
                "%" + material.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasTag(String tag) {
        return (root, query, criteriaBuilder) -> {
            if (tag == null || tag.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            query.distinct(true);
            return criteriaBuilder.equal(root.join(Product_.tags), tag);
        };
    }

    public static Specification<Product> hasDiscount() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThan(root.get(Product_.discount), BigDecimal.ZERO);
    }

    public static Specification<Product> createdAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get(Product_.createdAt), dateTime);
        };
    }

    public static Specification<Product> createdBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get(Product_.createdAt), dateTime);
        };
    }

    public static Specification<Product> isActive() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get(Product_.status), Product.ProductStatus.ACTIVE);
    }

    public static Specification<Product> isAvailableProduct() {
        return isActive().and(isAvailable(true));
    }

    public static Specification<Product> isOutOfStock() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.equal(root.get(Product_.status), Product.ProductStatus.OUT_OF_STOCK),
            criteriaBuilder.lessThanOrEqualTo(root.get(Product_.stockQuantity), 0)
        );
    }

    public static Specification<Product> isLowStock() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.greaterThan(root.get(Product_.stockQuantity), 0),
            criteriaBuilder.lessThanOrEqualTo(root.get(Product_.stockQuantity), root.get(Product_.minStockLevel))
        );
    }

    public static Specification<Product> isHighRated() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThanOrEqualTo(root.get(Product_.rating), new BigDecimal("4.5"));
    }
}