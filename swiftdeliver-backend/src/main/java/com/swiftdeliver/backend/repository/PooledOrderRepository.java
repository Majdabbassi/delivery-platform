package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.PooledOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface PooledOrderRepository extends JpaRepository<PooledOrder, Long> {

    List<PooledOrder> findByDeliveryCompanyId(Long deliveryCompanyId);

    List<PooledOrder> findByOrderId(Long orderId);

    List<PooledOrder> findByDeliveryCompanyIdIn(Set<Long> deliveryCompanyIds);

    long countByDeliveryCompanyId(Long deliveryCompanyId);

    long countByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    void deleteByDeliveryCompanyIdAndOrderId(Long deliveryCompanyId, Long orderId);

    boolean existsByDeliveryCompanyIdAndOrderId(Long deliveryCompanyId, Long orderId);
}
