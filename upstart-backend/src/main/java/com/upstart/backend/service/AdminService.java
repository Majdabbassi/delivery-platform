package com.upstart.backend.service;

import com.upstart.backend.entity.Admin;
import com.upstart.backend.exception.ResourceNotFoundException;
import com.upstart.backend.repository.AdminRepository;
import com.upstart.backend.specification.AdminSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Create operations
    public Admin createAdmin(Admin admin) {
        if (adminRepository.existsByUsername(admin.getUsername())) {
            throw new RuntimeException("Admin username already exists: " + admin.getUsername());
        }
        if (adminRepository.existsByEmail(admin.getEmail())) {
            throw new RuntimeException("Admin email already exists: " + admin.getEmail());
        }
        if (admin.getPassword() != null && !admin.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        if (admin.getRole() == null) {
            admin.setRole(Admin.AdminRole.ADMIN);
        }
        return adminRepository.save(admin);
    }

    // Read operations
    @Transactional(readOnly = true)
    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByUsername(String username) {
        return adminRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<Admin> findByNationalId(String nationalId) {
        return adminRepository.findByNationalId(nationalId);
    }

    @Transactional(readOnly = true)
    public Page<Admin> getAllAdmins(Pageable pageable) {
        return adminRepository.findAll(pageable);
    }

    // Search
    @Transactional(readOnly = true)
    public Page<Admin> searchAdmins(
            String username, String email, String firstName, String lastName,
            String phoneNumber, Admin.AdminRole role, Boolean enabled, Boolean verified,
            String department, String position, String nationalId,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDate hiredAfter, LocalDate hiredBefore,
            LocalDate bornAfter, LocalDate bornBefore,
            String address, String emergencyContact,
            BigDecimal minSalary, BigDecimal maxSalary,
            Boolean isActive,
            Pageable pageable) {

        Specification<Admin> spec = null;

        if (username != null && !username.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasUsername(username) : spec.and(AdminSpecifications.hasUsername(username));
        }
        if (email != null && !email.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasEmail(email) : spec.and(AdminSpecifications.hasEmail(email));
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasFirstName(firstName) : spec.and(AdminSpecifications.hasFirstName(firstName));
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasLastName(lastName) : spec.and(AdminSpecifications.hasLastName(lastName));
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasPhoneNumber(phoneNumber) : spec.and(AdminSpecifications.hasPhoneNumber(phoneNumber));
        }
        if (role != null) {
            spec = (spec == null) ? AdminSpecifications.hasRole(role) : spec.and(AdminSpecifications.hasRole(role));
        }
        if (enabled != null) {
            spec = (spec == null) ? AdminSpecifications.isEnabled(enabled) : spec.and(AdminSpecifications.isEnabled(enabled));
        }
        if (verified != null) {
            spec = (spec == null) ? AdminSpecifications.isVerified(verified) : spec.and(AdminSpecifications.isVerified(verified));
        }
        if (department != null && !department.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasDepartment(department) : spec.and(AdminSpecifications.hasDepartment(department));
        }
        if (position != null && !position.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasPosition(position) : spec.and(AdminSpecifications.hasPosition(position));
        }
        if (nationalId != null && !nationalId.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasNationalId(nationalId) : spec.and(AdminSpecifications.hasNationalId(nationalId));
        }
        if (createdAfter != null) {
            spec = (spec == null) ? AdminSpecifications.createdAfter(createdAfter) : spec.and(AdminSpecifications.createdAfter(createdAfter));
        }
        if (createdBefore != null) {
            spec = (spec == null) ? AdminSpecifications.createdBefore(createdBefore) : spec.and(AdminSpecifications.createdBefore(createdBefore));
        }
        if (hiredAfter != null) {
            spec = (spec == null) ? AdminSpecifications.hiredAfter(hiredAfter) : spec.and(AdminSpecifications.hiredAfter(hiredAfter));
        }
        if (hiredBefore != null) {
            spec = (spec == null) ? AdminSpecifications.hiredBefore(hiredBefore) : spec.and(AdminSpecifications.hiredBefore(hiredBefore));
        }
        if (bornAfter != null) {
            spec = (spec == null) ? AdminSpecifications.bornAfter(bornAfter) : spec.and(AdminSpecifications.bornAfter(bornAfter));
        }
        if (bornBefore != null) {
            spec = (spec == null) ? AdminSpecifications.bornBefore(bornBefore) : spec.and(AdminSpecifications.bornBefore(bornBefore));
        }
        if (address != null && !address.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasAddress(address) : spec.and(AdminSpecifications.hasAddress(address));
        }
        if (emergencyContact != null && !emergencyContact.trim().isEmpty()) {
            spec = (spec == null) ? AdminSpecifications.hasEmergencyContact(emergencyContact) : spec.and(AdminSpecifications.hasEmergencyContact(emergencyContact));
        }
        if (minSalary != null) {
            spec = (spec == null) ? AdminSpecifications.hasMinSalary(minSalary) : spec.and(AdminSpecifications.hasMinSalary(minSalary));
        }
        if (maxSalary != null) {
            spec = (spec == null) ? AdminSpecifications.hasMaxSalary(maxSalary) : spec.and(AdminSpecifications.hasMaxSalary(maxSalary));
        }
        if (isActive != null) {
            spec = (spec == null) ? AdminSpecifications.isActive(isActive) : spec.and(AdminSpecifications.isActive(isActive));
        }

        return adminRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Admin> searchByName(String searchTerm, Pageable pageable) {
        return adminRepository.findAll(AdminSpecifications.searchByName(searchTerm), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Admin> searchByContact(String searchTerm, Pageable pageable) {
        return adminRepository.findAll(AdminSpecifications.searchByContact(searchTerm), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Admin> getActiveAndVerifiedAdmins(Pageable pageable) {
        return adminRepository.findAll(AdminSpecifications.isActiveAndVerified(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Admin> getSuperAdmins(Pageable pageable) {
        return adminRepository.findByRole(Admin.AdminRole.SUPER_ADMIN, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Admin> getAdminsByRole(Admin.AdminRole role, Pageable pageable) {
        return adminRepository.findByRole(role, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Admin> getAdminsByDepartment(String department, Pageable pageable) {
        return adminRepository.findByDepartment(department, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Admin> getRecentlyHiredAdmins(LocalDate since, Pageable pageable) {
        LocalDate effectiveSince = since != null ? since : LocalDate.now().minusDays(90);
        return adminRepository.findByHireDateGreaterThanEqual(effectiveSince, pageable);
    }

    // Update operations
    public Admin updateAdmin(Long id, Admin updatedAdmin) {
        Admin existing = getAdminById(id);

        if (updatedAdmin.getUsername() != null && !updatedAdmin.getUsername().equals(existing.getUsername())) {
            if (adminRepository.existsByUsernameAndIdNot(updatedAdmin.getUsername(), id)) {
                throw new RuntimeException("Admin username already exists: " + updatedAdmin.getUsername());
            }
            existing.setUsername(updatedAdmin.getUsername());
        }
        if (updatedAdmin.getEmail() != null && !updatedAdmin.getEmail().equals(existing.getEmail())) {
            if (adminRepository.existsByEmailAndIdNot(updatedAdmin.getEmail(), id)) {
                throw new RuntimeException("Admin email already exists: " + updatedAdmin.getEmail());
            }
            existing.setEmail(updatedAdmin.getEmail());
        }
        if (updatedAdmin.getPassword() != null && !updatedAdmin.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(updatedAdmin.getPassword()));
        }
        if (updatedAdmin.getFirstName() != null) {
            existing.setFirstName(updatedAdmin.getFirstName());
        }
        if (updatedAdmin.getLastName() != null) {
            existing.setLastName(updatedAdmin.getLastName());
        }
        if (updatedAdmin.getPhoneNumber() != null) {
            existing.setPhoneNumber(updatedAdmin.getPhoneNumber());
        }
        if (updatedAdmin.getRole() != null) {
            existing.setRole(updatedAdmin.getRole());
        }
        if (updatedAdmin.getEnabled() != null) {
            existing.setEnabled(updatedAdmin.getEnabled());
        }
        if (updatedAdmin.getVerified() != null) {
            existing.setVerified(updatedAdmin.getVerified());
        }
        if (updatedAdmin.getProfilePicture() != null) {
            existing.setProfilePicture(updatedAdmin.getProfilePicture());
        }
        if (updatedAdmin.getDepartment() != null) {
            existing.setDepartment(updatedAdmin.getDepartment());
        }
        if (updatedAdmin.getPosition() != null) {
            existing.setPosition(updatedAdmin.getPosition());
        }
        if (updatedAdmin.getPermissions() != null) {
            existing.setPermissions(updatedAdmin.getPermissions());
        }
        if (updatedAdmin.getAssignedModules() != null) {
            existing.setAssignedModules(updatedAdmin.getAssignedModules());
        }
        if (updatedAdmin.getAccessLevel() != null) {
            existing.setAccessLevel(updatedAdmin.getAccessLevel());
        }
        if (updatedAdmin.getIsLocked() != null) {
            existing.setIsLocked(updatedAdmin.getIsLocked());
        }
        if (updatedAdmin.getLockReason() != null) {
            existing.setLockReason(updatedAdmin.getLockReason());
        }
        if (updatedAdmin.getCanManageUsers() != null) {
            existing.setCanManageUsers(updatedAdmin.getCanManageUsers());
        }
        if (updatedAdmin.getCanManageSystem() != null) {
            existing.setCanManageSystem(updatedAdmin.getCanManageSystem());
        }
        if (updatedAdmin.getCanViewReports() != null) {
            existing.setCanViewReports(updatedAdmin.getCanViewReports());
        }
        if (updatedAdmin.getCanManageContent() != null) {
            existing.setCanManageContent(updatedAdmin.getCanManageContent());
        }
        if (updatedAdmin.getSessionTimeout() != null) {
            existing.setSessionTimeout(updatedAdmin.getSessionTimeout());
        }
        if (updatedAdmin.getDescription() != null) {
            existing.setDescription(updatedAdmin.getDescription());
        }
        if (updatedAdmin.getEmergencyContact() != null) {
            existing.setEmergencyContact(updatedAdmin.getEmergencyContact());
        }
        if (updatedAdmin.getAddress() != null) {
            existing.setAddress(updatedAdmin.getAddress());
        }
        if (updatedAdmin.getNationalId() != null) {
            if (!updatedAdmin.getNationalId().equals(existing.getNationalId())
                    && adminRepository.existsByNationalIdAndIdNot(updatedAdmin.getNationalId(), id)) {
                throw new RuntimeException("Admin national ID already exists: " + updatedAdmin.getNationalId());
            }
            existing.setNationalId(updatedAdmin.getNationalId());
        }
        if (updatedAdmin.getDateOfBirth() != null) {
            existing.setDateOfBirth(updatedAdmin.getDateOfBirth());
        }
        if (updatedAdmin.getHireDate() != null) {
            existing.setHireDate(updatedAdmin.getHireDate());
        }
        if (updatedAdmin.getSalary() != null) {
            existing.setSalary(updatedAdmin.getSalary());
        }
        if (updatedAdmin.getIsActive() != null) {
            existing.setIsActive(updatedAdmin.getIsActive());
        }
        if (updatedAdmin.getNotes() != null) {
            existing.setNotes(updatedAdmin.getNotes());
        }
        if (updatedAdmin.getCreatedBy() != null) {
            existing.setCreatedBy(updatedAdmin.getCreatedBy());
        }
        if (updatedAdmin.getUpdatedBy() != null) {
            existing.setUpdatedBy(updatedAdmin.getUpdatedBy());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return adminRepository.save(existing);
    }

    public Admin updateEnabledStatus(Long id, boolean enabled) {
        Admin admin = getAdminById(id);
        admin.setEnabled(enabled);
        admin.setUpdatedAt(LocalDateTime.now());
        return adminRepository.save(admin);
    }

    public Admin updateVerifiedStatus(Long id, boolean verified) {
        Admin admin = getAdminById(id);
        admin.setVerified(verified);
        admin.setUpdatedAt(LocalDateTime.now());
        return adminRepository.save(admin);
    }

    public Admin updateActiveStatus(Long id, boolean isActive) {
        Admin admin = getAdminById(id);
        admin.setIsActive(isActive);
        admin.setUpdatedAt(LocalDateTime.now());
        return adminRepository.save(admin);
    }

    public Admin updateRole(Long id, Admin.AdminRole role) {
        Admin admin = getAdminById(id);
        admin.setRole(role);
        admin.setUpdatedAt(LocalDateTime.now());
        return adminRepository.save(admin);
    }

    public Admin lockAdmin(Long id, String reason) {
        Admin admin = getAdminById(id);
        admin.setIsLocked(true);
        admin.setLockReason(reason != null && !reason.isBlank() ? reason : "Manually locked by super admin");
        admin.setUpdatedAt(LocalDateTime.now());
        return adminRepository.save(admin);
    }

    public Admin unlockAdmin(Long id) {
        Admin admin = getAdminById(id);
        admin.setIsLocked(false);
        admin.setLockReason(null);
        admin.setLoginAttempts(0);
        admin.setUpdatedAt(LocalDateTime.now());
        return adminRepository.save(admin);
    }

    // Delete operations
    public void deleteAdmin(Long id) {
        Admin admin = getAdminById(id);
        adminRepository.delete(admin);
    }

    public void softDeleteAdmin(Long id) {
        Admin admin = getAdminById(id);
        admin.setIsActive(false);
        admin.setEnabled(false);
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);
    }

    // Validation methods
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return adminRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return adminRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByNationalId(String nationalId) {
        return adminRepository.existsByNationalId(nationalId);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsernameAndIdNot(String username, Long id) {
        return adminRepository.existsByUsernameAndIdNot(username, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return adminRepository.existsByEmailAndIdNot(email, id);
    }

    @Transactional(readOnly = true)
    public boolean existsByNationalIdAndIdNot(String nationalId, Long id) {
        return adminRepository.existsByNationalIdAndIdNot(nationalId, id);
    }

    // Statistical operations
    @Transactional(readOnly = true)
    public long countAllAdmins() {
        return adminRepository.count();
    }

    @Transactional(readOnly = true)
    public long countActiveAdmins() {
        return adminRepository.countByIsActive(true);
    }

    @Transactional(readOnly = true)
    public long countVerifiedAdmins() {
        return adminRepository.countByVerified(true);
    }

    @Transactional(readOnly = true)
    public long countActiveAndVerifiedAdmins() {
        return adminRepository.countByEnabledAndVerified(true, true);
    }

    @Transactional(readOnly = true)
    public long countSuperAdmins() {
        return adminRepository.countByRole(Admin.AdminRole.SUPER_ADMIN);
    }

    @Transactional(readOnly = true)
    public long countAdmins() {
        return adminRepository.countByRole(Admin.AdminRole.ADMIN);
    }

    @Transactional(readOnly = true)
    public long countModerators() {
        return adminRepository.countByRole(Admin.AdminRole.MODERATOR);
    }

    @Transactional(readOnly = true)
    public long countLockedAdmins() {
        return adminRepository.countByIsLocked(true);
    }

    @Transactional(readOnly = true)
    public long countRecentlyHiredAdmins() {
        return adminRepository.countByHireDateGreaterThanEqual(LocalDate.now().minusDays(90));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = countAllAdmins();
        long active = countActiveAdmins();
        long verified = countVerifiedAdmins();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("verified", verified);
        stats.put("activeAndVerified", countActiveAndVerifiedAdmins());
        stats.put("superAdmins", countSuperAdmins());
        stats.put("admins", countAdmins());
        stats.put("moderators", countModerators());
        stats.put("recentlyHired", countRecentlyHiredAdmins());
        stats.put("totalAdmins", total);
        stats.put("activeAdmins", active);
        stats.put("lockedAdmins", countLockedAdmins());
        return stats;
    }
}