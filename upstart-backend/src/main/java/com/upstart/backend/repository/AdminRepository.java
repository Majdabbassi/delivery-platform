package com.upstart.backend.repository;

import com.upstart.backend.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long>, JpaSpecificationExecutor<Admin> {

    Optional<Admin> findByUsername(String username);

    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByNationalId(String nationalId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

    long countByRole(Admin.AdminRole role);

    long countByRoleIn(List<Admin.AdminRole> roles);

    long countByEnabled(Boolean enabled);

    long countByVerified(Boolean verified);

    long countByEnabledAndVerified(Boolean enabled, Boolean verified);

    long countByIsLocked(Boolean isLocked);

    long countByIsActive(Boolean isActive);

    long countByHireDateGreaterThanEqual(LocalDate since);

    Page<Admin> findByRole(Admin.AdminRole role, Pageable pageable);

    Page<Admin> findByRoleIn(List<Admin.AdminRole> roles, Pageable pageable);

    Page<Admin> findByDepartment(String department, Pageable pageable);

    Page<Admin> findByHireDateGreaterThanEqual(LocalDate since, Pageable pageable);

    Page<Admin> findByEnabledAndVerified(Boolean enabled, Boolean verified, Pageable pageable);
}