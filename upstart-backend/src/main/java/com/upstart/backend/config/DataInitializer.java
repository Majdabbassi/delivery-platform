package com.upstart.backend.config;

import com.upstart.backend.entity.Admin;
import com.upstart.backend.entity.CustomerUser;
import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.DeliveryOwner;
import com.upstart.backend.entity.DriverPerson;
import com.upstart.backend.entity.Order;
import com.upstart.backend.entity.Partnership;
import com.upstart.backend.entity.Product;
import com.upstart.backend.entity.SuperAdmin;
import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.entity.VendorOwner;
import com.upstart.backend.repository.AdminRepository;
import com.upstart.backend.repository.CustomerUserRepository;
import com.upstart.backend.repository.DeliveryCompanyRepository;
import com.upstart.backend.repository.DeliveryOwnerRepository;
import com.upstart.backend.repository.DriverPersonRepository;
import com.upstart.backend.repository.OrderRepository;
import com.upstart.backend.repository.PartnershipRepository;
import com.upstart.backend.repository.ProductRepository;
import com.upstart.backend.repository.SuperAdminRepository;
import com.upstart.backend.repository.VendorCompanyRepository;
import com.upstart.backend.repository.VendorOwnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Upstart@2026!";

    private final SuperAdminRepository superAdminRepository;
    private final AdminRepository adminRepository;
    private final CustomerUserRepository customerUserRepository;
    private final DeliveryOwnerRepository deliveryOwnerRepository;
    private final DeliveryCompanyRepository deliveryCompanyRepository;
    private final VendorOwnerRepository vendorOwnerRepository;
    private final VendorCompanyRepository vendorCompanyRepository;
    private final DriverPersonRepository driverPersonRepository;
    private final PartnershipRepository partnershipRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:REDACTED_ADMIN_PASSWORD}")
    private String adminPassword;

    private String demoPasswordHash;

    public DataInitializer(SuperAdminRepository superAdminRepository,
                           AdminRepository adminRepository,
                           CustomerUserRepository customerUserRepository,
                           DeliveryOwnerRepository deliveryOwnerRepository,
                           DeliveryCompanyRepository deliveryCompanyRepository,
                           VendorOwnerRepository vendorOwnerRepository,
                           VendorCompanyRepository vendorCompanyRepository,
                           DriverPersonRepository driverPersonRepository,
                           PartnershipRepository partnershipRepository,
                           OrderRepository orderRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           Environment environment) {
        this.superAdminRepository = superAdminRepository;
        this.adminRepository = adminRepository;
        this.customerUserRepository = customerUserRepository;
        this.deliveryOwnerRepository = deliveryOwnerRepository;
        this.deliveryCompanyRepository = deliveryCompanyRepository;
        this.vendorOwnerRepository = vendorOwnerRepository;
        this.vendorCompanyRepository = vendorCompanyRepository;
        this.driverPersonRepository = driverPersonRepository;
        this.partnershipRepository = partnershipRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (superAdminRepository.count() == 0) {
            bootstrapAdmin();
        }

        seedAdminsIfNeeded();

        if (!environment.acceptsProfiles(Profiles.of("test"))) {
            seedDemoData();
        }

        seedProductsIfNeeded();
    }

    private void bootstrapAdmin() {
        String password = (adminPassword == null || adminPassword.isBlank())
                ? "REDACTED_ADMIN_PASSWORD"
                : adminPassword;

        SuperAdmin admin = new SuperAdmin(
                adminUsername,
                adminUsername + "@upstart.local",
                passwordEncoder.encode(password),
                "Super",
                "Admin"
        );

        superAdminRepository.save(admin);
        log.warn("===================================================================");
        log.warn("No Super Admin found. Bootstrapped default admin account:");
        log.warn("  username: {}", adminUsername);
        log.warn("  password: {}", password);
        log.warn("CHANGE THIS PASSWORD IMMEDIATELY IN PRODUCTION!");
        log.warn("===================================================================");
    }

    private void seedAdminsIfNeeded() {
        if (adminRepository.count() > 0) {
            log.info("Admins already present - skipping admin seeding");
            return;
        }

        String passwordHash = passwordEncoder.encode(DEMO_PASSWORD);

        Admin opsAdmin = new Admin();
        opsAdmin.setUsername("opsadmin");
        opsAdmin.setEmail("ops.admin@upstart.local");
        opsAdmin.setPassword(passwordHash);
        opsAdmin.setFirstName("Omar");
        opsAdmin.setLastName("El Khattabi");
        opsAdmin.setPhoneNumber("+212660000010");
        opsAdmin.setRole(Admin.AdminRole.SUPER_ADMIN);
        opsAdmin.setEnabled(true);
        opsAdmin.setVerified(true);
        opsAdmin.setIsActive(true);
        opsAdmin.setDepartment("Operations");
        opsAdmin.setPosition("Operations Director");
        opsAdmin.setAccessLevel(Admin.AccessLevel.FULL);
        opsAdmin.setCanManageUsers(true);
        opsAdmin.setCanManageSystem(true);
        opsAdmin.setCanViewReports(true);
        opsAdmin.setCanManageContent(true);
        opsAdmin.setSessionTimeout(60);
        opsAdmin.setHireDate(LocalDate.now().minusDays(30));
        opsAdmin.setSalary(new BigDecimal("18000.00"));
        opsAdmin.setNationalId("ID-ADMIN-000001");
        opsAdmin.setPermissions(List.of("users:manage", "orders:manage", "system:admin"));
        opsAdmin.setAssignedModules(List.of("Dashboard", "Users", "Orders", "System"));
        opsAdmin.setAddress("1 Administrative Quarter, Rabat");
        opsAdmin.setCreatedBy("system");

        Admin contentAdmin = new Admin();
        contentAdmin.setUsername("contentadmin");
        contentAdmin.setEmail("content.admin@upstart.local");
        contentAdmin.setPassword(passwordHash);
        contentAdmin.setFirstName("Salma");
        contentAdmin.setLastName("Berrada");
        contentAdmin.setPhoneNumber("+212660000011");
        contentAdmin.setRole(Admin.AdminRole.ADMIN);
        contentAdmin.setEnabled(true);
        contentAdmin.setVerified(true);
        contentAdmin.setIsActive(true);
        contentAdmin.setDepartment("Content");
        contentAdmin.setPosition("Content Manager");
        contentAdmin.setAccessLevel(Admin.AccessLevel.LIMITED);
        contentAdmin.setCanManageContent(true);
        contentAdmin.setCanViewReports(true);
        contentAdmin.setSessionTimeout(45);
        contentAdmin.setHireDate(LocalDate.now().minusMonths(8));
        contentAdmin.setSalary(new BigDecimal("12500.00"));
        contentAdmin.setNationalId("ID-ADMIN-000002");
        contentAdmin.setPermissions(List.of("content:manage", "reports:view"));
        contentAdmin.setAssignedModules(List.of("Products", "Content"));
        contentAdmin.setAddress("23 Content Park, Casablanca");
        contentAdmin.setCreatedBy("opsadmin");

        Admin supportModerator = new Admin();
        supportModerator.setUsername("supportmod");
        supportModerator.setEmail("support.mod@upstart.local");
        supportModerator.setPassword(passwordHash);
        supportModerator.setFirstName("Yasmine");
        supportModerator.setLastName("Fassi");
        supportModerator.setPhoneNumber("+212660000012");
        supportModerator.setRole(Admin.AdminRole.MODERATOR);
        supportModerator.setEnabled(true);
        supportModerator.setVerified(false);
        supportModerator.setIsActive(true);
        supportModerator.setDepartment("Support");
        supportModerator.setPosition("Support Moderator");
        supportModerator.setAccessLevel(Admin.AccessLevel.READ_ONLY);
        supportModerator.setCanViewReports(true);
        supportModerator.setSessionTimeout(30);
        supportModerator.setHireDate(LocalDate.now().minusMonths(3));
        supportModerator.setSalary(new BigDecimal("9000.00"));
        supportModerator.setNationalId("ID-ADMIN-000003");
        supportModerator.setPermissions(List.of("content:view"));
        supportModerator.setAssignedModules(List.of("Support", "Reports"));
        supportModerator.setAddress("5 Helpdesk Avenue, Tangier");
        supportModerator.setCreatedBy("opsadmin");

        adminRepository.saveAll(List.of(opsAdmin, contentAdmin, supportModerator));
        log.info("Seeded {} demo admin accounts", 3);
    }

    private void seedProductsIfNeeded() {
        if (productRepository.count() > 0) {
            log.info("Products already present - skipping product seeding");
            return;
        }

        List<VendorCompany> vendorCompanies = vendorCompanyRepository.findAll(
                Sort.by(Sort.Direction.ASC, "id"));

        List<Product> products = new ArrayList<>();

        products.add(buildProduct("Organic Avocado (500g)", "Ripe Hass avocado, farm-sourced daily.", "Grocery", "Fresh Produce", "GreenFarm", "GR-FP-AVO-001",
                "611100000001", new BigDecimal("12.50"), 40, 8, Product.ProductStatus.ACTIVE, true, true, new BigDecimal("4.60"), 128, 0));
        products.add(buildProduct("Atlas Olive Oil 1L", "Cold-pressed extra virgin olive oil from Meknes groves.", "Grocery", "Pantry Staples", "AtlasGroves", "GR-PS-OIL-002",
                "611100000002", new BigDecimal("85.00"), 25, 5, Product.ProductStatus.ACTIVE, true, true, new BigDecimal("4.80"), 96, 10));
        products.add(buildProduct("Fresh Baguette (2-pack)", "Artisan-baked daily, crispy crust and soft crumb.", "Bakery", "Bread", "MaisonDuPain", "BK-BR-BAG-003",
                "611100000003", new BigDecimal("9.00"), 120, 20, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.30"), 210, 0));
        products.add(buildProduct("Moroccan Dates Box 2kg", "Premium Majhoul dates, gift-ready packaging.", "Grocery", "Dried Fruits", "SaharaHarvest", "GR-DF-DAT-004",
                "611100000004", new BigDecimal("110.00"), 18, 20, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.90"), 45, 15));
        products.add(buildProduct("Fresh Cow Milk 1L", "Pasteurized whole milk from local dairy cooperatives.", "Grocery", "Dairy", "CoopLaiterie", "GR-DY-MLK-005",
                "611100000005", new BigDecimal("8.50"), 0, 10, Product.ProductStatus.OUT_OF_STOCK, false, false, new BigDecimal("4.10"), 320, 0));

        products.add(buildProduct("Wireless Noise-Cancel Headphones", "Over-ear Bluetooth headphones with 30h battery.", "Electronics", "Audio", "TechNova", "EL-AU-HPH-006",
                "611100000006", new BigDecimal("899.00"), 30, 5, Product.ProductStatus.ACTIVE, true, true, new BigDecimal("4.80"), 87, 50));
        products.add(buildProduct("Smart Watch Pro S2", "AMOLED display, GPS, heart-rate and sleep tracking.", "Electronics", "Wearables", "TechNova", "EL-WR-WAT-007",
                "611100000007", new BigDecimal("1299.00"), 22, 3, Product.ProductStatus.ACTIVE, true, true, new BigDecimal("4.70"), 142, 100));
        products.add(buildProduct("USB-C Fast Charger 65W", "GaN charger compatible with laptops and phones.", "Electronics", "Accessories", "VoltCore", "EL-AC-CHG-008",
                "611100000008", new BigDecimal("189.00"), 60, 10, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.50"), 240, 20));
        products.add(buildProduct("Bluetooth Speaker Mini", "Compact waterproof speaker with rich bass.", "Electronics", "Audio", "VoltCore", "EL-AU-SPK-009",
                "611100000009", new BigDecimal("249.00"), 15, 16, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.20"), 68, 0));
        products.add(buildProduct("4K Action Camera", "Ultra HD camera with stabilization and waterproof case.", "Electronics", "Cameras", "PixelPro", "EL-CM-ACT-010",
                "611100000010", new BigDecimal("1499.00"), 12, 3, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.60"), 39, 200));

        products.add(buildProduct("Men's Leather Wallet", "Hand-stitched genuine leather bifold wallet.", "Fashion", "Accessories", "CuirMaroc", "FT-AC-WAL-011",
                "611100000011", new BigDecimal("180.00"), 45, 6, Product.ProductStatus.ACTIVE, true, true, new BigDecimal("4.40"), 172, 20));
        products.add(buildProduct("Women's Kaftan", "Elegant Moroccan kaftan with hand embroidery.", "Fashion", "Traditional", "MaisonZellige", "FT-TR-KFT-012",
                "611100000012", new BigDecimal("1450.00"), 10, 2, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.90"), 26, 150));
        products.add(buildProduct("Cotton T-Shirt (Unisex)", "100% organic cotton, pre-shrunk, multiple sizes.", "Fashion", "Apparel", "WearMorocco", "FT-AP-TSH-013",
                "611100000013", new BigDecimal("75.00"), 150, 30, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.20"), 410, 0));
        products.add(buildProduct("Leather Belt", "Full-grain leather belt, adjustable buckle.", "Fashion", "Accessories", "CuirMaroc", "FT-AC-BEL-014",
                "611100000014", new BigDecimal("140.00"), 35, 8, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.30"), 88, 15));
        products.add(buildProduct("Embroidered Baby Blanket", "Soft cotton blanket with Berber embroidery.", "Fashion", "Textiles", "MaisonZellige", "FT-TX-BLK-015",
                "611100000015", new BigDecimal("210.00"), 0, 4, Product.ProductStatus.OUT_OF_STOCK, false, false, new BigDecimal("4.70"), 54, 0));

        products.add(buildProduct("Argan Oil Shampoo 400ml", "Nourishing shampoo enriched with Moroccan argan oil.", "Beauty", "Hair Care", "ArganPure", "BT-HC-SHM-016",
                "611100000016", new BigDecimal("95.00"), 55, 12, Product.ProductStatus.ACTIVE, true, true, new BigDecimal("4.60"), 190, 10));
        products.add(buildProduct("Rose Face Cream 50ml", "Hydrating day cream with rose extract.", "Beauty", "Skincare", "RoseAtlas", "BT-SK-CRM-017",
                "611100000017", new BigDecimal("160.00"), 40, 8, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.50"), 132, 15));
        products.add(buildProduct("Rassoul Clay Mask", "Purifying Moroccan clay mask, natural formula.", "Beauty", "Skincare", "ArganPure", "BT-SK-MSK-018",
                "611100000018", new BigDecimal("55.00"), 80, 15, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.40"), 260, 0));
        products.add(buildProduct("Perfume Oud Essence 50ml", "Concentrated oud perfume oil with warm amber notes.", "Beauty", "Fragrance", "OudMaison", "BT-FR-ODR-019",
                "611100000019", new BigDecimal("780.00"), 0, 3, Product.ProductStatus.OUT_OF_STOCK, false, true, new BigDecimal("4.90"), 66, 0));

        products.add(buildProduct("Ceramic Tagine 4L", "Hand-painted traditional Moroccan tagine.", "Home", "Kitchen", "PoterieMaroc", "HM-KT-TGN-020",
                "611100000020", new BigDecimal("320.00"), 20, 4, Product.ProductStatus.ACTIVE, true, true, new BigDecimal("4.80"), 98, 30));
        products.add(buildProduct("Brass Tea Set", "Authentic Moroccan brass teapot with 6 glasses.", "Home", "Kitchen", "AteliersFes", "HM-KT-TEA-021",
                "611100000021", new BigDecimal("450.00"), 14, 3, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.70"), 61, 45));
        products.add(buildProduct("Woven Wool Rug 2x3m", "Hand-woven berber rug in natural wool.", "Home", "Decor", "TapisMaroc", "HM-DC-RUG-022",
                "611100000022", new BigDecimal("1900.00"), 8, 2, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.90"), 22, 200));
        products.add(buildProduct("Olive Wood Cutting Board", "Durable olive wood board, food-safe finish.", "Home", "Kitchen", "AteliersFes", "HM-KT-BRD-023",
                "611100000023", new BigDecimal("145.00"), 30, 6, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.50"), 105, 0));
        products.add(buildProduct("Lantern Candle Holder", "Bronze Moroccan lantern, hand-crafted.", "Home", "Lighting", "PoterieMaroc", "HM-LT-LAN-024",
                "611100000024", new BigDecimal("265.00"), 16, 18, Product.ProductStatus.ACTIVE, true, false, new BigDecimal("4.60"), 49, 25));

        for (int i = 0; i < products.size(); i++) {
            if (!vendorCompanies.isEmpty()) {
                products.get(i).setVendorCompany(vendorCompanies.get(i % vendorCompanies.size()));
            }
        }

        productRepository.saveAll(products);
        log.info("Seeded {} demo products", products.size());
    }

    private Product buildProduct(String name, String description, String category, String subcategory,
                                 String brand, String sku, String barcode, BigDecimal price,
                                 int stock, int minStock, Product.ProductStatus status,
                                 boolean available, boolean featured, BigDecimal rating,
                                 int reviewCount, int discountPct) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setBrand(brand);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setPrice(price);
        product.setCostPrice(price.multiply(new BigDecimal("0.65")).setScale(2, java.math.RoundingMode.HALF_UP));
        product.setStockQuantity(stock);
        product.setMinStockLevel(minStock);
        product.setMaxStockLevel(Math.max(minStock * 10, 100));
        product.setStatus(status);
        product.setIsAvailable(available);
        product.setIsFeatured(featured);
        product.setRating(rating);
        product.setReviewCount(reviewCount);
        product.setWeight(0.5 + (category.hashCode() % 10) * 0.4);
        product.setDimensions("10x10x10 cm");
        product.setCreatedAt(LocalDateTime.now().minusDays((category.hashCode() % 40) + 3));
        product.setUpdatedAt(LocalDateTime.now());
        product.setLastUpdated(LocalDateTime.now());
        if (discountPct > 0) {
            product.setDiscount(new BigDecimal(discountPct));
            product.setDiscountType(Product.DiscountType.PERCENTAGE);
            product.setDiscountStartDate(LocalDate.now().minusDays(5));
            product.setDiscountEndDate(LocalDate.now().plusDays(25));
        }
        product.setTags(List.of(category, brand));
        product.setImageUrls(List.of(
                "https://images.upstart.ma/products/" + sku.toLowerCase() + "-1.jpg",
                "https://images.upstart.ma/products/" + sku.toLowerCase() + "-2.jpg"));
        product.getSpecifications().put("material", "Quality grade");
        product.getSpecifications().put("origin", "Morocco");
        product.setTotalSold(reviewCount * 3);
        product.setTotalRevenue(price.multiply(new BigDecimal(reviewCount * 3)));
        return product;
    }

    private void seedDemoData() {
        if (customerUserRepository.count() > 0) {
            log.info("Demo dataset already present - skipping demo data seeding");
            return;
        }

        demoPasswordHash = passwordEncoder.encode(DEMO_PASSWORD);
        log.info("Seeding demo data...");

        List<SuperAdmin> admins = seedSuperAdmins();
        List<CustomerUser> customers = seedCustomers();
        List<DeliveryCompany> deliveryCompanies = seedDeliveryCompanies();
        List<VendorCompany> vendorCompanies = seedVendorCompanies();
        List<DriverPerson> drivers = seedDrivers(deliveryCompanies);
        updateDeliveryCompaniesActiveDriversCount(deliveryCompanies, drivers);
        List<Partnership> partnerships = seedPartnerships(vendorCompanies, deliveryCompanies);
        List<Order> orders = seedOrders(customers, vendorCompanies, deliveryCompanies, drivers, partnerships);

        deliveryCompanyRepository.saveAll(deliveryCompanies);
        vendorCompanyRepository.saveAll(vendorCompanies);
        partnershipRepository.saveAll(partnerships);
        orderRepository.saveAll(orders);

        log.info("Demo data seeded: {} admins, {} customers, {} delivery owners, {} delivery companies, " +
                        "{} vendor owners, {} vendor companies, {} drivers, {} partnerships, {} orders",
                admins.size(), customers.size(), deliveryOwnerRepository.count(),
                deliveryCompanies.size(), vendorOwnerRepository.count(), vendorCompanies.size(),
                drivers.size(), partnerships.size(), orders.size());
    }

    private void updateDeliveryCompaniesActiveDriversCount(List<DeliveryCompany> companies, List<DriverPerson> drivers) {
        for (DeliveryCompany company : companies) {
            long available = drivers.stream()
                    .filter(d -> company.equals(d.getDeliveryCompany()))
                    .filter(d -> Boolean.TRUE.equals(d.getIsAvailable()))
                    .count();
            company.setActiveDriversCount((int) available);
        }
    }

    private List<SuperAdmin> seedSuperAdmins() {
        List<SuperAdmin> admins = new ArrayList<>();

        SuperAdmin sysAdmin = new SuperAdmin(
                "sysadmin",
                "system.admin@upstart.local",
                demoPasswordHash,
                "System",
                "Administrator"
        );
        sysAdmin.setSecurityLevel(8);
        sysAdmin.setLastSystemAccess(LocalDateTime.now().minusHours(2));
        sysAdmin.setSystemPermissions("{\"users\":\"manage\",\"orders\":\"manage\",\"partners\":\"manage\",\"system\":\"admin\"}");
        admins.add(sysAdmin);

        return superAdminRepository.saveAll(admins);
    }

    private List<CustomerUser> seedCustomers() {
        List<CustomerUser> customers = new ArrayList<>();

        CustomerUser c1 = new CustomerUser("ahmedbenali", "ahmed.benali@example.com", demoPasswordHash,
                "Ahmed", "Benali", "+212600000001", "12 Rue Mohammed V, Casablanca");
        c1.setLoyaltyPoints(340);
        c1.setPreferredPaymentMethod("CARD");
        c1.setTotalOrders(24L);
        c1.setTotalSpent(new BigDecimal("12840.50"));
        c1.setDateOfBirth(LocalDate.of(1992, 5, 14));
        c1.setGender("MALE");
        c1.setIsPremium(true);
        c1.setNotificationPreferences("{\"email\":true,\"sms\":true,\"push\":false}");
        customers.add(c1);

        CustomerUser c2 = new CustomerUser("saraelmadi", "sara.elmadi@example.com", demoPasswordHash,
                "Sara", "El Madi", "+212600000002", "45 Avenue Hassan II, Rabat");
        c2.setLoyaltyPoints(95);
        c2.setPreferredPaymentMethod("CASH");
        c2.setTotalOrders(7L);
        c2.setTotalSpent(new BigDecimal("980.25"));
        c2.setDateOfBirth(LocalDate.of(1995, 11, 3));
        c2.setGender("FEMALE");
        customers.add(c2);

        CustomerUser c3 = new CustomerUser("yassinelhaj", "yassine.elhaj@example.com", demoPasswordHash,
                "Yassine", "El Haj", "+212600000003", "88 Boulevard Zerktouni, Casablanca");
        c3.setLoyaltyPoints(520);
        c3.setPreferredPaymentMethod("CARD");
        c3.setTotalOrders(31L);
        c3.setTotalSpent(new BigDecimal("20530.75"));
        c3.setDateOfBirth(LocalDate.of(1988, 8, 22));
        c3.setGender("MALE");
        c3.setIsPremium(true);
        customers.add(c3);

        CustomerUser c4 = new CustomerUser("linafarouk", "lina.farouk@example.com", demoPasswordHash,
                "Lina", "Farouk", "+212600000004", "14 Rue des Fleurs, Marrakech");
        c4.setLoyaltyPoints(60);
        c4.setPreferredPaymentMethod("WALLET");
        c4.setTotalOrders(4L);
        c4.setTotalSpent(new BigDecimal("410.90"));
        c4.setDateOfBirth(LocalDate.of(1999, 2, 17));
        c4.setGender("FEMALE");
        customers.add(c4);

        CustomerUser c5 = new CustomerUser("omaridrissi", "omar.idrissi@example.com", demoPasswordHash,
                "Omar", "Idrissi", "+212600000005", "9 Corniche Boulevard, Tangier");
        c5.setLoyaltyPoints(150);
        c5.setPreferredPaymentMethod("CARD");
        c5.setTotalOrders(12L);
        c5.setTotalSpent(new BigDecimal("3450.00"));
        c5.setDateOfBirth(LocalDate.of(1990, 7, 9));
        c5.setGender("MALE");
        customers.add(c5);

        CustomerUser c6 = new CustomerUser("nouhaallaoui", "nouha.allaoui@example.com", demoPasswordHash,
                "Nouha", "Allaoui", "+212600000006", "230 Avenue des FAR, Fes");
        c6.setLoyaltyPoints(410);
        c6.setPreferredPaymentMethod("CARD");
        c6.setTotalOrders(28L);
        c6.setTotalSpent(new BigDecimal("15200.60"));
        c6.setDateOfBirth(LocalDate.of(1993, 12, 30));
        c6.setGender("FEMALE");
        c6.setIsPremium(true);
        customers.add(c6);

        CustomerUser c7 = new CustomerUser("karimbouzidi", "karim.bouzidi@example.com", demoPasswordHash,
                "Karim", "Bouzidi", "+212600000007", "7 Rue Ibn Sina, Agadir");
        c7.setLoyaltyPoints(40);
        c7.setPreferredPaymentMethod("CASH");
        c7.setTotalOrders(3L);
        c7.setTotalSpent(new BigDecimal("275.45"));
        c7.setDateOfBirth(LocalDate.of(1996, 4, 11));
        c7.setGender("MALE");
        customers.add(c7);

        return customerUserRepository.saveAll(customers);
    }

    private List<DeliveryCompany> seedDeliveryCompanies() {
        List<DeliveryCompany> companies = new ArrayList<>();

        DeliveryOwner owner1 = new DeliveryOwner("deliveryali", "ali.tazi@example.com", demoPasswordHash,
                "Ali", "Tazi", "+212610000001", "DO-2024-0001");
        owner1.setAddress("24 Bis Rue Flandria, Casablanca");
        owner1.setEmergencyContact("+212610000099");
        owner1.setLogisticsExperienceYears(8);
        owner1.setPreferredServiceRegions("Casablanca, Rabat, Tangier");
        owner1.setTransportLicenseNumber("TL-2019-4471");
        owner1.setIsVerifiedOwner(true);
        owner1.setDateOfBirth(LocalDate.of(1985, 3, 19));

        DeliveryOwner owner2 = new DeliveryOwner("fatimazohra", "fatima.bennani@example.com", demoPasswordHash,
                "Fatima Zahra", "Bennani", "+212610000002", "DO-2024-0002");
        owner2.setAddress("31 Marjane District, Fez");
        owner2.setEmergencyContact("+212610000088");
        owner2.setLogisticsExperienceYears(5);
        owner2.setPreferredServiceRegions("Marrakech, Fes, Agadir");
        owner2.setTransportLicenseNumber("TL-2021-8820");
        owner2.setIsVerifiedOwner(true);
        owner2.setDateOfBirth(LocalDate.of(1990, 9, 2));

        deliveryOwnerRepository.saveAll(List.of(owner1, owner2));

        DeliveryCompany co1 = new DeliveryCompany("ExpressLink Logistics", "Casablanca", owner1);
        co1.setCompanyAddress("15 Zone Industrielle, Ain Sebaa, Casablanca");
        co1.setOperatingLicense("DL-LIC-2024-0001");
        co1.setContactPhone("+212522000101");
        co1.setContactEmail("contact@expresslink.ma");
        co1.setManagedZones("Casablanca, Bouskoura, Mohammedia");
        co1.setIsLicensed(true);
        co1.setMaxDrivers(60);
        co1.setTotalDeliveriesManaged(1580L);
        co1.setTotalRevenue(new BigDecimal("482350.00"));
        co1.setCommissionRate(new BigDecimal("0.0700"));
        co1.setRating(new BigDecimal("4.70"));
        co1.setEmergencyContact("+212522000199");
        co1.setOperatingHours("06:00-22:00");
        co1.setVehicleTypesSupported("CAR, VAN, BIKE");
        co1.setRegistrationDate(LocalDateTime.now().minusMonths(28));
        co1.setLastDeliveryDate(LocalDateTime.now().minusHours(3));
        DeliveryCompany dc1 = deliveryCompanyRepository.save(co1);
        companies.add(dc1);

        DeliveryCompany co2 = new DeliveryCompany("Swift Courier Co.", "Rabat", owner1);
        co2.setCompanyAddress("Route de Kenitra, Rabat");
        co2.setOperatingLicense("DL-LIC-2024-0002");
        co2.setContactPhone("+212537000202");
        co2.setContactEmail("hello@swiftcourier.ma");
        co2.setManagedZones("Rabat, Sale, Temara");
        co2.setIsLicensed(true);
        co2.setMaxDrivers(40);
        co2.setTotalDeliveriesManaged(1240L);
        co2.setTotalRevenue(new BigDecimal("315900.00"));
        co2.setCommissionRate(new BigDecimal("0.0600"));
        co2.setRating(new BigDecimal("4.50"));
        co2.setEmergencyContact("+212537000299");
        co2.setOperatingHours("07:00-21:00");
        co2.setVehicleTypesSupported("CAR, SCOOTER");
        co2.setRegistrationDate(LocalDateTime.now().minusMonths(22));
        co2.setLastDeliveryDate(LocalDateTime.now().minusHours(8));
        DeliveryCompany dc2 = deliveryCompanyRepository.save(co2);
        companies.add(dc2);

        DeliveryCompany co3 = new DeliveryCompany("BlueSky Delivery", "Marrakech", owner2);
        co3.setCompanyAddress("Nouvelle Ville, Gueliz, Marrakech");
        co3.setOperatingLicense("DL-LIC-2025-0003");
        co3.setContactPhone("+212524000303");
        co3.setContactEmail("support@bluesky.ma");
        co3.setManagedZones("Marrakech, Tamansourt, Ourika");
        co3.setIsLicensed(true);
        co3.setMaxDrivers(55);
        co3.setTotalDeliveriesManaged(1930L);
        co3.setTotalRevenue(new BigDecimal("521200.00"));
        co3.setCommissionRate(new BigDecimal("0.0750"));
        co3.setRating(new BigDecimal("4.80"));
        co3.setEmergencyContact("+212524000399");
        co3.setOperatingHours("05:30-23:00");
        co3.setVehicleTypesSupported("CAR, VAN, TRUCK");
        co3.setRegistrationDate(LocalDateTime.now().minusMonths(14));
        co3.setLastDeliveryDate(LocalDateTime.now().minusHours(1));
        DeliveryCompany dc3 = deliveryCompanyRepository.save(co3);
        companies.add(dc3);

        DeliveryCompany co4 = new DeliveryCompany("Atlas Express", "Fes", owner2);
        co4.setCompanyAddress("Avenue Slaoui, Fes");
        co4.setOperatingLicense("DL-LIC-2025-0004");
        co4.setContactPhone("+212535000404");
        co4.setContactEmail("info@atlasexpress.ma");
        co4.setManagedZones("Fes, Meknes, Ifrane");
        co4.setIsLicensed(true);
        co4.setMaxDrivers(35);
        co4.setTotalDeliveriesManaged(720L);
        co4.setTotalRevenue(new BigDecimal("198450.00"));
        co4.setCommissionRate(new BigDecimal("0.0550"));
        co4.setRating(new BigDecimal("4.20"));
        co4.setEmergencyContact("+212535000499");
        co4.setOperatingHours("08:00-20:00");
        co4.setVehicleTypesSupported("VAN");
        co4.setRegistrationDate(LocalDateTime.now().minusMonths(9));
        co4.setLastDeliveryDate(LocalDateTime.now().minusHours(26));
        DeliveryCompany dc4 = deliveryCompanyRepository.save(co4);
        companies.add(dc4);

        return companies;
    }

    private List<VendorCompany> seedVendorCompanies() {
        List<VendorCompany> companies = new ArrayList<>();

        VendorOwner owner1 = new VendorOwner("naimabarka", "naima.barka@example.com", demoPasswordHash,
                "Naima", "Barka", "+212620000001", "VO-2023-0001");
        owner1.setAddress("11 Rue du Caire, Casablanca");
        owner1.setEmergencyContact("+212620000099");
        owner1.setBusinessExperienceYears(12);
        owner1.setPreferredBusinessCategory("Restaurants");
        owner1.setIsVerifiedOwner(true);
        owner1.setDateOfBirth(LocalDate.of(1982, 6, 25));

        VendorOwner owner2 = new VendorOwner("jamalhassani", "jamal.hassani@example.com", demoPasswordHash,
                "Jamal", "Hassani", "+212620000002", "VO-2023-0002");
        owner2.setAddress("Quartier Industriel, Tangier");
        owner2.setEmergencyContact("+212620000088");
        owner2.setBusinessExperienceYears(7);
        owner2.setPreferredBusinessCategory("Electronics");
        owner2.setIsVerifiedOwner(true);
        owner2.setDateOfBirth(LocalDate.of(1987, 10, 8));

        VendorOwner owner3 = new VendorOwner("ilyasboukhris", "ilyas.boukhris@example.com", demoPasswordHash,
                "Ilyas", "Boukhris", "+212620000003", "VO-2023-0003");
        owner3.setAddress("Souk El Had, Fes");
        owner3.setEmergencyContact("+212620000077");
        owner3.setBusinessExperienceYears(4);
        owner3.setPreferredBusinessCategory("Grocery");
        owner3.setIsVerifiedOwner(true);
        owner3.setDateOfBirth(LocalDate.of(1991, 1, 15));

        vendorOwnerRepository.saveAll(List.of(owner1, owner2, owner3));

        VendorCompany vc1 = new VendorCompany("FreshBite Market", "12 Rue Atlas, Casablanca", owner1);
        vc1.setBusinessLicense("VC-LIC-2023-0001");
        vc1.setBusinessDescription("Organic groceries and fresh produce delivered fast.");
        vc1.setContactPhone("+212522100111");
        vc1.setContactEmail("orders@freshbite.ma");
        vc1.setIsVerified(true);
        vc1.setRating(new BigDecimal("4.60"));
        vc1.setCommissionRate(new BigDecimal("0.0800"));
        vc1.setTotalOrders(890L);
        vc1.setTotalRevenue(new BigDecimal("250000.00"));
        vc1.setRegistrationDate(LocalDateTime.now().minusMonths(30));
        vc1.setLastOrderDate(LocalDateTime.now().minusHours(2));
        VendorCompany saved1 = vendorCompanyRepository.save(vc1);
        companies.add(saved1);

        VendorCompany vc2 = new VendorCompany("Pizzeria Roma", "5 Boulevard Mohammed V, Rabat", owner1);
        vc2.setBusinessLicense("VC-LIC-2023-0002");
        vc2.setBusinessDescription("Authentic Italian wood-fired pizza and pasta.");
        vc2.setContactPhone("+212537222222");
        vc2.setContactEmail("ciao@pizzeriaroma.ma");
        vc2.setIsVerified(true);
        vc2.setRating(new BigDecimal("4.40"));
        vc2.setCommissionRate(new BigDecimal("0.0700"));
        vc2.setTotalOrders(654L);
        vc2.setTotalRevenue(new BigDecimal("154000.00"));
        vc2.setRegistrationDate(LocalDateTime.now().minusMonths(26));
        vc2.setLastOrderDate(LocalDateTime.now().minusHours(5));
        VendorCompany saved2 = vendorCompanyRepository.save(vc2);
        companies.add(saved2);

        VendorCompany vc3 = new VendorCompany("TechNova Store", "22 Corniche, Tangier", owner2);
        vc3.setBusinessLicense("VC-LIC-2024-0003");
        vc3.setBusinessDescription("Consumer electronics, accessories and smart devices.");
        vc3.setContactPhone("+212539333333");
        vc3.setContactEmail("sales@technova.ma");
        vc3.setIsVerified(true);
        vc3.setRating(new BigDecimal("4.90"));
        vc3.setCommissionRate(new BigDecimal("0.0900"));
        vc3.setTotalOrders(320L);
        vc3.setTotalRevenue(new BigDecimal("890000.00"));
        vc3.setRegistrationDate(LocalDateTime.now().minusMonths(18));
        vc3.setLastOrderDate(LocalDateTime.now().minusDays(1));
        VendorCompany saved3 = vendorCompanyRepository.save(vc3);
        companies.add(saved3);

        VendorCompany vc4 = new VendorCompany("GreenGrocer", "150 Avenue Hassan II, Fes", owner3);
        vc4.setBusinessLicense("VC-LIC-2024-0004");
        vc4.setBusinessDescription("Farm-to-door groceries, dairy and artisan bread.");
        vc4.setContactPhone("+212535444444");
        vc4.setContactEmail("hello@greengrocer.ma");
        vc4.setIsVerified(true);
        vc4.setRating(new BigDecimal("4.30"));
        vc4.setCommissionRate(new BigDecimal("0.0600"));
        vc4.setTotalOrders(1120L);
        vc4.setTotalRevenue(new BigDecimal("320000.00"));
        vc4.setRegistrationDate(LocalDateTime.now().minusMonths(16));
        vc4.setLastOrderDate(LocalDateTime.now().minusHours(6));
        VendorCompany saved4 = vendorCompanyRepository.save(vc4);
        companies.add(saved4);

        VendorCompany vc5 = new VendorCompany("Café Al Baraka", "3 Rue Souk, Marrakech", owner1);
        vc5.setBusinessLicense("VC-LIC-2025-0005");
        vc5.setBusinessDescription("Traditional Moroccan coffee, pastries and light meals.");
        vc5.setContactPhone("+212524555555");
        vc5.setContactEmail("contact@albaraka.ma");
        vc5.setIsVerified(true);
        vc5.setRating(new BigDecimal("4.50"));
        vc5.setCommissionRate(new BigDecimal("0.0400"));
        vc5.setTotalOrders(480L);
        vc5.setTotalRevenue(new BigDecimal("120000.00"));
        vc5.setRegistrationDate(LocalDateTime.now().minusMonths(6));
        vc5.setLastOrderDate(LocalDateTime.now().minusHours(4));
        VendorCompany saved5 = vendorCompanyRepository.save(vc5);
        companies.add(saved5);

        return companies;
    }

    private List<DriverPerson> seedDrivers(List<DeliveryCompany> companies) {
        List<DriverPerson> drivers = new ArrayList<>();

        DeliveryCompany expressLink = companies.get(0);
        DeliveryCompany swift = companies.get(1);
        DeliveryCompany blueSky = companies.get(2);
        DeliveryCompany atlas = companies.get(3);

        DriverPerson d1 = new DriverPerson("youssefbenza", "youssef.benza@example.com", demoPasswordHash,
                "Youssef", "Benza", "DR-LIC-0001", DriverPerson.VehicleType.CAR, "123-A-4567",
                "+212630000001");
        d1.setCurrentLocation("Bouskoura, Casablanca");
        d1.setRating(new BigDecimal("4.80"));
        d1.setTotalDeliveries(410L);
        d1.setTotalEarnings(new BigDecimal("84500.00"));
        d1.setLastActive(LocalDateTime.now().minusMinutes(12));
        d1.setDeliveryZone("Casablanca");
        d1.setEmergencyContact("+212630000099");
        d1.setIsVerified(true);
        d1.setVehicleModel("Dacia Logan");
        d1.setVehicleColor("White");
        d1.setDeliveryCompany(expressLink);
        drivers.add(d1);

        DriverPerson d2 = new DriverPerson("hamidmourad", "hamid.mourad@example.com", demoPasswordHash,
                "Hamid", "Mourad", "DR-LIC-0002", DriverPerson.VehicleType.VAN, "455-B-8901",
                "+212630000002");
        d2.setCurrentLocation("Mohammedia");
        d2.setRating(new BigDecimal("4.50"));
        d2.setTotalDeliveries(280L);
        d2.setTotalEarnings(new BigDecimal("62000.00"));
        d2.setLastActive(LocalDateTime.now().minusHours(1));
        d2.setDeliveryZone("Mohammedia");
        d2.setEmergencyContact("+212630000098");
        d2.setIsVerified(true);
        d2.setVehicleModel("Renault Kangoo");
        d2.setVehicleColor("Blue");
        d2.setDeliveryCompany(expressLink);
        drivers.add(d2);

        DriverPerson d3 = new DriverPerson("salimahmed", "salim.ahmed@example.com", demoPasswordHash,
                "Salim", "Ahmed", "DR-LIC-0003", DriverPerson.VehicleType.BIKE, "SM-90-112",
                "+212630000003");
        d3.setCurrentLocation("Ain Sebaa, Casablanca");
        d3.setRating(new BigDecimal("4.30"));
        d3.setTotalDeliveries(190L);
        d3.setTotalEarnings(new BigDecimal("24000.00"));
        d3.setLastActive(LocalDateTime.now().minusMinutes(45));
        d3.setDeliveryZone("Casablanca");
        d3.setIsVerified(true);
        d3.setVehicleModel("Yamaha NMAX");
        d3.setVehicleColor("Black");
        d3.setDeliveryCompany(expressLink);
        drivers.add(d3);

        DriverPerson d4 = new DriverPerson("khalidnaimi", "khalid.naimi@example.com", demoPasswordHash,
                "Khalid", "Naimi", "DR-LIC-0004", DriverPerson.VehicleType.CAR, "RB-77-3456",
                "+212630000004");
        d4.setCurrentLocation("Rabat");
        d4.setRating(new BigDecimal("4.60"));
        d4.setTotalDeliveries(350L);
        d4.setTotalEarnings(new BigDecimal("73000.00"));
        d4.setLastActive(LocalDateTime.now().minusMinutes(20));
        d4.setDeliveryZone("Rabat");
        d4.setEmergencyContact("+212630000096");
        d4.setIsVerified(true);
        d4.setVehicleModel("Peugeot 208");
        d4.setVehicleColor("Red");
        d4.setDeliveryCompany(swift);
        drivers.add(d4);

        DriverPerson d5 = new DriverPerson("omarfaik", "omar.faik@example.com", demoPasswordHash,
                "Omar", "Faik", "DR-LIC-0005", DriverPerson.VehicleType.SCOOTER, "SL-12-998",
                "+212630000005");
        d5.setCurrentLocation("Sale");
        d5.setRating(new BigDecimal("4.10"));
        d5.setTotalDeliveries(120L);
        d5.setTotalEarnings(new BigDecimal("17500.00"));
        d5.setLastActive(LocalDateTime.now().minusHours(2));
        d5.setDeliveryZone("Sale");
        d5.setIsVerified(true);
        d5.setVehicleModel("Vespa Primavera");
        d5.setVehicleColor("Green");
        d5.setDeliveryCompany(swift);
        drivers.add(d5);

        DriverPerson d6 = new DriverPerson("meryembelm", "meryem.belm@example.com", demoPasswordHash,
                "Meryem", "Belm", "DR-LIC-0006", DriverPerson.VehicleType.CAR, "MA-55-6711",
                "+212630000006");
        d6.setCurrentLocation("Gueliz, Marrakech");
        d6.setRating(new BigDecimal("4.90"));
        d6.setTotalDeliveries(510L);
        d6.setTotalEarnings(new BigDecimal("98000.00"));
        d6.setLastActive(LocalDateTime.now().minusMinutes(5));
        d6.setDeliveryZone("Marrakech");
        d6.setEmergencyContact("+212630000094");
        d6.setIsVerified(true);
        d6.setVehicleModel("Toyota Yaris");
        d6.setVehicleColor("Silver");
        d6.setDeliveryCompany(blueSky);
        drivers.add(d6);

        DriverPerson d7 = new DriverPerson("rchidlamrani", "rchid.lamrani@example.com", demoPasswordHash,
                "Rachid", "Lamrani", "DR-LIC-0007", DriverPerson.VehicleType.TRUCK, "MT-89-2233",
                "+212630000007");
        d7.setCurrentLocation("Ourika Road, Marrakech");
        d7.setRating(new BigDecimal("4.40"));
        d7.setTotalDeliveries(260L);
        d7.setTotalEarnings(new BigDecimal("112000.00"));
        d7.setLastActive(LocalDateTime.now().minusHours(3));
        d7.setDeliveryZone("Marrakech");
        d7.setIsVerified(true);
        d7.setVehicleModel("Ford Transit");
        d7.setVehicleColor("White");
        d7.setDeliveryCompany(blueSky);
        drivers.add(d7);

        DriverPerson d8 = new DriverPerson("hichambouta", "hicham.bouta@example.com", demoPasswordHash,
                "Hicham", "Bouta", "DR-LIC-0008", DriverPerson.VehicleType.VAN, "FS-34-7788",
                "+212630000008");
        d8.setCurrentLocation("Fes");
        d8.setRating(new BigDecimal("4.20"));
        d8.setTotalDeliveries(150L);
        d8.setTotalEarnings(new BigDecimal("38000.00"));
        d8.setLastActive(LocalDateTime.now().minusMinutes(30));
        d8.setDeliveryZone("Fes");
        d8.setIsVerified(true);
        d8.setVehicleModel("Fiat Doblo");
        d8.setVehicleColor("Grey");
        d8.setDeliveryCompany(atlas);
        drivers.add(d8);

        return driverPersonRepository.saveAll(drivers);
    }

    private List<Partnership> seedPartnerships(List<VendorCompany> vendors, List<DeliveryCompany> carriers) {
        List<Partnership> partnerships = new ArrayList<>();

        Partnership p1 = new Partnership();
        p1.setVendorCompany(vendors.get(0));
        p1.setDeliveryCompany(carriers.get(0));
        p1.setStatus(Partnership.PartnershipStatus.ACTIVE);
        p1.setCommissionRate(new BigDecimal("0.0800"));
        p1.setServiceAreas(listOf("Casablanca", "Bouskoura", "Mohammedia"));
        p1.setMinimumOrderValue(new BigDecimal("50.00"));
        p1.setMaximumDeliveryDistanceKm(25.0);
        p1.setEstimatedDeliveryTimeHours(2);
        p1.setPartnershipTerms("Standard 12-month agreement with exclusive weekend slots.");
        p1.setIsExclusive(true);
        p1.setContractStartDate(LocalDateTime.now().minusMonths(11));
        p1.setContractEndDate(LocalDateTime.now().plusMonths(13));
        p1.setTotalOrdersCompleted(560L);
        p1.setTotalRevenueGenerated(new BigDecimal("178000.00"));
        p1.setAverageRating(new BigDecimal("4.70"));
        p1.setTotalRatingsCount(210L);
        p1.setActivatedAt(LocalDateTime.now().minusMonths(11));
        p1.setNotes("Exclusive partner for FreshBite Market in Casablanca.");
        partnerships.add(p1);

        Partnership p2 = new Partnership();
        p2.setVendorCompany(vendors.get(1));
        p2.setDeliveryCompany(carriers.get(1));
        p2.setStatus(Partnership.PartnershipStatus.ACTIVE);
        p2.setCommissionRate(new BigDecimal("0.0700"));
        p2.setServiceAreas(listOf("Rabat", "Sale"));
        p2.setMinimumOrderValue(new BigDecimal("40.00"));
        p2.setMaximumDeliveryDistanceKm(20.0);
        p2.setEstimatedDeliveryTimeHours(1);
        p2.setPartnershipTerms("Standard agreement, 1-hour delivery commitment.");
        p2.setIsExclusive(false);
        p2.setContractStartDate(LocalDateTime.now().minusMonths(8));
        p2.setContractEndDate(LocalDateTime.now().plusMonths(16));
        p2.setTotalOrdersCompleted(340L);
        p2.setTotalRevenueGenerated(new BigDecimal("98000.00"));
        p2.setAverageRating(new BigDecimal("4.50"));
        p2.setTotalRatingsCount(150L);
        p2.setActivatedAt(LocalDateTime.now().minusMonths(8));
        p2.setNotes("Hot-food focused partnership.");
        partnerships.add(p2);

        Partnership p3 = new Partnership();
        p3.setVendorCompany(vendors.get(2));
        p3.setDeliveryCompany(carriers.get(2));
        p3.setStatus(Partnership.PartnershipStatus.ACTIVE);
        p3.setCommissionRate(new BigDecimal("0.0900"));
        p3.setServiceAreas(listOf("Marrakech", "Tamansourt"));
        p3.setMinimumOrderValue(new BigDecimal("100.00"));
        p3.setMaximumDeliveryDistanceKm(30.0);
        p3.setEstimatedDeliveryTimeHours(3);
        p3.setPartnershipTerms("Premium electronics handling and signature-on-delivery.");
        p3.setIsExclusive(true);
        p3.setContractStartDate(LocalDateTime.now().minusMonths(6));
        p3.setContractEndDate(LocalDateTime.now().plusMonths(18));
        p3.setTotalOrdersCompleted(210L);
        p3.setTotalRevenueGenerated(new BigDecimal("412000.00"));
        p3.setAverageRating(new BigDecimal("4.90"));
        p3.setTotalRatingsCount(95L);
        p3.setActivatedAt(LocalDateTime.now().minusMonths(6));
        p3.setNotes("Exclusive for all TechNova deliveries.");
        partnerships.add(p3);

        Partnership p4 = new Partnership();
        p4.setVendorCompany(vendors.get(3));
        p4.setDeliveryCompany(carriers.get(3));
        p4.setStatus(Partnership.PartnershipStatus.ACTIVE);
        p4.setCommissionRate(new BigDecimal("0.0600"));
        p4.setServiceAreas(listOf("Fes", "Meknes"));
        p4.setMinimumOrderValue(new BigDecimal("30.00"));
        p4.setMaximumDeliveryDistanceKm(15.0);
        p4.setEstimatedDeliveryTimeHours(1);
        p4.setPartnershipTerms("Cold-chain grocery handling required.");
        p4.setIsExclusive(false);
        p4.setContractStartDate(LocalDateTime.now().minusMonths(5));
        p4.setContractEndDate(LocalDateTime.now().plusMonths(19));
        p4.setTotalOrdersCompleted(640L);
        p4.setTotalRevenueGenerated(new BigDecimal("185000.00"));
        p4.setAverageRating(new BigDecimal("4.20"));
        p4.setTotalRatingsCount(280L);
        p4.setActivatedAt(LocalDateTime.now().minusMonths(5));
        p4.setNotes("High volume, low margin partnership.");
        partnerships.add(p4);

        Partnership p5 = new Partnership();
        p5.setVendorCompany(vendors.get(4));
        p5.setDeliveryCompany(carriers.get(0));
        p5.setStatus(Partnership.PartnershipStatus.PENDING);
        p5.setCommissionRate(new BigDecimal("0.1000"));
        p5.setServiceAreas(listOf("Marrakech", "Tamansourt"));
        p5.setMinimumOrderValue(new BigDecimal("20.00"));
        p5.setMaximumDeliveryDistanceKm(12.0);
        p5.setEstimatedDeliveryTimeHours(1);
        p5.setPartnershipTerms("Draft agreement awaiting approval.");
        p5.setIsExclusive(false);
        p5.setNotes("Submitted by vendor, awaiting carrier confirmation.");
        partnerships.add(p5);

        Partnership p6 = new Partnership();
        p6.setVendorCompany(vendors.get(0));
        p6.setDeliveryCompany(carriers.get(1));
        p6.setStatus(Partnership.PartnershipStatus.SUSPENDED);
        p6.setCommissionRate(new BigDecimal("0.0700"));
        p6.setServiceAreas(listOf("Rabat"));
        p6.setMinimumOrderValue(new BigDecimal("60.00"));
        p6.setMaximumDeliveryDistanceKm(25.0);
        p6.setEstimatedDeliveryTimeHours(2);
        p6.setIsExclusive(false);
        p6.setContractStartDate(LocalDateTime.now().minusMonths(10));
        p6.setContractEndDate(LocalDateTime.now().minusMonths(1));
        p6.setTotalOrdersCompleted(45L);
        p6.setTotalRevenueGenerated(new BigDecimal("12000.00"));
        p6.setAverageRating(new BigDecimal("3.90"));
        p6.setTotalRatingsCount(30L);
        p6.setActivatedAt(LocalDateTime.now().minusMonths(10));
        p6.setNotes("Suspended due to repeated SLA breaches.");
        partnerships.add(p6);

        return partnershipRepository.saveAll(partnerships);
    }

    private List<Order> seedOrders(List<CustomerUser> customers, List<VendorCompany> vendors,
                                   List<DeliveryCompany> carriers, List<DriverPerson> drivers,
                                   List<Partnership> partnerships) {
        List<Order> orders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        CustomerUser c1 = customers.get(0);
        CustomerUser c2 = customers.get(1);
        CustomerUser c3 = customers.get(2);
        CustomerUser c4 = customers.get(3);
        CustomerUser c5 = customers.get(4);
        CustomerUser c6 = customers.get(5);
        CustomerUser c7 = customers.get(6);

        VendorCompany freshBite = vendors.get(0);
        VendorCompany pizzeria = vendors.get(1);
        VendorCompany techNova = vendors.get(2);
        VendorCompany greenGrocer = vendors.get(3);
        VendorCompany cafe = vendors.get(4);

        DeliveryCompany expressLink = carriers.get(0);
        DeliveryCompany swift = carriers.get(1);
        DeliveryCompany blueSky = carriers.get(2);
        DeliveryCompany atlas = carriers.get(3);

        DriverPerson d1 = drivers.get(0);
        DriverPerson d2 = drivers.get(1);
        DriverPerson d3 = drivers.get(2);
        DriverPerson d4 = drivers.get(3);
        DriverPerson d5 = drivers.get(4);
        DriverPerson d6 = drivers.get(5);
        DriverPerson d7 = drivers.get(6);
        DriverPerson d8 = drivers.get(7);

        Partnership pFreshBiteExpress = partnerships.get(0);
        Partnership pPizzeriaSwift = partnerships.get(1);
        Partnership pTechNovaBlueSky = partnerships.get(2);
        Partnership pGreenGrocerAtlas = partnerships.get(3);

        // Recently delivered orders
        orders.add(buildOrder("ORD-2026", c1, freshBite, expressLink, d1, pFreshBiteExpress,
                "DELIVERED", "NORMAL", "12 Rue Atlas, Casablanca", "12 Rue Mohammed V, Casablanca",
                "245.50", "25.00", 8.4, 6.5, "Weekly grocery restock", "TRK-2026-01001",
                now.minusDays(1).minusHours(2), now.minusDays(1).minusHours(1), 4,
                "Fresh and on time."));
        orders.add(buildOrder("ORD-2026", c3, techNova, blueSky, d6, pTechNovaBlueSky,
                "DELIVERED", "HIGH", "22 Corniche, Tangier", "88 Boulevard Zerktouni, Casablanca",
                "1450.00", "60.00", 3.4, 4.2, "Wireless headphones", "TRK-2026-01002",
                now.minusDays(1).minusHours(5), now.minusDays(1).minusHours(4), 5,
                "Packaging was excellent."));
        orders.add(buildOrder("ORD-2026", c2, pizzeria, swift, d4, pPizzeriaSwift,
                "DELIVERED", "URGENT", "5 Boulevard Mohammed V, Rabat", "45 Avenue Hassan II, Rabat",
                "120.00", "15.00", 2.1, 1.8, "Family pizza order", "TRK-2026-01003",
                now.minusDays(2).minusHours(3), now.minusDays(2).minusHours(2), 4,
                "Still warm when it arrived."));
        orders.add(buildOrder("ORD-2026", c6, greenGrocer, atlas, d8, pGreenGrocerAtlas,
                "DELIVERED", "NORMAL", "150 Avenue Hassan II, Fes", "230 Avenue des FAR, Fes",
                "180.20", "18.00", 3.9, 8.0, "Weekly dairy and bread", "TRK-2026-01004",
                now.minusDays(2).minusHours(6), now.minusDays(2).minusHours(5), 3,
                "Good, but a bit late."));
        orders.add(buildOrder("ORD-2026", c4, cafe, expressLink, d3, null,
                "DELIVERED", "LOW", "3 Rue Souk, Marrakech", "14 Rue des Fleurs, Marrakech",
                "75.00", "20.00", 5.2, 2.0, "Pastries and coffee", "TRK-2026-01005",
                now.minusDays(3).minusHours(2), now.minusDays(3).minusHours(1), 5,
                "Delicious and quick!"));
        orders.add(buildOrder("ORD-2026", c5, freshBite, swift, d5, null,
                "DELIVERED", "NORMAL", "12 Rue Atlas, Casablanca", "9 Corniche Boulevard, Tangier",
                "310.00", "45.00", 12.6, 7.2, "Household essentials", "TRK-2026-01006",
                now.minusDays(3).minusHours(7), now.minusDays(3).minusHours(6), 4,
                "Everything was perfect."));
        orders.add(buildOrder("ORD-2026", c7, greenGrocer, atlas, d8, pGreenGrocerAtlas,
                "DELIVERED", "NORMAL", "150 Avenue Hassan II, Fes", "7 Rue Ibn Sina, Agadir",
                "155.00", "30.00", 9.8, 5.0, "Fruit basket", "TRK-2026-01007",
                now.minusDays(4).minusHours(4), now.minusDays(4).minusHours(3), 4,
                "Fruits were very fresh."));
        orders.add(buildOrder("ORD-2026", c1, pizzeria, expressLink, d2, null,
                "DELIVERED", "HIGH", "5 Boulevard Mohammed V, Rabat", "12 Rue Mohammed V, Casablanca",
                "95.50", "22.00", 1.9, 3.0, "Business lunch", "TRK-2026-01008",
                now.minusDays(4).minusHours(1), now.minusDays(4).minusHours(0), 3,
                "Average."));
        orders.add(buildOrder("ORD-2026", c2, techNova, blueSky, d7, pTechNovaBlueSky,
                "DELIVERED", "HIGH", "22 Corniche, Tangier", "45 Avenue Hassan II, Rabat",
                "720.00", "55.00", 2.8, 5.5, "Smart watch", "TRK-2026-01009",
                now.minusDays(5).minusHours(3), now.minusDays(5).minusHours(2), 5,
                "Fast and secure delivery."));
        orders.add(buildOrder("ORD-2026", c6, cafe, expressLink, d1, null,
                "DELIVERED", "LOW", "3 Rue Souk, Marrakech", "230 Avenue des FAR, Fes",
                "45.00", "28.00", 7.4, 1.5, "Artisan honey", "TRK-2026-01010",
                now.minusDays(5).minusHours(8), now.minusDays(5).minusHours(7), 4,
                "Honey jar was sealed perfectly."));
        orders.add(buildOrder("ORD-2026", c3, freshBite, expressLink, d2, pFreshBiteExpress,
                "DELIVERED", "URGENT", "12 Rue Atlas, Casablanca", "88 Boulevard Zerktouni, Casablanca",
                "430.00", "28.00", 5.6, 9.0, "Premium grocery order", "TRK-2026-01011",
                now.minusDays(6).minusHours(2), now.minusDays(6).minusHours(1), 5,
                "Outstanding service."));
        orders.add(buildOrder("ORD-2026", c5, pizzeria, swift, d4, pPizzeriaSwift,
                "DELIVERED", "NORMAL", "5 Boulevard Mohammed V, Rabat", "9 Corniche Boulevard, Tangier",
                "210.00", "18.00", 3.0, 4.0, "Pasta boxes", "TRK-2026-01012",
                now.minusDays(6).minusHours(5), now.minusDays(6).minusHours(4), 4,
                "Great."));
        orders.add(buildOrder("ORD-2026", c7, techNova, blueSky, d6, null,
                "DELIVERED", "HIGH", "22 Corniche, Tangier", "7 Rue Ibn Sina, Agadir",
                "560.00", "42.00", 6.1, 3.2, "Tablet and case", "TRK-2026-01013",
                now.minusDays(7).minusHours(4), now.minusDays(7).minusHours(3), 4,
                "On time."));
        orders.add(buildOrder("ORD-2026", c4, freshBite, expressLink, d3, pFreshBiteExpress,
                "COMPLETED", "NORMAL", "12 Rue Atlas, Casablanca", "14 Rue des Fleurs, Marrakech",
                "265.00", "24.00", 6.8, 6.0, "Coffee and snacks", "TRK-2026-01014",
                now.minusDays(8).minusHours(1), now.minusDays(8).minusHours(0), 4,
                "Nice driver."));
        orders.add(buildOrder("ORD-2026", c2, greenGrocer, atlas, d8, pGreenGrocerAtlas,
                "COMPLETED", "NORMAL", "150 Avenue Hassan II, Fes", "45 Avenue Hassan II, Rabat",
                "140.00", "20.00", 4.2, 6.5, "Kitchen staples", "TRK-2026-01015",
                now.minusDays(9).minusHours(2), now.minusDays(9).minusHours(1), 3,
                "Slightly damaged carton."));

        // In-progress orders
        orders.add(buildOrder("ORD-2026", c1, freshBite, expressLink, d1, pFreshBiteExpress,
                "PICKED_UP", "HIGH", "12 Rue Atlas, Casablanca", "12 Rue Mohammed V, Casablanca",
                "320.00", "22.00", 4.5, 7.8, "Grocery delivery", "TRK-2026-02001",
                now.plusHours(1), null, 0, null));
        orders.add(buildOrder("ORD-2026", c3, techNova, blueSky, d6, pTechNovaBlueSky,
                "IN_TRANSIT", "URGENT", "22 Corniche, Tangier", "88 Boulevard Zerktouni, Casablanca",
                "890.00", "58.00", 3.1, 4.6, "Laptop charger", "TRK-2026-02002",
                now.plusHours(2), null, 0, null));
        orders.add(buildOrder("ORD-2026", c6, pizzeria, swift, d4, pPizzeriaSwift,
                "IN_PROGRESS", "NORMAL", "5 Boulevard Mohammed V, Rabat", "230 Avenue des FAR, Fes",
                "65.00", "16.00", 2.3, 2.0, "Lunch pasta", "TRK-2026-02003",
                now.plusHours(1), null, 0, null));
        orders.add(buildOrder("ORD-2026", c5, greenGrocer, atlas, d8, pGreenGrocerAtlas,
                "PICKED_UP", "NORMAL", "150 Avenue Hassan II, Fes", "9 Corniche Boulevard, Tangier",
                "98.50", "26.00", 5.5, 4.4, "Fresh produce", "TRK-2026-02004",
                now.plusHours(3), null, 0, null));
        orders.add(buildOrder("ORD-2026", c2, cafe, expressLink, d2, null,
                "IN_TRANSIT", "LOW", "3 Rue Souk, Marrakech", "45 Avenue Hassan II, Rabat",
                "52.00", "30.00", 8.0, 3.0, "Moroccan tea set", "TRK-2026-02005",
                now.plusHours(2), null, 0, null));
        orders.add(buildOrder("ORD-2026", c7, freshBite, expressLink, d3, pFreshBiteExpress,
                "ASSIGNED", "NORMAL", "12 Rue Atlas, Casablanca", "7 Rue Ibn Sina, Agadir",
                "180.00", "35.00", 7.9, 6.2, "Breakfast items", "TRK-2026-02006",
                now.plusHours(4), null, 0, null));

        // Just confirmed / recently assigned
        orders.add(buildOrder("ORD-2026", c3, techNova, blueSky, null, pTechNovaBlueSky,
                "ASSIGNED", "HIGH", "22 Corniche, Tangier", "88 Boulevard Zerktouni, Casablanca",
                "640.00", "50.00", 3.6, 3.8, "Smart speaker", "TRK-2026-03001",
                now.plusHours(5), null, 0, null));
        orders.add(buildOrder("ORD-2026", c4, pizzeria, swift, null, pPizzeriaSwift,
                "CONFIRMED", "NORMAL", "5 Boulevard Mohammed V, Rabat", "14 Rue des Fleurs, Marrakech",
                "85.00", "24.00", 6.3, 1.5, "Dessert box", "TRK-2026-03002",
                now.plusHours(6), null, 0, null));
        orders.add(buildOrder("ORD-2026", c1, greenGrocer, atlas, null, pGreenGrocerAtlas,
                "CONFIRMED", "NORMAL", "150 Avenue Hassan II, Fes", "12 Rue Mohammed V, Casablanca",
                "125.00", "28.00", 4.8, 5.5, "Household goods", "TRK-2026-03003",
                now.plusHours(8), null, 0, null));
        orders.add(buildOrder("ORD-2026", c5, cafe, expressLink, null, null,
                "ASSIGNED", "LOW", "3 Rue Souk, Marrakech", "9 Corniche Boulevard, Tangier",
                "60.00", "32.00", 9.1, 2.5, "Olive oil", "TRK-2026-03004",
                now.plusHours(7), null, 0, null));

        // Pending (awaiting assignment)
        orders.add(buildOrder("ORD-2026", c2, freshBite, null, null, null,
                "PENDING", "NORMAL", "12 Rue Atlas, Casablanca", "45 Avenue Hassan II, Rabat",
                "210.00", null, 4.0, 8.0, "Grocery order", "TRK-2026-04001",
                now.plusDays(1), null, 0, null));
        orders.add(buildOrder("ORD-2026", c6, techNova, null, null, null,
                "PENDING", "URGENT", "22 Corniche, Tangier", "230 Avenue des FAR, Fes",
                "1120.00", null, 2.5, 6.0, "Laptop", "TRK-2026-04002",
                now.plusDays(1), null, 0, null));
        orders.add(buildOrder("ORD-2026", c1, pizzeria, null, null, null,
                "PENDING", "NORMAL", "5 Boulevard Mohammed V, Rabat", "12 Rue Mohammed V, Casablanca",
                "150.00", null, 1.7, 3.0, "Weekend family meal", "TRK-2026-04003",
                now.plusDays(2), null, 0, null));
        orders.add(buildOrder("ORD-2026", c5, greenGrocer, null, null, null,
                "PENDING", "LOW", "150 Avenue Hassan II, Fes", "9 Corniche Boulevard, Tangier",
                "79.90", null, 6.0, 4.0, "Shelf staples", "TRK-2026-04004",
                now.plusDays(2), null, 0, null));
        orders.add(buildOrder("ORD-2026", c3, cafe, null, null, null,
                "PENDING", "NORMAL", "3 Rue Souk, Marrakech", "88 Boulevard Zerktouni, Casablanca",
                "120.00", null, 5.3, 3.5, "Coffee beans bundle", "TRK-2026-04005",
                now.plusDays(1), null, 0, null));
        orders.add(buildOrder("ORD-2026", c7, freshBite, null, null, null,
                "PENDING", "NORMAL", "12 Rue Atlas, Casablanca", "7 Rue Ibn Sina, Agadir",
                "95.00", null, 8.5, 7.0, "Pantry restock", "TRK-2026-04006",
                now.plusDays(3), null, 0, null));
        orders.add(buildOrder("ORD-2026", c4, techNova, null, null, null,
                "PENDING", "HIGH", "22 Corniche, Tangier", "14 Rue des Fleurs, Marrakech",
                "350.00", null, 2.9, 2.2, "Bluetooth speaker", "TRK-2026-04007",
                now.plusDays(2), null, 0, null));

        // Cancelled / failed
        orders.add(buildOrder("ORD-2026", c2, freshBite, expressLink, d1, null,
                "CANCELLED", "NORMAL", "12 Rue Atlas, Casablanca", "45 Avenue Hassan II, Rabat",
                "88.00", "18.00", 4.4, 3.0, "Customer cancelled", "TRK-2026-05001",
                now.minusDays(2), null, 0, null));
        orders.add(buildOrder("ORD-2026", c6, pizzeria, swift, d5, null,
                "CANCELLED", "NORMAL", "5 Boulevard Mohammed V, Rabat", "230 Avenue des FAR, Fes",
                "110.00", "20.00", 2.0, 2.5, "Out of stock", "TRK-2026-05002",
                now.minusDays(4), null, 0, null));
        orders.add(buildOrder("ORD-2026", c1, techNova, blueSky, null, null,
                "FAILED", "HIGH", "22 Corniche, Tangier", "12 Rue Mohammed V, Casablanca",
                "410.00", null, 3.8, 2.0, "Address unreachable", "TRK-2026-05003",
                now.minusDays(3), null, 0, null));
        orders.add(buildOrder("ORD-2026", c5, greenGrocer, null, null, null,
                "CANCELLED", "LOW", "150 Avenue Hassan II, Fes", "9 Corniche Boulevard, Tangier",
                "54.00", null, 5.0, 3.5, "Duplicate order", "TRK-2026-05004",
                now.minusDays(5), null, 0, null));

        return orderRepository.saveAll(orders);
    }

    private int orderSeq = 0;

    private List<String> listOf(String... values) {
        return new ArrayList<>(List.of(values));
    }

    private Order buildOrder(String orderPrefix, CustomerUser customer, VendorCompany vendor,
                             DeliveryCompany delivery, DriverPerson driver, Partnership partnership,
                             String status, String priority,
                             String pickupAddress, String deliveryAddress,
                             String amount, String fee,
                             double distanceKm, double weightKg,
                             String description, String tracking,
                             LocalDateTime estimatedDeliveryTime, LocalDateTime actualDeliveryTime,
                             int rating, String review) {
        orderSeq++;
        Order order = new Order();
        order.setOrderNumber(String.format("%s-%04d", orderPrefix, orderSeq));
        order.setCustomerUser(customer);
        order.setVendorCompany(vendor);
        order.setDeliveryCompany(delivery);
        order.setDriverPerson(driver);
        order.setPartnership(partnership);
        order.setStatus(Order.OrderStatus.valueOf(status));
        order.setPriority(Order.OrderPriority.valueOf(priority));
        order.setPickupAddress(pickupAddress);
        order.setDeliveryAddress(deliveryAddress);
        order.setPickupLatitude(33.5731 + (orderSeq % 5) * 0.02);
        order.setPickupLongitude(-7.5898 + (orderSeq % 4) * 0.03);
        order.setDeliveryLatitude(33.5731 + (orderSeq % 3) * 0.05);
        order.setDeliveryLongitude(-7.5898 + (orderSeq % 6) * 0.04);
        order.setOrderAmount(new BigDecimal(amount));
        order.setDeliveryFee(fee != null ? new BigDecimal(fee) : null);
        order.setTotalAmount(fee != null
                ? new BigDecimal(amount).add(new BigDecimal(fee))
                : new BigDecimal(amount));
        order.setDistanceKm(distanceKm);
        order.setWeightKg(weightKg);
        order.setPackageDimensions("30x20x15 cm");
        order.setIsFragile(distanceKm > 6.0);
        order.setRequiresSignature(rating >= 5);
        order.setDescription(description);
        order.setSpecialInstructions("Leave with reception if no answer.");
        order.setTrackingNumber(tracking);
        order.setEstimatedDeliveryTime(estimatedDeliveryTime);
        order.setActualDeliveryTime(actualDeliveryTime);
        order.setScheduledPickupTime(estimatedDeliveryTime.minusHours(2));
        order.setScheduledDeliveryTime(estimatedDeliveryTime);
        order.setAssignedAt(delivery != null ? estimatedDeliveryTime.minusHours(3) : null);
        order.setRating(rating > 0 ? rating : null);
        order.setReview(rating > 0 ? review : null);
        return order;
    }
}