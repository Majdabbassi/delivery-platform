package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    long countByStatus(Product.ProductStatus status);

    long countByIsAvailable(Boolean isAvailable);

    long countByIsFeatured(Boolean isFeatured);

    long countByVendorCompany_Id(Long vendorCompanyId);

    long countByCategory(String category);

    List<Product> findByVendorCompany_Id(Long vendorCompanyId);

    List<Product> findByCategoryAndIsAvailableTrue(String category);

    List<Product> findByStatusAndStockQuantityLessThanEqual(Product.ProductStatus status, Integer stockQuantity);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.category <> '' ORDER BY p.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT p.subcategory FROM Product p WHERE p.subcategory IS NOT NULL AND p.subcategory <> '' ORDER BY p.subcategory")
    List<String> findDistinctSubcategories();

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.brand <> '' ORDER BY p.brand")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT t FROM Product p JOIN p.tags t ORDER BY t")
    List<String> findDistinctTags();

    @Query("SELECT COALESCE(SUM(p.price * p.stockQuantity), 0) FROM Product p")
    BigDecimal findTotalInventoryValue();

    @Query("SELECT COALESCE(SUM(p.price * p.stockQuantity), 0) FROM Product p WHERE p.vendorCompany.id = :vendorCompanyId")
    BigDecimal findTotalInventoryValueByVendor(@Param("vendorCompanyId") Long vendorCompanyId);

    @Query("SELECT COALESCE(SUM(p.totalRevenue), 0) FROM Product p")
    BigDecimal findTotalRevenue();

    @Query("SELECT COALESCE(SUM(p.totalSold), 0) FROM Product p")
    Long findTotalSold();

    @Query("SELECT p.category, COUNT(p) FROM Product p WHERE p.category IS NOT NULL AND p.category <> '' GROUP BY p.category")
    List<Object[]> countByCategoryGrouped();

    @Query("SELECT p.status, COUNT(p) FROM Product p GROUP BY p.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT COUNT(DISTINCT p.vendorCompany.id) FROM Product p WHERE p.vendorCompany IS NOT NULL")
    long countVendorCompaniesWithProducts();
}