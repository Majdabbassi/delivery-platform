package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long>, JpaSpecificationExecutor<SuperAdmin> {
    
    Optional<SuperAdmin> findByUsername(String username);
    
    Optional<SuperAdmin> findByEmail(String email);
    
    Optional<SuperAdmin> findByUsernameOrEmail(String username, String email);
    
    @Query("SELECT sa FROM SuperAdmin sa WHERE sa.lastSystemAccess >= :since")
    List<SuperAdmin> findActiveAdminsSince(LocalDateTime since);
    
    @Query("SELECT sa FROM SuperAdmin sa WHERE sa.securityLevel >= :minLevel")
    List<SuperAdmin> findByMinimumSecurityLevel(Integer minLevel);
    
    @Query("SELECT COUNT(sa) FROM SuperAdmin sa WHERE sa.isEnabled = true")
    Long countActiveAdmins();

    long countByIsEnabled(Boolean isEnabled);

    @Query("SELECT COUNT(sa) FROM SuperAdmin sa WHERE sa.lastSystemAccess IS NOT NULL")
    Long countWithSystemAccess();

    @Query("SELECT AVG(sa.securityLevel) FROM SuperAdmin sa")
    Double getAverageSecurityLevel();
}