package com.upstart.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Bid id is required")
    @Column(name = "bid_id", nullable = false, unique = true)
    private String bidId;

    @NotNull(message = "Order is required")
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bidder_type", nullable = false, length = 30)
    private BidderType bidderType;

    @Column(name = "delivery_company_id", nullable = true)
    private Long deliveryCompanyId;

    @Column(name = "driver_id", nullable = true)
    private Long driverId;

    @NotNull(message = "Bid amount is required")
    @Column(name = "bid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal bidAmount;

    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;

    @Column(name = "message", length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BidStatus status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "response_message")
    private String responseMessage;

    public enum BidStatus {
        SUBMITTED, ACCEPTED, REJECTED, WITHDRAWN, EXPIRED
    }

    public enum BidderType {
        COMPANY,
        INDEPENDENT_DRIVER
    }
}
