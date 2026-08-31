package com.upstart.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admins")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    public enum AdminRole {
        SUPER_ADMIN, ADMIN, MODERATOR
    }

    public enum AccessLevel {
        FULL, LIMITED, READ_ONLY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Email is required")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password")
    private String password;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private AdminRole role = AdminRole.ADMIN;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "department")
    private String department;

    @Column(name = "position")
    private String position;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_permissions", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "permission")
    private List<String> permissions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_assigned_modules", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "module")
    private List<String> assignedModules = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level")
    private AccessLevel accessLevel = AccessLevel.LIMITED;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "lock_reason")
    private String lockReason;

    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;

    @Column(name = "can_manage_users")
    private Boolean canManageUsers = false;

    @Column(name = "can_manage_system")
    private Boolean canManageSystem = false;

    @Column(name = "can_view_reports")
    private Boolean canViewReports = false;

    @Column(name = "can_manage_content")
    private Boolean canManageContent = false;

    @Column(name = "session_timeout")
    private Integer sessionTimeout = 30;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "address")
    private String address;

    @Column(name = "national_id", unique = true)
    private String nationalId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (role == null) {
            role = AdminRole.ADMIN;
        }
        if (enabled == null) {
            enabled = true;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (isLocked == null) {
            isLocked = false;
        }
        if (loginAttempts == null) {
            loginAttempts = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}