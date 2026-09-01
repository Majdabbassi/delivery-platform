package com.upstart.backend.service;

import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.DeliveryOwner;
import com.upstart.backend.entity.Order;
import com.upstart.backend.entity.VendorCompany;
import com.upstart.backend.entity.VendorOwner;
import com.upstart.backend.exception.ResourceNotFoundException;
import com.upstart.backend.repository.DeliveryCompanyRepository;
import com.upstart.backend.repository.VendorCompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityServiceTest {

    private VendorCompanyRepository vendorCompanyRepository;
    private DeliveryCompanyRepository deliveryCompanyRepository;
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        vendorCompanyRepository = mock(VendorCompanyRepository.class);
        deliveryCompanyRepository = mock(DeliveryCompanyRepository.class);
        securityService = new SecurityService(vendorCompanyRepository, deliveryCompanyRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private VendorOwner vendorOwner(long id, String username) {
        VendorOwner owner = new VendorOwner();
        owner.setId(id);
        owner.setUsername(username);
        owner.setEmail(username + "@example.com");
        owner.setPassword("hash");
        owner.setFirstName(username);
        owner.setLastName("Owner");
        owner.setRole(com.upstart.backend.entity.User.Role.VENDOR_OWNER);
        return owner;
    }

    private DeliveryOwner deliveryOwner(long id, String username) {
        DeliveryOwner owner = new DeliveryOwner();
        owner.setId(id);
        owner.setUsername(username);
        owner.setEmail(username + "@example.com");
        owner.setPassword("hash");
        owner.setFirstName(username);
        owner.setLastName("Owner");
        owner.setRole(com.upstart.backend.entity.User.Role.DELIVERY_OWNER);
        return owner;
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + ((com.upstart.backend.entity.User) principal).getRole().name()))));
    }

    @Test
    void getOwnedVendorCompanyOrThrow_allowsOwner() {
        VendorOwner owner = vendorOwner(10L, "owner1");
        VendorCompany company = new VendorCompany();
        company.setId(5L);
        company.setOwner(owner);
        when(vendorCompanyRepository.findById(5L)).thenReturn(Optional.of(company));

        authenticate(owner);
        VendorCompany result = securityService.getOwnedVendorCompanyOrThrow(5L);
        assertEquals(5L, result.getId());
    }

    @Test
    void getOwnedVendorCompanyOrThrow_deniesOtherOwner() {
        VendorOwner ownerA = vendorOwner(10L, "ownerA");
        VendorOwner ownerB = vendorOwner(20L, "ownerB");
        VendorCompany company = new VendorCompany();
        company.setId(5L);
        company.setOwner(ownerB);
        when(vendorCompanyRepository.findById(5L)).thenReturn(Optional.of(company));

        authenticate(ownerA);
        assertThrows(AccessDeniedException.class, () -> securityService.getOwnedVendorCompanyOrThrow(5L));
    }

    @Test
    void getOwnedDeliveryCompanyOrThrow_deniesOtherOwner() {
        DeliveryOwner ownerA = deliveryOwner(11L, "dA");
        DeliveryOwner ownerB = deliveryOwner(21L, "dB");
        DeliveryCompany company = new DeliveryCompany();
        company.setId(7L);
        company.setOwner(ownerB);
        when(deliveryCompanyRepository.findById(7L)).thenReturn(Optional.of(company));

        authenticate(ownerA);
        assertThrows(AccessDeniedException.class, () -> securityService.getOwnedDeliveryCompanyOrThrow(7L));
    }

    @Test
    void getOwnedVendorCompanyOrThrow_notFound() {
        when(vendorCompanyRepository.findById(anyLong())).thenReturn(Optional.empty());
        authenticate(vendorOwner(99L, "o"));
        assertThrows(ResourceNotFoundException.class, () -> securityService.getOwnedVendorCompanyOrThrow(999L));
    }

    @Test
    void filterOrdersForUser_vendorOwner_onlySeesOwnCompaniesOrders() {
        VendorOwner owner1 = vendorOwner(10L, "owner1");
        VendorOwner owner2 = vendorOwner(20L, "owner2");

        VendorCompany vc1 = new VendorCompany();
        vc1.setId(1L);
        vc1.setOwner(owner1);
        VendorCompany vc2 = new VendorCompany();
        vc2.setId(2L);
        vc2.setOwner(owner2);

        Order own = new Order();
        own.setId(100L);
        own.setVendorCompany(vc1);
        Order other = new Order();
        other.setId(200L);
        other.setVendorCompany(vc2);

        authenticate(owner1);
        List<Order> filtered = securityService.filterOrdersForUser(List.of(own, other));
        assertEquals(1, filtered.size());
        assertEquals(100L, filtered.get(0).getId());
    }

    @Test
    void filterOrdersForUser_deliveryOwner_onlySeesOwnCompaniesOrders() {
        DeliveryOwner owner1 = deliveryOwner(30L, "d1");
        DeliveryOwner owner2 = deliveryOwner(40L, "d2");

        DeliveryCompany dc1 = new DeliveryCompany();
        dc1.setId(1L);
        dc1.setOwner(owner1);
        DeliveryCompany dc2 = new DeliveryCompany();
        dc2.setId(2L);
        dc2.setOwner(owner2);

        Order own = new Order();
        own.setId(300L);
        own.setDeliveryCompany(dc1);
        Order other = new Order();
        other.setId(400L);
        other.setDeliveryCompany(dc2);

        authenticate(owner1);
        List<Order> filtered = securityService.filterOrdersForUser(List.of(own, other));
        assertEquals(1, filtered.size());
        assertEquals(300L, filtered.get(0).getId());
    }

    @Test
    void filterOrdersForUser_noRoleSpecific_returnsAll() {
        DeliveryOwner owner1 = deliveryOwner(30L, "d1");
        DeliveryCompany dc1 = new DeliveryCompany();
        dc1.setId(1L);
        dc1.setOwner(owner1);
        Order own = new Order();
        own.setId(300L);
        own.setDeliveryCompany(dc1);

        // SUPER_ADMIN should see everything regardless of ownership.
        com.upstart.backend.entity.SuperAdmin admin = new com.upstart.backend.entity.SuperAdmin();
        admin.setId(1L);
        admin.setRole(com.upstart.backend.entity.User.Role.SUPER_ADMIN);

        authenticate(admin);
        List<Order> filtered = securityService.filterOrdersForUser(List.of(own));
        assertEquals(1, filtered.size());
    }
}
