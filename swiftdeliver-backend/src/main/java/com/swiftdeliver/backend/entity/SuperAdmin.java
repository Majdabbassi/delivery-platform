package com.swiftdeliver.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "super_admins")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdmin extends User {
    
    @Column(name = "system_permissions", columnDefinition = "TEXT")
    private String systemPermissions; // JSON string of permissions
    
    @Column(name = "last_system_access")
    private LocalDateTime lastSystemAccess;
    
    @Column(name = "security_level")
    private Integer securityLevel = 10; // Highest security level
    
    public SuperAdmin(String username, String email, String password, String firstName, String lastName) {
        super();
        setUsername(username);
        setEmail(email);
        setPassword(password);
        setFirstName(firstName);
        setLastName(lastName);
        setRole(Role.SUPER_ADMIN);
        setEnabled(true);
        this.securityLevel = 10;
    }
}