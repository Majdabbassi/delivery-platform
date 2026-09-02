package com.swiftdeliver.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persists a single "pool placement": a pending order made visible to a
 * delivery company in the order marketplace. Replaces the in-memory
 * company-to-order maps so that pool visibility survives restarts.
 */
@Entity
@Table(name = "pooled_orders",
       uniqueConstraints = @UniqueConstraint(columnNames = {"delivery_company_id", "order_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PooledOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_company_id", nullable = false)
    private Long deliveryCompanyId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    public PooledOrder(Long deliveryCompanyId, Long orderId) {
        this.deliveryCompanyId = deliveryCompanyId;
        this.orderId = orderId;
        this.addedAt = LocalDateTime.now();
    }
}
