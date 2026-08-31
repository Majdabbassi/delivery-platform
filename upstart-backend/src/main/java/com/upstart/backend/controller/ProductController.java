package com.upstart.backend.controller;

import com.upstart.backend.entity.Product;
import com.upstart.backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "APIs for managing products")
public class ProductController {

    @Autowired
    private ProductService productService;

    private static final String WRITE_ROLE = "hasAnyRole('SUPER_ADMIN','VENDOR_OWNER')";

    @PostMapping
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Create a new product", description = "Creates a new product with the provided details")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product created = productService.createProduct(product);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/vendor/{vendorCompanyId}")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Create a product for a vendor", description = "Creates a new product linked to the given vendor company")
    public ResponseEntity<Product> createProductForVendor(@PathVariable Long vendorCompanyId, @RequestBody Product product) {
        Product created = productService.createProductForVendor(vendorCompanyId, product);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a product by its unique identifier")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Retrieves a product by its SKU (stock keeping unit)")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Product not found with SKU: " + sku)));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Get product by barcode", description = "Retrieves a product by its barcode")
    public ResponseEntity<Product> getProductByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Product not found with barcode: " + barcode)));
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieves all products with pagination")
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products with dynamic criteria", description = "Search products using multiple optional criteria with pagination and sorting")
    public ResponseEntity<Page<Product>> searchProducts(
            @Parameter(description = "Name filter (partial match)") @RequestParam(required = false) String name,
            @Parameter(description = "Description filter (partial match)") @RequestParam(required = false) String description,
            @Parameter(description = "Category filter (partial match)") @RequestParam(required = false) String category,
            @Parameter(description = "Subcategory filter (partial match)") @RequestParam(required = false) String subcategory,
            @Parameter(description = "Brand filter (partial match)") @RequestParam(required = false) String brand,
            @Parameter(description = "SKU filter (exact match)") @RequestParam(required = false) String sku,
            @Parameter(description = "Barcode filter (exact match)") @RequestParam(required = false) String barcode,
            @Parameter(description = "Status filter") @RequestParam(required = false) Product.ProductStatus status,
            @Parameter(description = "Availability filter") @RequestParam(required = false) Boolean available,
            @Parameter(description = "Featured filter") @RequestParam(required = false) Boolean featured,
            @Parameter(description = "Vendor company ID filter") @RequestParam(required = false) Long vendorId,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Minimum rating") @RequestParam(required = false) BigDecimal minRating,
            @Parameter(description = "Minimum stock quantity") @RequestParam(required = false) Integer minStock,
            @Parameter(description = "Maximum stock quantity") @RequestParam(required = false) Integer maxStock,
            @Parameter(description = "Color filter") @RequestParam(required = false) String color,
            @Parameter(description = "Size filter") @RequestParam(required = false) String productSize,
            @Parameter(description = "Material filter") @RequestParam(required = false) String material,
            @Parameter(description = "Tag filter") @RequestParam(required = false) String tag,
            @Parameter(description = "Discounted filter") @RequestParam(required = false) Boolean discounted,
            @Parameter(description = "Created after date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @Parameter(description = "Created before date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Page size alias") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        int effectiveSize = pageSize != null ? pageSize : size;
        Pageable pageable = buildPageable(page, effectiveSize, sortBy, sortDir);
        Page<Product> products = productService.searchProducts(
                name, description, category, subcategory, brand, sku, barcode, status,
                available, featured, vendorId, minPrice, maxPrice, minRating,
                minStock, maxStock, color, productSize, material, tag, discounted,
                createdAfter, createdBefore, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search/name")
    @Operation(summary = "Search products by name", description = "Searches products whose name matches the search term")
    public ResponseEntity<Page<Product>> searchByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.searchByName(name, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/search/description")
    @Operation(summary = "Search products by description", description = "Searches products whose description matches the search term")
    public ResponseEntity<Page<Product>> searchByDescription(
            @RequestParam String description,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.searchByDescription(description, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available products", description = "Retrieves products that are currently available for ordering")
    public ResponseEntity<Page<Product>> getAvailableProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getAvailableProducts(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products", description = "Retrieves products marked as featured")
    public ResponseEntity<Page<Product>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getFeaturedProducts(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/high-rated")
    @Operation(summary = "Get high-rated products", description = "Retrieves products with a rating of 4.5 or above")
    public ResponseEntity<Page<Product>> getHighRatedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getHighRatedProducts(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low-stock products", description = "Retrieves products whose stock is at or below their minimum stock level")
    public ResponseEntity<Page<Product>> getLowStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getLowStockProducts(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/out-of-stock")
    @Operation(summary = "Get out-of-stock products", description = "Retrieves products that are out of stock")
    public ResponseEntity<Page<Product>> getOutOfStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getOutOfStockProducts(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/discounted")
    @Operation(summary = "Get discounted products", description = "Retrieves products that currently have a discount")
    public ResponseEntity<Page<Product>> getDiscountedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getDiscountedProducts(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/new")
    @Operation(summary = "Get new products", description = "Retrieves products created within the last 30 days")
    public ResponseEntity<Page<Product>> getNewProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getNewProducts(buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieves products in the given category")
    public ResponseEntity<Page<Product>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getProductsByCategory(category, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/brand/{brand}")
    @Operation(summary = "Get products by brand", description = "Retrieves products from the given brand")
    public ResponseEntity<Page<Product>> getProductsByBrand(
            @PathVariable String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getProductsByBrand(brand, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/vendor/{vendorId}")
    @Operation(summary = "Get products by vendor", description = "Retrieves all products belonging to the given vendor company")
    public ResponseEntity<List<Product>> getProductsByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(productService.getProductsByVendor(vendorId));
    }

    @GetMapping("/price-range")
    @Operation(summary = "Get products in a price range", description = "Retrieves products whose price falls within the given range")
    public ResponseEntity<Page<Product>> getProductsByPriceRange(
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get products in a date range", description = "Retrieves products created within the given date range")
    public ResponseEntity<Page<Product>> getProductsByDateRange(
            @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getProductsByDateRange(startDate, endDate, buildPageable(page, size, sortBy, sortDir)));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all product categories", description = "Retrieves the distinct list of product categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @GetMapping("/brands")
    @Operation(summary = "Get all product brands", description = "Retrieves the distinct list of product brands")
    public ResponseEntity<List<String>> getAllBrands() {
        return ResponseEntity.ok(productService.getAllBrands());
    }

    @GetMapping("/tags")
    @Operation(summary = "Get all product tags", description = "Retrieves the distinct list of product tags")
    public ResponseEntity<List<String>> getAllTags() {
        return ResponseEntity.ok(productService.getAllTags());
    }

    @GetMapping("/exists/sku/{sku}")
    @Operation(summary = "Check SKU existence", description = "Checks whether a product with the given SKU already exists")
    public ResponseEntity<Boolean> existsBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.existsBySku(sku));
    }

    @GetMapping("/exists/barcode/{barcode}")
    @Operation(summary = "Check barcode existence", description = "Checks whether a product with the given barcode already exists")
    public ResponseEntity<Boolean> existsByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.existsByBarcode(barcode));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Update product", description = "Updates an existing product's details")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Update product status", description = "Updates a product's status")
    public ResponseEntity<Product> updateProductStatus(@PathVariable Long id, @RequestParam Product.ProductStatus status) {
        return ResponseEntity.ok(productService.updateProductStatus(id, status));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Update product availability", description = "Updates whether a product is available for ordering")
    public ResponseEntity<Product> updateProductAvailability(@PathVariable Long id, @RequestParam Boolean available) {
        return ResponseEntity.ok(productService.updateProductAvailability(id, available));
    }

    @PatchMapping("/{id}/featured")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Update featured status", description = "Updates whether a product is featured")
    public ResponseEntity<Product> updateFeaturedStatus(@PathVariable Long id, @RequestParam Boolean featured) {
        return ResponseEntity.ok(productService.updateFeaturedStatus(id, featured));
    }

    @PatchMapping("/{id}/price")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Update product price", description = "Updates a product's price")
    public ResponseEntity<Product> updateProductPrice(@PathVariable Long id, @RequestParam BigDecimal price) {
        return ResponseEntity.ok(productService.updateProductPrice(id, price));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Update stock quantity", description = "Updates a product's stock quantity")
    public ResponseEntity<Product> updateStockQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        return ResponseEntity.ok(productService.updateStockQuantity(id, quantity));
    }

    @PatchMapping("/{id}/rating")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Update product rating", description = "Updates a product's rating and review count")
    public ResponseEntity<Product> updateProductRating(@PathVariable Long id,
            @RequestParam(required = false) BigDecimal rating,
            @RequestParam(required = false) Integer reviewCount) {
        return ResponseEntity.ok(productService.updateProductRating(id, rating, reviewCount));
    }

    @PatchMapping("/{id}/discount")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Update product discount", description = "Updates a product's discount details")
    public ResponseEntity<Product> updateProductDiscount(@PathVariable Long id,
            @Parameter(description = "Discount amount") @RequestParam BigDecimal discount,
            @Parameter(description = "Discount type (PERCENTAGE/FIXED)") @RequestParam(required = false) Product.DiscountType discountType,
            @Parameter(description = "Discount start date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Discount end date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(productService.updateDiscount(id, discount, discountType, startDate, endDate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Delete product", description = "Permanently deletes a product from the database")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    @DeleteMapping("/{id}/soft")
    @PreAuthorize(WRITE_ROLE)
    @Operation(summary = "Soft delete product", description = "Soft deletes a product by marking it as discontinued")
    public ResponseEntity<Product> softDeleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.softDeleteProduct(id));
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get product statistics", description = "Retrieves overall statistics about products")
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        return ResponseEntity.ok(productService.getProductStatistics());
    }

    @GetMapping("/stats/count/total")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count total products", description = "Returns the total count of products")
    public ResponseEntity<Long> countTotalProducts() {
        return ResponseEntity.ok(productService.countAllProducts());
    }

    @GetMapping("/stats/count/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count active products", description = "Returns the count of active products")
    public ResponseEntity<Long> countActiveProducts() {
        return ResponseEntity.ok(productService.countActiveProducts());
    }

    @GetMapping("/stats/count/available")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count available products", description = "Returns the count of available products")
    public ResponseEntity<Long> countAvailableProducts() {
        return ResponseEntity.ok(productService.countAvailableProducts());
    }

    @GetMapping("/stats/count/featured")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count featured products", description = "Returns the count of featured products")
    public ResponseEntity<Long> countFeaturedProducts() {
        return ResponseEntity.ok(productService.countFeaturedProducts());
    }

    @GetMapping("/stats/count/out-of-stock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count out-of-stock products", description = "Returns the count of out-of-stock products")
    public ResponseEntity<Long> countOutOfStockProducts() {
        return ResponseEntity.ok(productService.countOutOfStockProducts());
    }

    @GetMapping("/stats/count/low-stock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count low-stock products", description = "Returns the count of low-stock products")
    public ResponseEntity<Long> countLowStockProducts() {
        return ResponseEntity.ok(productService.countLowStockProducts());
    }

    @GetMapping("/stats/count/high-rated")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count high-rated products", description = "Returns the count of high-rated products")
    public ResponseEntity<Long> countHighRatedProducts() {
        return ResponseEntity.ok(productService.countHighRatedProducts());
    }

    @GetMapping("/stats/count/discounted")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Count discounted products", description = "Returns the count of discounted products")
    public ResponseEntity<Long> countDiscountedProducts() {
        return ResponseEntity.ok(productService.countDiscountedProducts());
    }

    @GetMapping("/stats/count/by-category")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Products by category", description = "Returns the count of products grouped by category")
    public ResponseEntity<Long> countProductsByCategory(@RequestParam String category) {
        return ResponseEntity.ok(productService.countProductsByCategory(category));
    }

    @GetMapping("/stats/count/by-vendor")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Products by vendor", description = "Returns the count of products belonging to a vendor company")
    public ResponseEntity<Long> countProductsByVendor(@RequestParam Long vendorId) {
        return ResponseEntity.ok(productService.countProductsByVendor(vendorId));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
}