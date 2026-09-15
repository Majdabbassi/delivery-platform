package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.CustomerUser;
import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.DeliveryOwner;
import com.swiftdeliver.backend.entity.DriverPerson;
import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.entity.User;
import com.swiftdeliver.backend.entity.VendorCompany;
import com.swiftdeliver.backend.entity.VendorOwner;
import com.swiftdeliver.backend.exception.ResourceNotFoundException;
import com.swiftdeliver.backend.repository.DeliveryCompanyRepository;
import com.swiftdeliver.backend.repository.VendorCompanyRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolves the currently authenticated principal and provides tenant-scoping
 * helpers so that owner-level resources (vendor companies, delivery companies)
 * can be securely bound to the user who owns them.
 */
@Service
public class SecurityService {

    private final VendorCompanyRepository vendorCompanyRepository;
    private final DeliveryCompanyRepository deliveryCompanyRepository;

    public SecurityService(VendorCompanyRepository vendorCompanyRepository,
                           DeliveryCompanyRepository deliveryCompanyRepository) {
        this.vendorCompanyRepository = vendorCompanyRepository;
        this.deliveryCompanyRepository = deliveryCompanyRepository;
    }

    /**
     * Returns the currently authenticated {@link User} (the actual entity, as set
     * by {@link UserService#loadUserByUsername}).
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("No authenticated user in context");
        }
        return user;
    }

    public User.Role getCurrentRole() {
        return getCurrentUser().getRole();
    }

    /**
     * Returns the owner ID to scope a query to. Super admins may query any
     * owner; everyone else is pinned to their own account so they cannot browse
     * another owner's resources.
     */
    public Long resolveOwnerIdForCurrentUser(Long requestedOwnerId, User.Role allowedRole) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.SUPER_ADMIN) {
            if (requestedOwnerId == null) {
                throw new IllegalArgumentException("ownerId is required for SUPER_ADMIN");
            }
            return requestedOwnerId;
        }
        if (currentUser.getRole() != allowedRole) {
            throw new AccessDeniedException("Current user is not a " + allowedRole);
        }
        return currentUser.getId();
    }

    public VendorOwner getCurrentVendorOwner() {
        User user = getCurrentUser();
        if (user instanceof VendorOwner vendorOwner) {
            return vendorOwner;
        }
        throw new AccessDeniedException("Current user is not a vendor owner");
    }

    public DeliveryOwner getCurrentDeliveryOwner() {
        User user = getCurrentUser();
        if (user instanceof DeliveryOwner deliveryOwner) {
            return deliveryOwner;
        }
        throw new AccessDeniedException("Current user is not a delivery owner");
    }

    public CustomerUser getCurrentCustomerUser() {
        User user = getCurrentUser();
        if (user instanceof CustomerUser customerUser) {
            return customerUser;
        }
        throw new AccessDeniedException("Current user is not a customer");
    }

    public DriverPerson getCurrentDriverPerson() {
        User user = getCurrentUser();
        if (user instanceof DriverPerson driverPerson) {
            return driverPerson;
        }
        throw new AccessDeniedException("Current user is not a driver");
    }

    /**
     * Loads a vendor company and asserts the current user (or a SUPER_ADMIN)
     * owns it. Returns the company on success.
     */
    public VendorCompany getOwnedVendorCompanyOrThrow(Long vendorCompanyId) {
        VendorCompany company = vendorCompanyRepository.findById(vendorCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "VendorCompany not found with id: " + vendorCompanyId));

        User user = getCurrentUser();
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            return company;
        }
        if (user instanceof VendorOwner owner
                && company.getOwner() != null
                && owner.getId().equals(company.getOwner().getId())) {
            return company;
        }
        throw new AccessDeniedException("You do not have access to this vendor company");
    }

    /**
     * Loads a delivery company and asserts the current user (or a SUPER_ADMIN)
     * owns it. Returns the company on success.
     */
    public DeliveryCompany getOwnedDeliveryCompanyOrThrow(Long deliveryCompanyId) {
        DeliveryCompany company = deliveryCompanyRepository.findById(deliveryCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DeliveryCompany not found with id: " + deliveryCompanyId));

        User user = getCurrentUser();
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            return company;
        }
        if (user instanceof DeliveryOwner owner
                && company.getOwner() != null
                && owner.getId().equals(company.getOwner().getId())) {
            return company;
        }
        throw new AccessDeniedException("You do not have access to this delivery company");
    }

    /**
     * True when the current user is a SUPER_ADMIN or owns the given vendor company.
     */
    public boolean canAccessVendorCompany(Long vendorCompanyId) {
        try {
            getOwnedVendorCompanyOrThrow(vendorCompanyId);
            return true;
        } catch (AccessDeniedException | ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * True when the current user is a SUPER_ADMIN or owns the given delivery company.
     */
    public boolean canAccessDeliveryCompany(Long deliveryCompanyId) {
        try {
            getOwnedDeliveryCompanyOrThrow(deliveryCompanyId);
            return true;
        } catch (AccessDeniedException | ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Reduces a list of orders to those the current user is allowed to see:
     * SUPER_ADMIN sees all; VENDOR_OWNER only their companies' orders;
     * DELIVERY_OWNER only their companies' assigned orders; others fall through.
     */
    public java.util.List<Order> filterOrdersForUser(java.util.List<Order> orders) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.VENDOR_OWNER) {
            Long ownerId = currentUser.getId();
            return orders.stream()
                    .filter(o -> o.getVendorCompany() != null
                            && o.getVendorCompany().getOwner() != null
                            && ownerId.equals(o.getVendorCompany().getOwner().getId()))
                    .toList();
        }
        if (currentUser.getRole() == User.Role.DELIVERY_OWNER) {
            Long ownerId = currentUser.getId();
            return orders.stream()
                    .filter(o -> o.getDeliveryCompany() != null
                            && o.getDeliveryCompany().getOwner() != null
                            && ownerId.equals(o.getDeliveryCompany().getOwner().getId()))
                    .toList();
        }
        return orders;
    }
}
