package com.upstart.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer_users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUser extends User {
    
    @Column(name = "default_address", columnDefinition = "TEXT")
    private String defaultAddress;
    
    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;
    
    @Column(name = "preferred_payment_method")
    private String preferredPaymentMethod; // CARD, CASH, WALLET, etc.
    
    @Column(name = "total_orders")
    private Long totalOrders = 0L;
    
    @Column(name = "total_spent", precision = 10, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "gender")
    private String gender; // MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    
    @Column(name = "notification_preferences", columnDefinition = "TEXT")
    private String notificationPreferences; // JSON string of notification settings
    
    @Column(name = "is_premium")
    private Boolean isPremium = false;
    
    public CustomerUser(String username, String email, String password, String firstName, String lastName) {
        super();
        setUsername(username);
        setEmail(email);
        setPassword(password);
        setFirstName(firstName);
        setLastName(lastName);
        setRole(Role.CLIENT);
        setEnabled(true);
        this.loyaltyPoints = 0;
        this.totalOrders = 0L;
        this.totalSpent = BigDecimal.ZERO;
        this.isPremium = false;
    }
    
    public CustomerUser(String username, String email, String password, String firstName, String lastName, 
                        String phoneNumber, String defaultAddress) {
        this(username, email, password, firstName, lastName);
        setPhoneNumber(phoneNumber);
        this.defaultAddress = defaultAddress;
    }
}