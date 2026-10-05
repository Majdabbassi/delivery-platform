package com.swiftdeliver.backend.specification;

import com.swiftdeliver.backend.entity.Partnership;
import com.swiftdeliver.backend.entity.Partnership.PartnershipStatus;
import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.DeliveryOwner;
import com.swiftdeliver.backend.entity.VendorCompany;
import com.swiftdeliver.backend.entity.VendorOwner;
import com.swiftdeliver.backend.repository.PartnershipRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that exercise PartnershipSpecifications against a real H2
 * database. These validate that the column names used in the specifications
 * (fixed in Item 6 — minimumOrderValue, totalOrdersCompleted,
 * totalRevenueGenerated, maximumDeliveryDistanceKm, serviceAreas) are mapped
 * correctly to the database schema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PartnershipSpecificationsTest {

    @Autowired private PartnershipRepository partnershipRepository;
    @Autowired private EntityManager em;

    private VendorOwner vendorOwner;
    private DeliveryOwner deliveryOwner;
    private VendorCompany vendorA;
    private DeliveryCompany deliveryA;

    @BeforeEach
    void setUp() {
        vendorOwner = persist(new VendorOwner(
                "vendor-owner-1", "vo@test.com", "password123", "Vendor", "Owner",
                "1111111111", "NAT-1"));

        deliveryOwner = persist(new DeliveryOwner(
                "delivery-owner-1", "do@test.com", "password123", "Delivery", "Owner",
                "2222222222", "NAT-2"));

        vendorA = new VendorCompany();
        vendorA.setCompanyName("Vendor A");
        vendorA.setOwner(vendorOwner);
        persist(vendorA);

        deliveryA = new DeliveryCompany();
        deliveryA.setCompanyName("Delivery A");
        deliveryA.setOwner(deliveryOwner);
        persist(deliveryA);
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    private Partnership buildPartnership(VendorCompany vendor,
                                         DeliveryCompany delivery,
                                         PartnershipStatus status,
                                         BigDecimal minOrderValue,
                                         Double maxDistance,
                                         List<String> serviceAreas,
                                         Long ordersCompleted,
                                         BigDecimal revenue) {
        Partnership p = new Partnership();
        p.setVendorCompany(vendor);
        p.setDeliveryCompany(delivery);
        p.setStatus(status);
        p.setMinimumOrderValue(minOrderValue);
        p.setMaximumDeliveryDistanceKm(maxDistance);
        p.setServiceAreas(serviceAreas);
        p.setTotalOrdersCompleted(ordersCompleted);
        p.setTotalRevenueGenerated(revenue);
        p.setCommissionRate(new BigDecimal("5.00"));
        p.setContractStartDate(LocalDateTime.now().minusDays(30));
        return p;
    }

    private Partnership save(VendorCompany vendor, DeliveryCompany delivery,
                             PartnershipStatus status, BigDecimal minOrder,
                             Double maxDist, List<String> areas, Long orders,
                             BigDecimal revenue) {
        Partnership p = buildPartnership(vendor, delivery, status,
                minOrder, maxDist, areas, orders, revenue);
        return partnershipRepository.save(p);
    }

    @Test
    @DisplayName("hasStatus filters by partnership status")
    void hasStatus() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of("Beirut"), 10L, BigDecimal.TEN);
        save(vendorA, deliveryA, PartnershipStatus.SUSPENDED,
                null, null, List.of("Beirut"), 5L, BigDecimal.ONE);

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.hasStatus(PartnershipStatus.ACTIVE));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(PartnershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("hasServiceArea filters using the ElementCollection column")
    void hasServiceArea() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of("Beirut", "Tripoli"), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of("Sidon"), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, null, 0L, BigDecimal.ZERO);

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.hasServiceArea("beirut"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getServiceAreas()).contains("Beirut");
    }

    @Test
    @DisplayName("hasAnyServiceArea matches any of the given areas")
    void hasAnyServiceArea() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of("Beirut"), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of("Tripoli"), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of("Sidon"), 0L, BigDecimal.ZERO);

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.hasAnyServiceArea(List.of("Beirut", "Sidon")));

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("canHandleOrderValue filters by minimumOrderValue column")
    void canHandleOrderValue() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("50.00"), null, List.of(), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("200.00"), null, List.of(), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of(), 0L, BigDecimal.ZERO); // no min = accepts all

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.canHandleOrderValue(new BigDecimal("100.00")));

        assertThat(results).hasSize(2); // minOrder=50 and null both qualify
    }

    @Test
    @DisplayName("canHandleDeliveryDistance filters by maximumDeliveryDistanceKm column")
    void canHandleDeliveryDistance() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, 10.0, List.of(), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, 100.0, List.of(), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of(), 0L, BigDecimal.ZERO); // no max = accepts all

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.canHandleDeliveryDistance(new BigDecimal("50.0")));

        assertThat(results).hasSize(2); // maxDist=100.0 and null both qualify
    }

    @Test
    @DisplayName("hasTotalOrdersGreaterThan filters by totalOrdersCompleted column")
    void hasTotalOrdersGreaterThan() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of(), 5L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of(), 100L, BigDecimal.ZERO);

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.hasTotalOrdersGreaterThan(10L));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalOrdersCompleted()).isEqualTo(100L);
    }

    @Test
    @DisplayName("hasTotalRevenueGreaterThan filters by totalRevenueGenerated column")
    void hasTotalRevenueGreaterThan() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of(), 0L, new BigDecimal("500"));
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of(), 0L, new BigDecimal("50000"));

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.hasTotalRevenueGreaterThan(new BigDecimal("10000")));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTotalRevenueGenerated())
                .isEqualByComparingTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("hasMinOrderValueBetween filters by minimumOrderValue column range")
    void hasMinOrderValueBetween() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("25.00"), null, List.of(), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("75.00"), null, List.of(), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("150.00"), null, List.of(), 0L, BigDecimal.ZERO);

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.hasMinOrderValueBetween(
                        new BigDecimal("50.00"), new BigDecimal("100.00")));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMinimumOrderValue())
                .isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    @DisplayName("isExclusive filters by the isExclusive flag")
    void isExclusive() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of(), 0L, BigDecimal.ZERO);
        Partnership ex = buildPartnership(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of(), 0L, BigDecimal.ZERO);
        ex.setIsExclusive(true);
        partnershipRepository.save(ex);
        em.flush();

        List<Partnership> results = partnershipRepository.findAll(
                PartnershipSpecifications.isExclusive(true));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(ex.getId());
    }

    @Test
    @DisplayName("composite: findBestForOrder filters by vendor, eligibility, area, and exclusivity")
    void findBestForOrder_compositeFilter() {
        // matching partnership: exclusive, active, handles the order
        Partnership matching = buildPartnership(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("50.00"), 30.0, List.of("Beirut"), 50L, new BigDecimal("50000"));
        matching.setIsExclusive(true);
        partnershipRepository.save(matching);
        em.flush();

        // non-exclusive partnership for the same vendor+delivery — excluded when preferExclusive=true
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("10.00"), 50.0, List.of("Beirut"), 20L, new BigDecimal("10000"));

        // minimum order value too high for the order
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("500.00"), null, List.of("Beirut"), 0L, BigDecimal.ZERO);

        // wrong service area
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                null, null, List.of("Tripoli"), 0L, BigDecimal.ZERO);

        // suspended — excluded by isCurrentlyActive
        save(vendorA, deliveryA, PartnershipStatus.SUSPENDED,
                null, null, List.of("Beirut"), 0L, BigDecimal.ZERO);

        Specification<Partnership> spec = PartnershipSpecifications.findBestForOrder(
                vendorA.getId(),
                new BigDecimal("100.00"),   // order value
                new BigDecimal("20.0"),     // delivery distance
                "Beirut",                   // service area
                true);                      // prefer exclusive

        List<Partnership> results = partnershipRepository.findAll(spec);

        // Only the exclusive, active, eligible partnership should match
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMinimumOrderValue())
                .isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(results.get(0).getTotalOrdersCompleted()).isEqualTo(50L);
    }

    @Test
    @DisplayName("composite: isEligibleForOrder filters by value, distance, area and active status")
    void isEligibleForOrder() {
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("25.00"), 20.0, List.of("Beirut"), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.ACTIVE,
                new BigDecimal("200.00"), null, List.of("Beirut"), 0L, BigDecimal.ZERO);
        save(vendorA, deliveryA, PartnershipStatus.SUSPENDED,
                null, null, List.of("Beirut"), 0L, BigDecimal.ZERO);

        Specification<Partnership> spec = PartnershipSpecifications.isEligibleForOrder(
                new BigDecimal("50.00"),
                new BigDecimal("15.0"),
                "Beirut");

        List<Partnership> results = partnershipRepository.findAll(spec);

        // minOrder=25 (OK), maxDist=20 (OK), area=Beirut, active -> match
        // minOrder=200 (fails order value check) -> excluded
        // SUSPENDED -> excluded
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMinimumOrderValue())
                .isEqualByComparingTo(new BigDecimal("25.00"));
    }
}
