package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.CustomerUser;
import com.swiftdeliver.backend.repository.CustomerUserRepository;
import com.swiftdeliver.backend.specification.CustomerUserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerUserService {

    private final CustomerUserRepository customerUserRepository;
    private final PasswordEncoder passwordEncoder;

    // CREATE
    public CustomerUser createCustomerUser(CustomerUser customerUser) {
        log.info("Creating new customer user with username: {}", customerUser.getUsername());
        
        // Check if username or email already exists
        if (customerUserRepository.findByUsername(customerUser.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + customerUser.getUsername());
        }
        
        if (customerUserRepository.findByEmail(customerUser.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + customerUser.getEmail());
        }
        
        if (customerUser.getPassword() != null && !customerUser.getPassword().startsWith("$2")) {
            customerUser.setPassword(passwordEncoder.encode(customerUser.getPassword()));
        }
        
        CustomerUser savedCustomerUser = customerUserRepository.save(customerUser);
        log.info("Customer user created successfully with ID: {}", savedCustomerUser.getId());
        return savedCustomerUser;
    }

    // READ - Get by ID
    @Transactional(readOnly = true)
    public Optional<CustomerUser> getCustomerUserById(Long id) {
        log.debug("Fetching customer user by ID: {}", id);
        return customerUserRepository.findById(id);
    }

    // READ - Get by username
    @Transactional(readOnly = true)
    public Optional<CustomerUser> getCustomerUserByUsername(String username) {
        log.debug("Fetching customer user by username: {}", username);
        return customerUserRepository.findByUsername(username);
    }

    // READ - Get by email
    @Transactional(readOnly = true)
    public Optional<CustomerUser> getCustomerUserByEmail(String email) {
        log.debug("Fetching customer user by email: {}", email);
        return customerUserRepository.findByEmail(email);
    }

    // READ - Get all customer users
    @Transactional(readOnly = true)
    public List<CustomerUser> getAllCustomerUsers() {
        log.debug("Fetching all customer users");
        return customerUserRepository.findAll();
    }

    // READ - Get all customer users with pagination
    @Transactional(readOnly = true)
    public Page<CustomerUser> getAllCustomerUsers(Pageable pageable) {
        log.debug("Fetching all customer users with pagination");
        return customerUserRepository.findAll(pageable);
    }

    // READ - Get all active customer users
    @Transactional(readOnly = true)
    public List<CustomerUser> getAllActiveCustomerUsers() {
        log.debug("Fetching all active customer users");
        return customerUserRepository.findAllActiveCustomerUsers();
    }

    // READ - Get customer users with pagination
    @Transactional(readOnly = true)
    public Page<CustomerUser> getActiveCustomerUsers(Pageable pageable) {
        log.debug("Fetching active customer users with pagination");
        return customerUserRepository.findAllActiveCustomerUsers(pageable);
    }

    // READ - Get premium customer users
    @Transactional(readOnly = true)
    public List<CustomerUser> getPremiumCustomerUsers() {
        log.debug("Fetching premium customer users");
        return customerUserRepository.findByIsPremium(true);
    }

    // READ - Search customer users by name
    @Transactional(readOnly = true)
    public List<CustomerUser> searchCustomerUsersByName(String name) {
        log.debug("Searching customer users by name: {}", name);
        return customerUserRepository.findCustomerUsersByName(name);
    }

    // READ - Get customer users by minimum loyalty points
    @Transactional(readOnly = true)
    public List<CustomerUser> getCustomerUsersByMinimumLoyaltyPoints(Integer minPoints) {
        log.debug("Fetching customer users with minimum loyalty points: {}", minPoints);
        return customerUserRepository.findCustomerUsersByMinLoyaltyPoints(minPoints);
    }

    // READ - Get top spenders
    @Transactional(readOnly = true)
    public List<CustomerUser> getTopSpenders(Pageable pageable) {
        log.debug("Fetching top spending customer users");
        return customerUserRepository.findTopSpenders(pageable);
    }

    // READ - Search customer users with dynamic criteria
    @Transactional(readOnly = true)
    public Page<CustomerUser> searchCustomerUsers(
            String username,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            Boolean isPremium,
            Boolean isEnabled,
            Integer minLoyaltyPoints,
            Integer maxLoyaltyPoints,
            BigDecimal minTotalSpent,
            BigDecimal maxTotalSpent,
            Integer minTotalOrders,
            Integer maxTotalOrders,
            String gender,
            String preferredPaymentMethod,
            String defaultAddress,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            LocalDate bornAfter,
            LocalDate bornBefore,
            Pageable pageable) {
        
        log.debug("Searching customer users with dynamic criteria");
        
        Specification<CustomerUser> spec = null;
        
        if (username != null && !username.trim().isEmpty()) {
            spec = (spec == null) ? CustomerUserSpecifications.hasUsername(username) : spec.and(CustomerUserSpecifications.hasUsername(username));
        }
        if (email != null && !email.trim().isEmpty()) {
            spec = (spec == null) ? CustomerUserSpecifications.hasEmail(email) : spec.and(CustomerUserSpecifications.hasEmail(email));
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            spec = (spec == null) ? CustomerUserSpecifications.hasFirstName(firstName) : spec.and(CustomerUserSpecifications.hasFirstName(firstName));
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            spec = (spec == null) ? CustomerUserSpecifications.hasLastName(lastName) : spec.and(CustomerUserSpecifications.hasLastName(lastName));
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            spec = (spec == null) ? CustomerUserSpecifications.hasPhoneNumber(phoneNumber) : spec.and(CustomerUserSpecifications.hasPhoneNumber(phoneNumber));
        }
        if (isPremium != null) {
            spec = (spec == null) ? CustomerUserSpecifications.isPremium(isPremium) : spec.and(CustomerUserSpecifications.isPremium(isPremium));
        }
        if (isEnabled != null) {
            spec = (spec == null) ? CustomerUserSpecifications.isEnabled(isEnabled) : spec.and(CustomerUserSpecifications.isEnabled(isEnabled));
        }
        if (minLoyaltyPoints != null) {
            spec = (spec == null) ? CustomerUserSpecifications.hasMinLoyaltyPoints(minLoyaltyPoints) : spec.and(CustomerUserSpecifications.hasMinLoyaltyPoints(minLoyaltyPoints));
        }
        if (maxLoyaltyPoints != null) {
            spec = (spec == null) ? CustomerUserSpecifications.hasMaxLoyaltyPoints(maxLoyaltyPoints) : spec.and(CustomerUserSpecifications.hasMaxLoyaltyPoints(maxLoyaltyPoints));
        }
        if (minTotalSpent != null) {
            spec = (spec == null) ? CustomerUserSpecifications.hasMinTotalSpent(minTotalSpent) : spec.and(CustomerUserSpecifications.hasMinTotalSpent(minTotalSpent));
        }
        if (maxTotalSpent != null) {
            spec = (spec == null) ? CustomerUserSpecifications.hasMaxTotalSpent(maxTotalSpent) : spec.and(CustomerUserSpecifications.hasMaxTotalSpent(maxTotalSpent));
        }
        if (minTotalOrders != null) {
            spec = (spec == null) ? CustomerUserSpecifications.hasMinTotalOrders(minTotalOrders) : spec.and(CustomerUserSpecifications.hasMinTotalOrders(minTotalOrders));
        }
        if (maxTotalOrders != null) {
            spec = (spec == null) ? CustomerUserSpecifications.hasMaxTotalOrders(maxTotalOrders) : spec.and(CustomerUserSpecifications.hasMaxTotalOrders(maxTotalOrders));
        }
        if (gender != null && !gender.trim().isEmpty()) {
            spec = (spec == null) ? CustomerUserSpecifications.hasGender(gender) : spec.and(CustomerUserSpecifications.hasGender(gender));
        }
        if (preferredPaymentMethod != null && !preferredPaymentMethod.trim().isEmpty()) {
            spec = (spec == null) ? CustomerUserSpecifications.hasPreferredPaymentMethod(preferredPaymentMethod) : spec.and(CustomerUserSpecifications.hasPreferredPaymentMethod(preferredPaymentMethod));
        }
        if (createdAfter != null) {
            spec = (spec == null) ? CustomerUserSpecifications.createdAfter(createdAfter) : spec.and(CustomerUserSpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? CustomerUserSpecifications.createdBefore(createdBefore) : spec.and(CustomerUserSpecifications.createdBefore(createdBefore));
        }
        if (bornAfter != null) {
            spec = (spec == null) ? CustomerUserSpecifications.birthDateAfter(bornAfter) : spec.and(CustomerUserSpecifications.birthDateAfter(bornAfter));
        }
        if (bornBefore != null) {
            spec = (spec == null) ? CustomerUserSpecifications.birthDateBefore(bornBefore) : spec.and(CustomerUserSpecifications.birthDateBefore(bornBefore));
        }
        if (defaultAddress != null && !defaultAddress.trim().isEmpty()) {
            spec = (spec == null) ? CustomerUserSpecifications.hasDefaultAddress(defaultAddress) : spec.and(CustomerUserSpecifications.hasDefaultAddress(defaultAddress));
        }
        
        return customerUserRepository.findAll(spec, pageable);
    }

    // UPDATE
    public CustomerUser updateCustomerUser(Long id, CustomerUser customerUserDetails) {
        log.info("Updating customer user with ID: {}", id);
        
        CustomerUser existingCustomerUser = customerUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer user not found with ID: " + id));

        // Update fields
        if (customerUserDetails.getFirstName() != null) {
            existingCustomerUser.setFirstName(customerUserDetails.getFirstName());
        }
        if (customerUserDetails.getLastName() != null) {
            existingCustomerUser.setLastName(customerUserDetails.getLastName());
        }
        if (customerUserDetails.getEmail() != null && !customerUserDetails.getEmail().equals(existingCustomerUser.getEmail())) {
            // Check if new email already exists
            if (customerUserRepository.findByEmail(customerUserDetails.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists: " + customerUserDetails.getEmail());
            }
            existingCustomerUser.setEmail(customerUserDetails.getEmail());
        }
        if (customerUserDetails.getPhoneNumber() != null) {
            existingCustomerUser.setPhoneNumber(customerUserDetails.getPhoneNumber());
        }
        if (customerUserDetails.getPassword() != null && !customerUserDetails.getPassword().startsWith("$2")) {
            existingCustomerUser.setPassword(passwordEncoder.encode(customerUserDetails.getPassword()));
        }
        if (customerUserDetails.getDefaultAddress() != null) {
            existingCustomerUser.setDefaultAddress(customerUserDetails.getDefaultAddress());
        }
        if (customerUserDetails.getPreferredPaymentMethod() != null) {
            existingCustomerUser.setPreferredPaymentMethod(customerUserDetails.getPreferredPaymentMethod());
        }
        if (customerUserDetails.getDateOfBirth() != null) {
            existingCustomerUser.setDateOfBirth(customerUserDetails.getDateOfBirth());
        }
        if (customerUserDetails.getGender() != null) {
            existingCustomerUser.setGender(customerUserDetails.getGender());
        }
        if (customerUserDetails.getNotificationPreferences() != null) {
            existingCustomerUser.setNotificationPreferences(customerUserDetails.getNotificationPreferences());
        }
        if (customerUserDetails.getIsPremium() != null) {
            existingCustomerUser.setIsPremium(customerUserDetails.getIsPremium());
        }

        CustomerUser updatedCustomerUser = customerUserRepository.save(existingCustomerUser);
        log.info("Customer user updated successfully with ID: {}", updatedCustomerUser.getId());
        return updatedCustomerUser;
    }

    // UPDATE - Add loyalty points
    public CustomerUser addLoyaltyPoints(Long customerUserId, Integer points) {
        log.info("Adding {} loyalty points to customer user ID: {}", points, customerUserId);
        
        CustomerUser customerUser = customerUserRepository.findById(customerUserId)
                .orElseThrow(() -> new RuntimeException("Customer user not found with ID: " + customerUserId));
        
        customerUser.setLoyaltyPoints(customerUser.getLoyaltyPoints() + points);
        return customerUserRepository.save(customerUser);
    }

    // UPDATE - Update spending and orders
    public CustomerUser updateSpendingAndOrders(Long customerUserId, BigDecimal amount) {
        log.info("Updating spending for customer user ID: {} with amount: {}", customerUserId, amount);
        
        CustomerUser customerUser = customerUserRepository.findById(customerUserId)
                .orElseThrow(() -> new RuntimeException("Customer user not found with ID: " + customerUserId));
        
        customerUser.setTotalSpent(customerUser.getTotalSpent().add(amount));
        customerUser.setTotalOrders(customerUser.getTotalOrders() + 1);
        
        // Add loyalty points based on spending (1 point per dollar)
        int loyaltyPointsToAdd = amount.intValue();
        customerUser.setLoyaltyPoints(customerUser.getLoyaltyPoints() + loyaltyPointsToAdd);
        
        return customerUserRepository.save(customerUser);
    }

    // UPDATE - Toggle premium status
    public CustomerUser togglePremiumStatus(Long customerUserId) {
        log.info("Toggling premium status for customer user ID: {}", customerUserId);
        
        CustomerUser customerUser = customerUserRepository.findById(customerUserId)
                .orElseThrow(() -> new RuntimeException("Customer user not found with ID: " + customerUserId));
        
        customerUser.setIsPremium(!customerUser.getIsPremium());
        return customerUserRepository.save(customerUser);
    }

    // DELETE - Soft delete (disable)
    public void disableCustomerUser(Long id) {
        log.info("Disabling customer user with ID: {}", id);
        
        CustomerUser customerUser = customerUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer user not found with ID: " + id));
        
        customerUser.setEnabled(false);
        customerUserRepository.save(customerUser);
        log.info("Customer user disabled successfully with ID: {}", id);
    }

    // DELETE - Hard delete
    public void deleteCustomerUser(Long id) {
        log.info("Deleting customer user with ID: {}", id);
        
        if (!customerUserRepository.existsById(id)) {
            throw new RuntimeException("Customer user not found with ID: " + id);
        }
        
        customerUserRepository.deleteById(id);
        log.info("Customer user deleted successfully with ID: {}", id);
    }

    // STATISTICS
    @Transactional(readOnly = true)
    public Long countActiveCustomerUsers() {
        return customerUserRepository.countActiveCustomerUsers();
    }

    @Transactional(readOnly = true)
    public Long countPremiumCustomerUsers() {
        return customerUserRepository.countPremiumCustomerUsers();
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageSpending() {
        return customerUserRepository.getAverageSpending();
    }

    @Transactional(readOnly = true)
    public Long getTotalLoyaltyPoints() {
        return customerUserRepository.getTotalLoyaltyPoints();
    }

    // UPDATE - Update loyalty points
    public CustomerUser updateLoyaltyPoints(Long customerUserId, Integer loyaltyPoints) {
        log.info("Updating loyalty points for customer user ID: {} to: {}", customerUserId, loyaltyPoints);
        
        CustomerUser customerUser = customerUserRepository.findById(customerUserId)
                .orElseThrow(() -> new RuntimeException("Customer user not found with ID: " + customerUserId));
        
        customerUser.setLoyaltyPoints(loyaltyPoints);
        return customerUserRepository.save(customerUser);
    }

    // UPDATE - Update premium status
    public CustomerUser updatePremiumStatus(Long customerUserId, Boolean isPremium) {
        log.info("Updating premium status for customer user ID: {} to: {}", customerUserId, isPremium);
        
        CustomerUser customerUser = customerUserRepository.findById(customerUserId)
                .orElseThrow(() -> new RuntimeException("Customer user not found with ID: " + customerUserId));
        
        customerUser.setIsPremium(isPremium);
        return customerUserRepository.save(customerUser);
    }

    // DELETE - Hard delete
    public void hardDeleteCustomerUser(Long id) {
        log.info("Hard deleting customer user with ID: {}", id);
        
        if (!customerUserRepository.existsById(id)) {
            throw new RuntimeException("Customer user not found with ID: " + id);
        }
        
        customerUserRepository.deleteById(id);
        log.info("Customer user hard deleted successfully with ID: {}", id);
    }

    // STATISTICS - Get customer user statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long active = countActiveCustomerUsers();
        stats.put("total", customerUserRepository.count());
        stats.put("active", active);
        stats.put("verified", customerUserRepository.countByIsEnabled(true));
        stats.put("activeAndVerified", active);
        stats.put("premium", countPremiumCustomerUsers());
        stats.put("withMultipleOrders", customerUserRepository.countByTotalOrdersGreaterThan(1L));
        return stats;
    }
}