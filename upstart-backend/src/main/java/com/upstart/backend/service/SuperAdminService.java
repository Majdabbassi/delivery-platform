package com.upstart.backend.service;

import com.upstart.backend.entity.SuperAdmin;
import com.upstart.backend.repository.SuperAdminRepository;
import com.upstart.backend.specification.SuperAdminSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;

    // CREATE
    public SuperAdmin createSuperAdmin(SuperAdmin superAdmin) {
        log.info("Creating new super admin with username: {}", superAdmin.getUsername());
        
        // Check if username or email already exists
        if (superAdminRepository.findByUsername(superAdmin.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + superAdmin.getUsername());
        }
        
        if (superAdminRepository.findByEmail(superAdmin.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists: " + superAdmin.getEmail());
        }
        
        // Set default values if not provided
        if (superAdmin.getSecurityLevel() == null) {
            superAdmin.setSecurityLevel(10); // Highest security level
        }
        
        if (superAdmin.getPassword() != null && !superAdmin.getPassword().startsWith("$2")) {
            superAdmin.setPassword(passwordEncoder.encode(superAdmin.getPassword()));
        }
        
        SuperAdmin savedSuperAdmin = superAdminRepository.save(superAdmin);
        log.info("Super admin created successfully with ID: {}", savedSuperAdmin.getId());
        return savedSuperAdmin;
    }

    // READ - Get by ID
    @Transactional(readOnly = true)
    public Optional<SuperAdmin> getSuperAdminById(Long id) {
        log.debug("Fetching super admin by ID: {}", id);
        return superAdminRepository.findById(id);
    }

    // READ - Get by username
    @Transactional(readOnly = true)
    public Optional<SuperAdmin> getSuperAdminByUsername(String username) {
        log.debug("Fetching super admin by username: {}", username);
        return superAdminRepository.findByUsername(username);
    }

    // READ - Get by email
    @Transactional(readOnly = true)
    public Optional<SuperAdmin> getSuperAdminByEmail(String email) {
        log.debug("Fetching super admin by email: {}", email);
        return superAdminRepository.findByEmail(email);
    }

    // READ - Get by username or email
    @Transactional(readOnly = true)
    public Optional<SuperAdmin> getSuperAdminByUsernameOrEmail(String usernameOrEmail) {
        log.debug("Fetching super admin by username or email: {}", usernameOrEmail);
        return superAdminRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }

    // READ - Get all super admins
    @Transactional(readOnly = true)
    public List<SuperAdmin> getAllSuperAdmins() {
        log.debug("Fetching all super admins");
        return superAdminRepository.findAll();
    }

    // READ - Get all super admins with pagination
    @Transactional(readOnly = true)
    public Page<SuperAdmin> getAllSuperAdmins(Pageable pageable) {
        log.debug("Fetching all super admins with pagination");
        return superAdminRepository.findAll(pageable);
    }

    // READ - Get active admins since a specific time
    @Transactional(readOnly = true)
    public List<SuperAdmin> getActiveAdminsSince(LocalDateTime since) {
        log.debug("Fetching active admins since: {}", since);
        return superAdminRepository.findActiveAdminsSince(since);
    }

    // READ - Get admins by minimum security level
    @Transactional(readOnly = true)
    public List<SuperAdmin> getAdminsByMinimumSecurityLevel(Integer minLevel) {
        log.debug("Fetching admins with minimum security level: {}", minLevel);
        return superAdminRepository.findByMinimumSecurityLevel(minLevel);
    }

    // READ - Search super admins with dynamic criteria
    @Transactional(readOnly = true)
    public Page<SuperAdmin> searchSuperAdmins(
            String username,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            Boolean isEnabled,
            Integer minSecurityLevel,
            Integer maxSecurityLevel,
            String systemPermissions,
            LocalDateTime lastAccessAfter,
            LocalDateTime lastAccessBefore,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable) {
        
        log.debug("Searching super admins with dynamic criteria");
        
        Specification<SuperAdmin> spec = null;
        
        if (username != null && !username.trim().isEmpty()) {
            spec = (spec == null) ? SuperAdminSpecifications.hasUsername(username) : spec.and(SuperAdminSpecifications.hasUsername(username));
        }
        if (email != null && !email.trim().isEmpty()) {
            spec = (spec == null) ? SuperAdminSpecifications.hasEmail(email) : spec.and(SuperAdminSpecifications.hasEmail(email));
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            spec = (spec == null) ? SuperAdminSpecifications.hasFirstName(firstName) : spec.and(SuperAdminSpecifications.hasFirstName(firstName));
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            spec = (spec == null) ? SuperAdminSpecifications.hasLastName(lastName) : spec.and(SuperAdminSpecifications.hasLastName(lastName));
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            spec = (spec == null) ? SuperAdminSpecifications.hasPhoneNumber(phoneNumber) : spec.and(SuperAdminSpecifications.hasPhoneNumber(phoneNumber));
        }
        if (isEnabled != null) {
            spec = (spec == null) ? SuperAdminSpecifications.isEnabled(isEnabled) : spec.and(SuperAdminSpecifications.isEnabled(isEnabled));
        }
        if (minSecurityLevel != null) {
            spec = (spec == null) ? SuperAdminSpecifications.hasMinSecurityLevel(minSecurityLevel) : spec.and(SuperAdminSpecifications.hasMinSecurityLevel(minSecurityLevel));
        }
        if (maxSecurityLevel != null) {
            spec = (spec == null) ? SuperAdminSpecifications.hasMaxSecurityLevel(maxSecurityLevel) : spec.and(SuperAdminSpecifications.hasMaxSecurityLevel(maxSecurityLevel));
        }
        if (systemPermissions != null && !systemPermissions.trim().isEmpty()) {
            spec = (spec == null) ? SuperAdminSpecifications.hasSystemPermissions(systemPermissions) : spec.and(SuperAdminSpecifications.hasSystemPermissions(systemPermissions));
        }
        if (lastAccessAfter != null) {
            spec = (spec == null) ? SuperAdminSpecifications.lastAccessAfter(lastAccessAfter) : spec.and(SuperAdminSpecifications.lastAccessAfter(lastAccessAfter));
        }
        if (lastAccessBefore != null) {
            spec = (spec == null) ? SuperAdminSpecifications.lastAccessBefore(lastAccessBefore) : spec.and(SuperAdminSpecifications.lastAccessBefore(lastAccessBefore));
        }
        if (createdAfter != null) {
            spec = (spec == null) ? SuperAdminSpecifications.createdAfter(createdAfter) : spec.and(SuperAdminSpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? SuperAdminSpecifications.createdBefore(createdBefore) : spec.and(SuperAdminSpecifications.createdBefore(createdBefore));
        }
        
        return superAdminRepository.findAll(spec, pageable);
    }

    // UPDATE
    public SuperAdmin updateSuperAdmin(Long id, SuperAdmin superAdminDetails) {
        log.info("Updating super admin with ID: {}", id);
        
        SuperAdmin existingSuperAdmin = superAdminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Super admin not found with ID: " + id));

        // Update fields
        if (superAdminDetails.getFirstName() != null) {
            existingSuperAdmin.setFirstName(superAdminDetails.getFirstName());
        }
        if (superAdminDetails.getLastName() != null) {
            existingSuperAdmin.setLastName(superAdminDetails.getLastName());
        }
        if (superAdminDetails.getEmail() != null && !superAdminDetails.getEmail().equals(existingSuperAdmin.getEmail())) {
            // Check if new email already exists
            if (superAdminRepository.findByEmail(superAdminDetails.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists: " + superAdminDetails.getEmail());
            }
            existingSuperAdmin.setEmail(superAdminDetails.getEmail());
        }
        if (superAdminDetails.getPhoneNumber() != null) {
            existingSuperAdmin.setPhoneNumber(superAdminDetails.getPhoneNumber());
        }
        if (superAdminDetails.getSystemPermissions() != null) {
            existingSuperAdmin.setSystemPermissions(superAdminDetails.getSystemPermissions());
        }
        if (superAdminDetails.getPassword() != null && !superAdminDetails.getPassword().startsWith("$2")) {
            existingSuperAdmin.setPassword(passwordEncoder.encode(superAdminDetails.getPassword()));
        }
        if (superAdminDetails.getSecurityLevel() != null) {
            existingSuperAdmin.setSecurityLevel(superAdminDetails.getSecurityLevel());
        }

        SuperAdmin updatedSuperAdmin = superAdminRepository.save(existingSuperAdmin);
        log.info("Super admin updated successfully with ID: {}", updatedSuperAdmin.getId());
        return updatedSuperAdmin;
    }

    // UPDATE - Update last system access
    public SuperAdmin updateLastSystemAccess(Long superAdminId) {
        log.info("Updating last system access for super admin ID: {}", superAdminId);
        
        SuperAdmin superAdmin = superAdminRepository.findById(superAdminId)
                .orElseThrow(() -> new RuntimeException("Super admin not found with ID: " + superAdminId));
        
        superAdmin.setLastSystemAccess(LocalDateTime.now());
        return superAdminRepository.save(superAdmin);
    }

    // UPDATE - Update system permissions
    public SuperAdmin updateSystemPermissions(Long superAdminId, String systemPermissions) {
        log.info("Updating system permissions for super admin ID: {}", superAdminId);
        
        SuperAdmin superAdmin = superAdminRepository.findById(superAdminId)
                .orElseThrow(() -> new RuntimeException("Super admin not found with ID: " + superAdminId));
        
        superAdmin.setSystemPermissions(systemPermissions);
        return superAdminRepository.save(superAdmin);
    }

    // UPDATE - Update security level
    public SuperAdmin updateSecurityLevel(Long superAdminId, Integer securityLevel) {
        log.info("Updating security level for super admin ID: {} to: {}", superAdminId, securityLevel);
        
        SuperAdmin superAdmin = superAdminRepository.findById(superAdminId)
                .orElseThrow(() -> new RuntimeException("Super admin not found with ID: " + superAdminId));
        
        superAdmin.setSecurityLevel(securityLevel);
        return superAdminRepository.save(superAdmin);
    }

    // DELETE - Soft delete (disable)
    public void disableSuperAdmin(Long id) {
        log.info("Disabling super admin with ID: {}", id);
        
        SuperAdmin superAdmin = superAdminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Super admin not found with ID: " + id));
        
        superAdmin.setEnabled(false);
        superAdminRepository.save(superAdmin);
        log.info("Super admin disabled successfully with ID: {}", id);
    }

    // DELETE - Hard delete
    public void deleteSuperAdmin(Long id) {
        log.info("Deleting super admin with ID: {}", id);
        
        if (!superAdminRepository.existsById(id)) {
            throw new RuntimeException("Super admin not found with ID: " + id);
        }
        
        superAdminRepository.deleteById(id);
        log.info("Super admin deleted successfully with ID: {}", id);
    }

    // STATISTICS
    @Transactional(readOnly = true)
    public Long countActiveSuperAdmins() {
        return superAdminRepository.countActiveAdmins();
    }

    @Transactional(readOnly = true)
    public Long countTotalSuperAdmins() {
        return superAdminRepository.count();
    }

    // STATISTICS - Get super admin statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getSuperAdminStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long active = countActiveSuperAdmins();
        stats.put("total", countTotalSuperAdmins());
        stats.put("active", active);
        stats.put("verified", superAdminRepository.countByIsEnabled(true));
        stats.put("activeAndVerified", active);
        stats.put("highSecurityLevel", getAdminsByMinimumSecurityLevel(8).size());
        stats.put("recentlyActive", getActiveAdminsSince(LocalDateTime.now().minusDays(7)).size());
        stats.put("totalSystemAccess", superAdminRepository.countWithSystemAccess());
        Double averageSecurityLevel = superAdminRepository.getAverageSecurityLevel();
        stats.put("averageSecurityLevel", averageSecurityLevel != null ? averageSecurityLevel : 0.0);
        return stats;
    }
}