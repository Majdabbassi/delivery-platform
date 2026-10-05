package com.swiftdeliver.backend.repository;

import com.swiftdeliver.backend.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    Optional<Bid> findByBidId(String bidId);

    List<Bid> findByOrderIdOrderBySubmittedAtDesc(Long orderId);

    Page<Bid> findByDeliveryCompanyIdOrderBySubmittedAtDesc(Long deliveryCompanyId, Pageable pageable);

    Page<Bid> findByDriverIdOrderBySubmittedAtDesc(Long driverId, Pageable pageable);

    List<Bid> findByStatusAndSubmittedAtBefore(Bid.BidStatus status, LocalDateTime submittedBefore);

    List<Bid> findByOrderIdAndStatus(Long orderId, Bid.BidStatus status);

    List<Bid> findByOrderIdAndStatusAndDeliveryCompanyId(Long orderId, Bid.BidStatus status, Long deliveryCompanyId);

    List<Bid> findByOrderIdAndStatusAndDriverId(Long orderId, Bid.BidStatus status, Long driverId);

    List<Bid> findBySubmittedAtBetween(LocalDateTime from, LocalDateTime to);

    List<Bid> findByStatus(Bid.BidStatus status);

    long countByDeliveryCompanyId(Long deliveryCompanyId);

    long countByDeliveryCompanyIdAndStatus(Long deliveryCompanyId, Bid.BidStatus status);

    long countByDriverId(Long driverId);

    long countByDriverIdAndStatus(Long driverId, Bid.BidStatus status);

    long countByStatusAndSubmittedAtBetween(Bid.BidStatus status, LocalDateTime from, LocalDateTime to);
}
