package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.Product;
import com.swiftdeliver.backend.entity.VendorCompany;
import com.swiftdeliver.backend.exception.ResourceNotFoundException;
import com.swiftdeliver.backend.repository.ProductRepository;
import com.swiftdeliver.backend.specification.ProductSpecifications;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    @Resource
    private ProductRepository productRepository;

    @Resource
    private VendorCompanyService vendorCompanyService;

    // Create operations
    public Product createProduct(Product product) {
        resolveVendorReference(product);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setLastUpdated(LocalDateTime.now());
        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }
        return productRepository.save(product);
    }

    public Product createProductForVendor(Long vendorCompanyId, Product product) {
        VendorCompany vendorCompany = vendorCompanyService.getVendorCompanyById(vendorCompanyId);
        product.setVendorCompany(vendorCompany);
        return createProduct(product);
    }

    // Read operations
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Product> findProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }

    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByVendor(Long vendorCompanyId) {
        return productRepository.findByVendorCompany_Id(vendorCompanyId);
    }

    // Search with criteria
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(
            String name, String description, String category, String subcategory,
            String brand, String sku, String barcode, Product.ProductStatus status,
            Boolean available, Boolean featured, Long vendorId,
            BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating,
            Integer minStock, Integer maxStock, String color, String size,
            String material, String tag, Boolean discounted,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            Pageable pageable) {

        Specification<Product> spec = null;

        if (name != null && !name.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasName(name) : spec.and(ProductSpecifications.hasName(name));
        }
        if (description != null && !description.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasDescription(description) : spec.and(ProductSpecifications.hasDescription(description));
        }
        if (category != null && !category.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasCategory(category) : spec.and(ProductSpecifications.hasCategory(category));
        }
        if (subcategory != null && !subcategory.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasSubcategory(subcategory) : spec.and(ProductSpecifications.hasSubcategory(subcategory));
        }
        if (brand != null && !brand.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasBrand(brand) : spec.and(ProductSpecifications.hasBrand(brand));
        }
        if (sku != null && !sku.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasSku(sku) : spec.and(ProductSpecifications.hasSku(sku));
        }
        if (barcode != null && !barcode.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasBarcode(barcode) : spec.and(ProductSpecifications.hasBarcode(barcode));
        }
        if (status != null) {
            spec = (spec == null) ? ProductSpecifications.hasStatus(status) : spec.and(ProductSpecifications.hasStatus(status));
        }
        if (available != null) {
            spec = (spec == null) ? ProductSpecifications.isAvailable(available) : spec.and(ProductSpecifications.isAvailable(available));
        }
        if (featured != null) {
            spec = (spec == null) ? ProductSpecifications.isFeatured(featured) : spec.and(ProductSpecifications.isFeatured(featured));
        }
        if (vendorId != null) {
            spec = (spec == null) ? ProductSpecifications.belongsToVendor(vendorId) : spec.and(ProductSpecifications.belongsToVendor(vendorId));
        }
        if (minPrice != null) {
            spec = (spec == null) ? ProductSpecifications.hasMinPrice(minPrice) : spec.and(ProductSpecifications.hasMinPrice(minPrice));
        }
        if (maxPrice != null) {
            spec = (spec == null) ? ProductSpecifications.hasMaxPrice(maxPrice) : spec.and(ProductSpecifications.hasMaxPrice(maxPrice));
        }
        if (minRating != null) {
            spec = (spec == null) ? ProductSpecifications.hasMinRating(minRating) : spec.and(ProductSpecifications.hasMinRating(minRating));
        }
        if (minStock != null) {
            spec = (spec == null) ? ProductSpecifications.hasMinStock(minStock) : spec.and(ProductSpecifications.hasMinStock(minStock));
        }
        if (maxStock != null) {
            spec = (spec == null) ? ProductSpecifications.hasMaxStock(maxStock) : spec.and(ProductSpecifications.hasMaxStock(maxStock));
        }
        if (color != null && !color.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasColor(color) : spec.and(ProductSpecifications.hasColor(color));
        }
        if (size != null && !size.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasProductSize(size) : spec.and(ProductSpecifications.hasProductSize(size));
        }
        if (material != null && !material.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasMaterial(material) : spec.and(ProductSpecifications.hasMaterial(material));
        }
        if (tag != null && !tag.trim().isEmpty()) {
            spec = (spec == null) ? ProductSpecifications.hasTag(tag) : spec.and(ProductSpecifications.hasTag(tag));
        }
        if (Boolean.TRUE.equals(discounted)) {
            spec = (spec == null) ? ProductSpecifications.hasDiscount() : spec.and(ProductSpecifications.hasDiscount());
        }
        if (createdAfter != null) {
            spec = (spec == null) ? ProductSpecifications.createdAfter(createdAfter) : spec.and(ProductSpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? ProductSpecifications.createdBefore(createdBefore) : spec.and(ProductSpecifications.createdBefore(createdBefore));
        }

        return productRepository.findAll(spec, pageable);
    }

    // Quick filter queries
    @Transactional(readOnly = true)
    public Page<Product> searchByName(String searchTerm, Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.hasName(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> searchByDescription(String searchTerm, Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.hasDescription(searchTerm),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getAvailableProducts(Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.isAvailableProduct(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getFeaturedProducts(Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.isFeatured(true),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getHighRatedProducts(Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.isHighRated(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getLowStockProducts(Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.isLowStock(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getOutOfStockProducts(Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.isOutOfStock(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getDiscountedProducts(Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.hasDiscount(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getNewProducts(Pageable pageable) {
        return getNewProducts(LocalDateTime.now().minusDays(30), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> getNewProducts(LocalDateTime since, Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.createdAfter(since),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.hasCategory(category),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByBrand(String brand, Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.hasBrand(brand),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<Product> spec =
                ProductSpecifications.createdAfter(startDate != null ? startDate.atStartOfDay() : null)
                        .and(ProductSpecifications.createdBefore(endDate != null ? endDate.atTime(23, 59, 59) : null));
        return productRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.hasMinPrice(minPrice).and(ProductSpecifications.hasMaxPrice(maxPrice)),
                pageable
        );
    }

    // Distinct values
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<String> getAllSubcategories() {
        return productRepository.findDistinctSubcategories();
    }

    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return productRepository.findDistinctBrands();
    }

    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return productRepository.findDistinctTags();
    }

    /** The owning vendor company's id, read inside a transaction (the relation is lazy). */
    @Transactional(readOnly = true)
    public Long getVendorCompanyIdOfProduct(Long productId) {
        Product product = getProductById(productId);
        return product.getVendorCompany() == null ? null : product.getVendorCompany().getId();
    }

    // Update operations
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existing = getProductById(id);
        if (updatedProduct.getVendorCompanyIdInput() != null) {
            existing.setVendorCompany(vendorCompanyService.getVendorCompanyById(updatedProduct.getVendorCompanyIdInput()));
        }

        if (updatedProduct.getName() != null) {
            existing.setName(updatedProduct.getName());
        }
        if (updatedProduct.getDescription() != null) {
            existing.setDescription(updatedProduct.getDescription());
        }
        if (updatedProduct.getPrice() != null) {
            existing.setPrice(updatedProduct.getPrice());
        }
        if (updatedProduct.getCostPrice() != null) {
            existing.setCostPrice(updatedProduct.getCostPrice());
        }
        if (updatedProduct.getCategory() != null) {
            existing.setCategory(updatedProduct.getCategory());
        }
        if (updatedProduct.getSubcategory() != null) {
            existing.setSubcategory(updatedProduct.getSubcategory());
        }
        if (updatedProduct.getBrand() != null) {
            existing.setBrand(updatedProduct.getBrand());
        }
        if (updatedProduct.getSku() != null) {
            existing.setSku(updatedProduct.getSku());
        }
        if (updatedProduct.getBarcode() != null) {
            existing.setBarcode(updatedProduct.getBarcode());
        }
        if (updatedProduct.getWeight() != null) {
            existing.setWeight(updatedProduct.getWeight());
        }
        if (updatedProduct.getDimensions() != null) {
            existing.setDimensions(updatedProduct.getDimensions());
        }
        if (updatedProduct.getColor() != null) {
            existing.setColor(updatedProduct.getColor());
        }
        if (updatedProduct.getSize() != null) {
            existing.setSize(updatedProduct.getSize());
        }
        if (updatedProduct.getMaterial() != null) {
            existing.setMaterial(updatedProduct.getMaterial());
        }
        if (updatedProduct.getStockQuantity() != null) {
            existing.setStockQuantity(updatedProduct.getStockQuantity());
        }
        if (updatedProduct.getMinStockLevel() != null) {
            existing.setMinStockLevel(updatedProduct.getMinStockLevel());
        }
        if (updatedProduct.getMaxStockLevel() != null) {
            existing.setMaxStockLevel(updatedProduct.getMaxStockLevel());
        }
        if (updatedProduct.getStatus() != null) {
            existing.setStatus(updatedProduct.getStatus());
        }
        if (updatedProduct.getIsAvailable() != null) {
            existing.setIsAvailable(updatedProduct.getIsAvailable());
        }
        if (updatedProduct.getIsFeatured() != null) {
            existing.setIsFeatured(updatedProduct.getIsFeatured());
        }
        if (updatedProduct.getRating() != null) {
            existing.setRating(updatedProduct.getRating());
        }
        if (updatedProduct.getReviewCount() != null) {
            existing.setReviewCount(updatedProduct.getReviewCount());
        }
        if (updatedProduct.getImageUrls() != null) {
            existing.setImageUrls(updatedProduct.getImageUrls());
        }
        if (updatedProduct.getTags() != null) {
            existing.setTags(updatedProduct.getTags());
        }
        if (updatedProduct.getSpecifications() != null) {
            existing.setSpecifications(updatedProduct.getSpecifications());
        }
        if (updatedProduct.getWarranty() != null) {
            existing.setWarranty(updatedProduct.getWarranty());
        }
        if (updatedProduct.getManufacturerDate() != null) {
            existing.setManufacturerDate(updatedProduct.getManufacturerDate());
        }
        if (updatedProduct.getExpiryDate() != null) {
            existing.setExpiryDate(updatedProduct.getExpiryDate());
        }
        if (updatedProduct.getDiscount() != null) {
            existing.setDiscount(updatedProduct.getDiscount());
        }
        if (updatedProduct.getDiscountType() != null) {
            existing.setDiscountType(updatedProduct.getDiscountType());
        }
        if (updatedProduct.getDiscountStartDate() != null) {
            existing.setDiscountStartDate(updatedProduct.getDiscountStartDate());
        }
        if (updatedProduct.getDiscountEndDate() != null) {
            existing.setDiscountEndDate(updatedProduct.getDiscountEndDate());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(existing);
    }

    // Status / availability update operations
    public Product updateProductStatus(Long id, Product.ProductStatus status) {
        Product product = getProductById(id);
        product.setStatus(status);
        if (status == Product.ProductStatus.OUT_OF_STOCK || status == Product.ProductStatus.DISCONTINUED) {
            product.setIsAvailable(false);
        } else if (product.getStockQuantity() != null && product.getStockQuantity() > 0) {
            product.setIsAvailable(true);
        }
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    public Product updateProductAvailability(Long id, boolean isAvailable) {
        Product product = getProductById(id);
        product.setIsAvailable(isAvailable);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    public Product updateFeaturedStatus(Long id, boolean isFeatured) {
        Product product = getProductById(id);
        product.setIsFeatured(isFeatured);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    public Product updateProductPrice(Long id, BigDecimal price) {
        Product product = getProductById(id);
        product.setPrice(price);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    public Product updateStockQuantity(Long id, Integer quantity) {
        Product product = getProductById(id);
        product.setStockQuantity(quantity);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    public Product updateProductRating(Long id, BigDecimal rating, Integer reviewCount) {
        Product product = getProductById(id);
        if (rating != null) {
            product.setRating(rating);
        }
        if (reviewCount != null) {
            product.setReviewCount(reviewCount);
        }
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    public Product updateDiscount(Long id, BigDecimal discount, Product.DiscountType discountType,
                                  LocalDate startDate, LocalDate endDate) {
        Product product = getProductById(id);
        product.setDiscount(discount);
        product.setDiscountType(discountType != null ? discountType : Product.DiscountType.PERCENTAGE);
        product.setDiscountStartDate(startDate);
        product.setDiscountEndDate(endDate);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    public Product removeDiscount(Long id) {
        Product product = getProductById(id);
        product.setDiscount(null);
        product.setDiscountType(null);
        product.setDiscountStartDate(null);
        product.setDiscountEndDate(null);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    // Delete operations
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    public Product softDeleteProduct(Long id) {
        Product product = getProductById(id);
        product.setStatus(Product.ProductStatus.DISCONTINUED);
        product.setIsAvailable(false);
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    // Validation methods
    @Transactional(readOnly = true)
    public boolean existsBySku(String sku) {
        return productRepository.existsBySku(sku);
    }

    @Transactional(readOnly = true)
    public boolean existsByBarcode(String barcode) {
        return productRepository.existsByBarcode(barcode);
    }

    @Transactional(readOnly = true)
    public boolean existsBySkuAndIdNot(String sku, Long id) {
        return productRepository.existsBySkuAndIdNot(sku, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByBarcodeAndIdNot(String barcode, Long id) {
        return productRepository.existsByBarcodeAndIdNot(barcode, id);
    }

    // Statistical operations
    @Transactional(readOnly = true)
    public long countAllProducts() {
        return productRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveProducts() {
        return productRepository.count(ProductSpecifications.isActive());
    }

    @Transactional(readOnly = true)
    public long countAvailableProducts() {
        return productRepository.count(ProductSpecifications.isAvailableProduct());
    }

    @Transactional(readOnly = true)
    public long countFeaturedProducts() {
        return productRepository.count(ProductSpecifications.isFeatured(true));
    }

    @Transactional(readOnly = true)
    public long countOutOfStockProducts() {
        return productRepository.count(ProductSpecifications.isOutOfStock());
    }

    @Transactional(readOnly = true)
    public long countLowStockProducts() {
        return productRepository.count(ProductSpecifications.isLowStock());
    }

    @Transactional(readOnly = true)
    public long countHighRatedProducts() {
        return productRepository.count(ProductSpecifications.isHighRated());
    }

    @Transactional(readOnly = true)
    public long countDiscountedProducts() {
        return productRepository.count(ProductSpecifications.hasDiscount());
    }

    @Transactional(readOnly = true)
    public long countProductsByCategory(String category) {
        return productRepository.count(ProductSpecifications.hasCategory(category));
    }

    @Transactional(readOnly = true)
    public long countProductsByVendor(Long vendorCompanyId) {
        return productRepository.count(ProductSpecifications.belongsToVendor(vendorCompanyId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProductStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", countAllProducts());
        stats.put("active", countActiveProducts());
        stats.put("available", countAvailableProducts());
        stats.put("featured", countFeaturedProducts());
        stats.put("outOfStock", countOutOfStockProducts());
        stats.put("lowStock", countLowStockProducts());
        stats.put("highRated", countHighRatedProducts());
        stats.put("withDiscount", countDiscountedProducts());

        BigDecimal totalValue = productRepository.findTotalInventoryValue();
        stats.put("totalValue", totalValue != null ? totalValue : BigDecimal.ZERO);
        stats.put("totalRevenue", productRepository.findTotalRevenue());
        stats.put("totalSold", productRepository.findTotalSold());

        Map<Object, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : productRepository.countByStatusGrouped()) {
            byStatus.put(row[0] != null ? row[0].toString() : "UNKNOWN", (Long) row[1]);
        }
        stats.put("byStatus", byStatus);

        Map<Object, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : productRepository.countByCategoryGrouped()) {
            byCategory.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("byCategory", byCategory);

        return stats;
    }

    private void resolveVendorReference(Product product) {
        if (product.getVendorCompanyIdInput() != null) {
            product.setVendorCompany(vendorCompanyService.getVendorCompanyById(product.getVendorCompanyIdInput()));
        } else if (product.getVendorCompany() != null && product.getVendorCompany().getId() != null) {
            product.setVendorCompany(vendorCompanyService.getVendorCompanyById(product.getVendorCompany().getId()));
        }
    }
}