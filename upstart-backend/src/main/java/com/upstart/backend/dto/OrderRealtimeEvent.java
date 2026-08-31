package com.upstart.backend.dto;

import com.upstart.backend.entity.Order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRealtimeEvent {

    private String type;

    private Long orderId;
    private String orderNumber;
    private String trackingNumber;

    private OrderStatus status;
    private Long driverPersonId;
    private String driverName;

    private Double latitude;
    private Double longitude;
    private Double speedKmh;

    private LocalDateTime timestamp;
}
