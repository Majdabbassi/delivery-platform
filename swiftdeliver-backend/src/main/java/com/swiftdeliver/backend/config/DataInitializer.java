package com.swiftdeliver.backend.config;

import com.swiftdeliver.backend.entity.*;
import com.swiftdeliver.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    private static final String DEMO_PASSWORD = "azerty";

    @Bean
    CommandLineRunner seedData(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            VendorOwnerRepository vendorOwnerRepository,
            DeliveryOwnerRepository deliveryOwnerRepository,
            CustomerUserRepository customerUserRepository,
            DriverPersonRepository driverPersonRepository,
            VendorCompanyRepository vendorCompanyRepository,
            DeliveryCompanyRepository deliveryCompanyRepository,
            ProductRepository productRepository,
            PartnershipRepository partnershipRepository,
            OrderRepository orderRepository,
            BidRepository bidRepository,
            PooledOrderRepository pooledOrderRepository) {
        return args -> seed(passwordEncoder, userRepository, vendorOwnerRepository,
                deliveryOwnerRepository, customerUserRepository, driverPersonRepository,
                vendorCompanyRepository, deliveryCompanyRepository, productRepository,
                partnershipRepository, orderRepository, bidRepository, pooledOrderRepository);
    }

    @Transactional
    void seed(PasswordEncoder passwordEncoder, UserRepository userRepository,
              VendorOwnerRepository vendorOwnerRepository, DeliveryOwnerRepository deliveryOwnerRepository,
              CustomerUserRepository customerUserRepository, DriverPersonRepository driverPersonRepository,
              VendorCompanyRepository vendorCompanyRepository, DeliveryCompanyRepository deliveryCompanyRepository,
              ProductRepository productRepository, PartnershipRepository partnershipRepository,
              OrderRepository orderRepository, BidRepository bidRepository,
              PooledOrderRepository pooledOrderRepository) {
        String password = passwordEncoder.encode(DEMO_PASSWORD);

        SuperAdmin admin = userRepository.findByUsername("demo.admin")
                .filter(SuperAdmin.class::isInstance)
                .map(SuperAdmin.class::cast)
                .or(() -> userRepository.findByEmail("admin@swiftdeliver.demo")
                        .filter(SuperAdmin.class::isInstance)
                        .map(SuperAdmin.class::cast))
                .orElseGet(() -> {
                    SuperAdmin value = new SuperAdmin("demo.admin", "admin@swiftdeliver.demo", password, "Maya", "Admin");
                    value.setPhoneNumber("+212600000001");
                    value.setSystemPermissions("{\"users\":true,\"companies\":true,\"reports\":true}");
                    value.setLastSystemAccess(LocalDateTime.now());
                    return userRepository.save(value);
                });
        admin.setPassword(password);
        admin.setUsername("admin");
        admin.setEmail("admin@swiftdeliver.demo");
        admin.setFirstName("super");
        admin.setLastName("Admin");
        admin.setPhoneNumber("+212600000001");
        admin.setSystemPermissions("{\"users\":true,\"companies\":true,\"reports\":true}");
        admin.setLastSystemAccess(LocalDateTime.now());
        userRepository.save(admin);

        VendorOwner vendorOne = vendorOwner("vendor.sofia", "sofia@swiftdeliver.demo", "Sofia", "Bennani", "+212600000002", "MA-V-10001", "Food & Grocery", vendorOwnerRepository, password);
        VendorOwner vendorTwo = vendorOwner("vendor.youssef", "youssef@swiftdeliver.demo", "Youssef", "Alaoui", "+212600000003", "MA-V-10002", "Electronics", vendorOwnerRepository, password);
        DeliveryOwner deliveryOne = deliveryOwner("delivery.nadia", "nadia@swiftdeliver.demo", "Nadia", "Tazi", "+212600000004", "MA-D-20001", "Casablanca, Mohammedia", deliveryOwnerRepository, password);
        DeliveryOwner deliveryTwo = deliveryOwner("delivery.omar", "omar@swiftdeliver.demo", "Omar", "Fassi", "+212600000005", "MA-D-20002", "Rabat, Sale, Temara", deliveryOwnerRepository, password);

        CustomerUser customerOne = customer("customer.amina", "amina@swiftdeliver.demo", "Amina", "Idrissi", "+212600000006", "12 Rue Atlas, Casablanca", customerUserRepository, password);
        CustomerUser customerTwo = customer("customer.karim", "karim@swiftdeliver.demo", "Karim", "Naciri", "+212600000007", "8 Avenue Mohammed V, Rabat", customerUserRepository, password);
        CustomerUser customerThree = customer("customer.leila", "leila@swiftdeliver.demo", "Leila", "Berrada", "+212600000008", "45 Rue des Fleurs, Casablanca", customerUserRepository, password);
        customerOne.setLoyaltyPoints(420); customerOne.setIsPremium(true);
        customerTwo.setLoyaltyPoints(185);
        customerThree.setLoyaltyPoints(760); customerThree.setIsPremium(true);
        customerUserRepository.saveAll(List.of(customerOne, customerTwo, customerThree));

        VendorCompany atlas = vendorCompany("Atlas Market", "LIC-V-30001", "Casablanca", vendorOne, vendorCompanyRepository);
        VendorCompany techHub = vendorCompany("TechHub Maroc", "LIC-V-30002", "Rabat", vendorTwo, vendorCompanyRepository);
        VendorCompany casaHome = vendorCompany("Casa Home & Living", "LIC-V-30003", "Casablanca", vendorOne, vendorCompanyRepository);
        DeliveryCompany swift = deliveryCompany("SwiftDrop Casablanca", "LIC-D-40001", "Casablanca", deliveryOne, deliveryCompanyRepository);
        DeliveryCompany city = deliveryCompany("CityLink Rabat", "LIC-D-40002", "Rabat", deliveryTwo, deliveryCompanyRepository);

        DriverPerson driverOne = driver("driver.ayoub", "ayoub@swiftdeliver.demo", "Ayoub", "El Mansouri", "LIC-DRV-50001", DriverPerson.VehicleType.SCOOTER, "A-12345", "+212600000009", swift, driverPersonRepository, password);
        DriverPerson driverTwo = driver("driver.salma", "salma@swiftdeliver.demo", "Salma", "Chraibi", "LIC-DRV-50002", DriverPerson.VehicleType.CAR, "B-23456", "+212600000010", swift, driverPersonRepository, password);
        DriverPerson driverThree = driver("driver.hamza", "hamza@swiftdeliver.demo", "Hamza", "Kabbaj", "LIC-DRV-50003", DriverPerson.VehicleType.VAN, "C-34567", "+212600000011", city, driverPersonRepository, password);
        DriverPerson driverFour = driver("driver.meriem", "meriem@swiftdeliver.demo", "Meriem", "Fadili", "LIC-DRV-50004", DriverPerson.VehicleType.BIKE, "D-45678", "+212600000012", city, driverPersonRepository, password);
        driverOne.setIsVerified(true); driverOne.setRating(new BigDecimal("4.80")); driverOne.setTotalDeliveries(238L);
        driverTwo.setIsVerified(true); driverTwo.setRating(new BigDecimal("4.60")); driverTwo.setTotalDeliveries(164L);
        driverThree.setIsVerified(true); driverThree.setRating(new BigDecimal("4.90")); driverThree.setTotalDeliveries(311L);
        driverFour.setIsVerified(true); driverFour.setRating(new BigDecimal("4.50")); driverFour.setTotalDeliveries(97L);
        driverPersonRepository.saveAll(List.of(driverOne, driverTwo, driverThree, driverFour));

        seedProducts(productRepository, atlas, techHub, casaHome);

        Partnership atlasSwift = partnership(atlas, swift, Partnership.PartnershipStatus.ACTIVE, "Casablanca", partnershipRepository);
        Partnership techCity = partnership(techHub, city, Partnership.PartnershipStatus.ACTIVE, "Rabat", partnershipRepository);
        partnership(casaHome, swift, Partnership.PartnershipStatus.PENDING, "Casablanca", partnershipRepository);

        Order orderOne = order("ORD-DEMO-1001", atlas, customerOne, swift, driverOne, atlasSwift, Order.OrderStatus.DELIVERED, Order.OrderPriority.NORMAL, "Market groceries", "Atlas Market, Casablanca", "38 Boulevard Zerktouni, Casablanca", new BigDecimal("235.00"), new BigDecimal("18.00"), orderRepository);
        Order orderTwo = order("ORD-DEMO-1002", techHub, customerTwo, city, driverThree, techCity, Order.OrderStatus.IN_TRANSIT, Order.OrderPriority.HIGH, "Laptop accessories", "TechHub Rabat", "8 Avenue Mohammed V, Rabat", new BigDecimal("890.00"), new BigDecimal("30.00"), orderRepository);
        Order orderFour = order("ORD-DEMO-1004", atlas, customerTwo, null, null, null, Order.OrderStatus.PENDING, Order.OrderPriority.URGENT, "Fresh produce basket", "Atlas Market, Casablanca", "8 Avenue Mohammed V, Rabat", new BigDecimal("180.00"), new BigDecimal("25.00"), orderRepository);
        Order orderFive = order("ORD-DEMO-1005", techHub, customerOne, null, null, null, Order.OrderStatus.PENDING, Order.OrderPriority.HIGH, "Wireless keyboard and mouse", "TechHub Rabat", "12 Rue Atlas, Casablanca", new BigDecimal("650.00"), new BigDecimal("28.00"), orderRepository);
        order("ORD-DEMO-1006", casaHome, customerThree, city, driverFour, techCity, Order.OrderStatus.ASSIGNED, Order.OrderPriority.NORMAL, "Bedroom lamp", "Casa Home, Casablanca", "45 Rue des Fleurs, Casablanca", new BigDecimal("310.00"), new BigDecimal("20.00"), orderRepository);
        order("ORD-DEMO-1007", atlas, customerOne, swift, driverTwo, atlasSwift, Order.OrderStatus.CANCELLED, Order.OrderPriority.LOW, "Weekly groceries", "Atlas Market, Casablanca", "12 Rue Atlas, Casablanca", new BigDecimal("95.00"), new BigDecimal("15.00"), orderRepository);
        order("ORD-DEMO-1008", techHub, customerTwo, swift, driverOne, null, Order.OrderStatus.DELIVERED, Order.OrderPriority.HIGH, "Smart home starter kit", "TechHub Rabat", "8 Avenue Mohammed V, Rabat", new BigDecimal("1250.00"), new BigDecimal("35.00"), orderRepository);

        bid(bidRepository, "BID-DEMO-1001", orderOne, swift, driverOne, new BigDecimal("22.00"), Bid.BidStatus.ACCEPTED);
        bid(bidRepository, "BID-DEMO-1002", orderOne, city, driverThree, new BigDecimal("25.00"), Bid.BidStatus.REJECTED);
        bid(bidRepository, "BID-DEMO-1003", orderTwo, swift, driverOne, new BigDecimal("34.00"), Bid.BidStatus.SUBMITTED);
        bid(bidRepository, "BID-DEMO-1004", orderTwo, city, driverThree, new BigDecimal("31.00"), Bid.BidStatus.WITHDRAWN);
        pool(pooledOrderRepository, swift, orderFour); pool(pooledOrderRepository, city, orderFour);
        pool(pooledOrderRepository, swift, orderFive); pool(pooledOrderRepository, city, orderFive);
    }

    private VendorOwner vendorOwner(String username, String email, String firstName, String lastName, String phone, String nationalId, String category, VendorOwnerRepository repository, String password) {
        return repository.findByNationalId(nationalId).orElseGet(() -> {
            VendorOwner owner = new VendorOwner(username, email, password, firstName, lastName, phone, nationalId);
            owner.setDateOfBirth(LocalDate.of(1988, 4, 12)); owner.setAddress("Casablanca, Morocco"); owner.setEmergencyContact("+212611111111");
            owner.setBusinessExperienceYears(8); owner.setPreferredBusinessCategory(category); owner.setIsVerifiedOwner(true);
            return repository.save(owner);
        });
    }

    private DeliveryOwner deliveryOwner(String username, String email, String firstName, String lastName, String phone, String nationalId, String regions, DeliveryOwnerRepository repository, String password) {
        return repository.findByNationalId(nationalId).orElseGet(() -> {
            DeliveryOwner owner = new DeliveryOwner(username, email, password, firstName, lastName, phone, nationalId);
            owner.setDateOfBirth(LocalDate.of(1985, 9, 22)); owner.setAddress("Morocco"); owner.setEmergencyContact("+212622222222");
            owner.setLogisticsExperienceYears(10); owner.setPreferredServiceRegions(regions); owner.setTransportLicenseNumber("TRANSPORT-" + nationalId); owner.setIsVerifiedOwner(true);
            return repository.save(owner);
        });
    }

    private CustomerUser customer(String username, String email, String firstName, String lastName, String phone, String address, CustomerUserRepository repository, String password) {
        return repository.findByUsername(username).orElseGet(() -> {
            CustomerUser customer = new CustomerUser(username, email, password, firstName, lastName, phone, address);
            customer.setDateOfBirth(LocalDate.of(1992, 6, 15)); customer.setGender("PREFER_NOT_TO_SAY"); customer.setPreferredPaymentMethod("CARD");
            customer.setNotificationPreferences("{\"email\":true,\"sms\":true,\"push\":true}"); return repository.save(customer);
        });
    }

    private DriverPerson driver(String username, String email, String firstName, String lastName, String license, DriverPerson.VehicleType vehicleType, String plate, String phone, DeliveryCompany company, DriverPersonRepository repository, String password) {
        return repository.findByLicenseNumber(license).orElseGet(() -> {
            DriverPerson driver = new DriverPerson(username, email, password, firstName, lastName, license, vehicleType, plate, phone);
            driver.setDeliveryCompany(company); driver.setCurrentLocation(company.getServiceRegion()); driver.setDeliveryZone(company.getServiceRegion());
            driver.setVehicleModel(vehicleType == DriverPerson.VehicleType.BIKE ? "City Bike" : "Urban Express"); driver.setVehicleColor("White"); driver.setEmergencyContact("+212633333333");
            return repository.save(driver);
        });
    }

    private VendorCompany vendorCompany(String name, String license, String city, VendorOwner owner, VendorCompanyRepository repository) {
        return repository.findByBusinessLicense(license).orElseGet(() -> {
            VendorCompany company = new VendorCompany(name, city + ", Morocco", owner);
            company.setBusinessLicense(license); company.setBusinessDescription("Verified local business serving customers through SwiftDeliver."); company.setContactPhone("+212520000001");
            company.setContactEmail(name.toLowerCase().replace(" ", ".") + "@swiftdeliver.demo"); company.setIsVerified(true); company.setRating(new BigDecimal("4.60"));
            company.setCommissionRate(new BigDecimal("0.1000")); return repository.save(company);
        });
    }

    private DeliveryCompany deliveryCompany(String name, String license, String city, DeliveryOwner owner, DeliveryCompanyRepository repository) {
        return repository.findByOperatingLicense(license).orElseGet(() -> {
            DeliveryCompany company = new DeliveryCompany(name, city, owner);
            company.setOperatingLicense(license); company.setCompanyAddress(city + ", Morocco"); company.setContactPhone("+212530000001");
            company.setContactEmail(name.toLowerCase().replace(" ", ".") + "@swiftdeliver.demo"); company.setManagedZones(city + ", Downtown, Residential");
            company.setIsLicensed(true); company.setRating(new BigDecimal("4.70")); company.setOperatingHours("06:00-22:00"); company.setVehicleTypesSupported("BIKE,SCOOTER,CAR,VAN");
            return repository.save(company);
        });
    }

    private void seedProducts(ProductRepository repository, VendorCompany atlas, VendorCompany techHub, VendorCompany casaHome) {
        String[][] data = {
                {"Organic Olive Oil","Food","Pantry","Atlas Select","79.90","120","Atlas Market"},{"Basmati Rice 5kg","Food","Pantry","Atlas Select","68.00","85","Atlas Market"},{"Premium Dates","Food","Snacks","Sahara Gold","45.00","200","Atlas Market"},{"Fresh Orange Juice","Food","Beverages","Citrus Farm","24.50","75","Atlas Market"},{"Green Tea Collection","Food","Beverages","Nour Tea","38.00","140","Atlas Market"},{"Almond Granola","Food","Breakfast","Healthy Start","52.00","90","Atlas Market"},{"Honey 500g","Food","Pantry","Rif Honey","62.00","110","Atlas Market"},{"Dark Chocolate Box","Food","Snacks","Cacao Maison","89.00","60","Atlas Market"},{"Couscous 1kg","Food","Pantry","Atlas Select","18.00","180","Atlas Market"},{"Spice Gift Set","Food","Pantry","Marrakech Spice","74.00","45","Atlas Market"},
                {"Wireless Keyboard","Electronics","Accessories","KeyPro","299.00","70","TechHub Maroc"},{"Wireless Mouse","Electronics","Accessories","KeyPro","169.00","95","TechHub Maroc"},{"USB-C Hub","Electronics","Accessories","ConnectX","249.00","55","TechHub Maroc"},{"Noise Cancelling Headphones","Electronics","Audio","SonicWave","899.00","34","TechHub Maroc"},{"Bluetooth Speaker","Electronics","Audio","SonicWave","499.00","42","TechHub Maroc"},{"1080p Webcam","Electronics","Computer","ViewPoint","379.00","38","TechHub Maroc"},{"Portable SSD 1TB","Electronics","Storage","DataFast","999.00","26","TechHub Maroc"},{"Smart LED Bulb","Electronics","Smart Home","BrightHome","119.00","120","TechHub Maroc"},{"Power Bank 20000mAh","Electronics","Accessories","VoltGo","229.00","80","TechHub Maroc"},{"Laptop Stand","Electronics","Computer","DeskEase","189.00","44","TechHub Maroc"},
                {"Ceramic Dinner Set","Home","Kitchen","Casa Living","349.00","32","Casa Home & Living"},{"Cotton Bed Linen","Home","Bedroom","SoftNest","279.00","48","Casa Home & Living"},{"Decorative Table Lamp","Home","Decor","LumiCasa","310.00","25","Casa Home & Living"},{"Bamboo Storage Basket","Home","Storage","EcoCasa","99.00","76","Casa Home & Living"},{"Scented Candle Set","Home","Decor","Calm Space","129.00","90","Casa Home & Living"},{"Memory Foam Pillow","Home","Bedroom","SleepWell","159.00","61","Casa Home & Living"},{"Kitchen Knife Set","Home","Kitchen","ChefPro","229.00","29","Casa Home & Living"},{"Wall Clock","Home","Decor","TimeCraft","149.00","33","Casa Home & Living"},{"Plant Pot Set","Home","Decor","GreenCorner","109.00","80","Casa Home & Living"},{"Microfiber Towel Set","Home","Bathroom","PureCotton","139.00","72","Casa Home & Living"},{"Aromatherapy Diffuser","Home","Wellness","Calm Space","249.00","37","Casa Home & Living"},{"Reusable Water Bottle","Lifestyle","Wellness","Hydra","89.00","150","Casa Home & Living"},{"Travel Organizer","Lifestyle","Travel","PackSmart","119.00","64","Casa Home & Living"},{"Canvas Backpack","Lifestyle","Bags","UrbanCarry","239.00","41","Casa Home & Living"},{"Desk Organizer","Lifestyle","Office","DeskEase","79.00","88","Casa Home & Living"},{"Yoga Mat","Lifestyle","Fitness","MoveWell","179.00","50","Casa Home & Living"},{"Resistance Bands","Lifestyle","Fitness","MoveWell","99.00","70","Casa Home & Living"},{"Weekly Planner","Lifestyle","Office","PaperWorks","59.00","115","Casa Home & Living"},{"Minimalist Wallet","Lifestyle","Accessories","UrbanCarry","129.00","46","Casa Home & Living"},{"Reading Glasses","Lifestyle","Accessories","ClearView","89.00","52","Casa Home & Living"}
        };
        Map<String, VendorCompany> companies = Map.of("Atlas Market", atlas, "TechHub Maroc", techHub, "Casa Home & Living", casaHome);
        for (int index = 0; index < data.length; index++) {
            if (repository.findBySku("DEMO-SKU-" + (index + 1)).isPresent()) continue;
            String[] item = data[index]; Product product = new Product(); product.setName(item[0]); product.setDescription("Demo catalog item: " + item[0]);
            product.setPrice(new BigDecimal(item[4])); product.setCostPrice(new BigDecimal(item[4]).multiply(new BigDecimal("0.70"))); product.setCategory(item[1]); product.setSubcategory(item[2]); product.setBrand(item[3]);
            product.setSku("DEMO-SKU-" + (index + 1)); product.setBarcode("629000000" + String.format("%03d", index + 1)); product.setWeight(0.5 + (index % 6) * 0.25); product.setDimensions("20x15x10 cm");
            product.setStockQuantity(Integer.valueOf(item[5])); product.setMinStockLevel(10); product.setMaxStockLevel(250); product.setStatus(Product.ProductStatus.ACTIVE); product.setIsAvailable(true); product.setIsFeatured(index % 5 == 0);
            product.setRating(new BigDecimal("4.20").add(new BigDecimal(index % 8).movePointLeft(1))); product.setReviewCount(12 + index * 3); product.setImageUrls(List.of("https://images.unsplash.com/photo-demo-" + (index + 1)));
            product.setTags(Arrays.asList(item[1].toLowerCase(), item[2].toLowerCase(), "demo")); product.setSpecifications(Map.of("brand", item[3], "origin", "Morocco", "catalog", "demo")); product.setWarranty("12 months");
            product.setManufacturerDate(LocalDate.now().minusMonths(6)); product.setVendorCompany(companies.get(item[6])); product.setTotalSold(index * 4); product.setTotalRevenue(new BigDecimal(item[4]).multiply(BigDecimal.valueOf(index * 4L)));
            if (index % 7 == 0) { product.setDiscount(new BigDecimal("10.00")); product.setDiscountType(Product.DiscountType.PERCENTAGE); product.setDiscountStartDate(LocalDate.now().minusDays(10)); product.setDiscountEndDate(LocalDate.now().plusDays(20)); }
            repository.save(product);
        }
    }

    private Partnership partnership(VendorCompany vendor, DeliveryCompany delivery, Partnership.PartnershipStatus status, String area, PartnershipRepository repository) {
        return repository.findByVendorCompanyAndDeliveryCompany(vendor, delivery).orElseGet(() -> {
            Partnership value = new Partnership(); value.setVendorCompany(vendor); value.setDeliveryCompany(delivery); value.setStatus(status); value.setCommissionRate(new BigDecimal("8.50"));
            value.setServiceAreas(List.of(area, "Downtown", "Residential")); value.setMinimumOrderValue(new BigDecimal("50.00")); value.setMaximumDeliveryDistanceKm(35.0); value.setEstimatedDeliveryTimeHours(2);
            value.setPartnershipTerms("Demo partnership with standard delivery terms."); value.setIsExclusive(false); value.setContractStartDate(LocalDateTime.now().minusMonths(6)); value.setContractEndDate(LocalDateTime.now().plusMonths(6)); value.setAverageRating(new BigDecimal("4.50")); value.setNotes("Seeded demo partnership");
            if (status == Partnership.PartnershipStatus.ACTIVE) value.setActivatedAt(LocalDateTime.now().minusMonths(6)); return repository.save(value);
        });
    }

    private Order order(String number, VendorCompany vendor, CustomerUser customer, DeliveryCompany delivery, DriverPerson driver, Partnership partnership, Order.OrderStatus status, Order.OrderPriority priority, String description, String pickup, String address, BigDecimal amount, BigDecimal fee, OrderRepository repository) {
        return repository.findByOrderNumber(number).orElseGet(() -> {
            Order value = new Order(); value.setOrderNumber(number); value.setVendorCompany(vendor); value.setCustomerUser(customer); value.setDeliveryCompany(delivery); value.setDriverPerson(driver); value.setPartnership(partnership); value.setPickupAddress(pickup); value.setDeliveryAddress(address);
            value.setPickupLatitude(33.5731); value.setPickupLongitude(-7.5898); value.setDeliveryLatitude(33.5899); value.setDeliveryLongitude(-7.6039); value.setOrderAmount(amount); value.setDeliveryFee(fee); value.setTotalAmount(amount.add(fee)); value.setStatus(status); value.setPriority(priority); value.setDescription(description); value.setSpecialInstructions("Please call customer before arrival.");
            value.setDistanceKm(7.5); value.setWeightKg(2.4); value.setPackageDimensions("30x25x20 cm"); value.setIsFragile(priority == Order.OrderPriority.HIGH); value.setRequiresSignature(status == Order.OrderStatus.IN_TRANSIT || status == Order.OrderStatus.ASSIGNED); value.setTrackingNumber("TRK-" + number.substring(number.length() - 4)); value.setScheduledPickupTime(LocalDateTime.now().plusHours(2)); value.setScheduledDeliveryTime(LocalDateTime.now().plusHours(5));
            if (status == Order.OrderStatus.DELIVERED) { value.setActualPickupTime(LocalDateTime.now().minusDays(2)); value.setActualDeliveryTime(LocalDateTime.now().minusDays(2).plusHours(2)); value.setRating(5); value.setReview("Fast and professional delivery."); }
            if (status == Order.OrderStatus.CANCELLED) value.setCancellationReason("Customer changed the delivery address."); if (driver != null) value.setAssignedAt(LocalDateTime.now().minusHours(3)); return repository.save(value);
        });
    }

    private void bid(BidRepository repository, String bidId, Order order, DeliveryCompany company, DriverPerson driver, BigDecimal amount, Bid.BidStatus status) {
        if (repository.findByBidId(bidId).isPresent()) return; Bid value = new Bid(); value.setBidId(bidId); value.setOrderId(order.getId()); value.setBidderType(Bid.BidderType.COMPANY); value.setDeliveryCompanyId(company.getId()); value.setDriverId(driver.getId()); value.setBidAmount(amount); value.setEstimatedDeliveryTime(LocalDateTime.now().plusHours(3)); value.setMessage("Available for reliable same-day delivery."); value.setStatus(status); value.setSubmittedAt(LocalDateTime.now().minusHours(4));
        if (status == Bid.BidStatus.ACCEPTED || status == Bid.BidStatus.REJECTED) { value.setRespondedAt(LocalDateTime.now().minusHours(2)); value.setResponseMessage(status == Bid.BidStatus.ACCEPTED ? "Bid accepted." : "Another bid was selected."); } repository.save(value);
    }

    private void pool(PooledOrderRepository repository, DeliveryCompany company, Order order) {
        if (!repository.existsByDeliveryCompanyIdAndOrderId(company.getId(), order.getId())) repository.save(new PooledOrder(company.getId(), order.getId()));
    }
}