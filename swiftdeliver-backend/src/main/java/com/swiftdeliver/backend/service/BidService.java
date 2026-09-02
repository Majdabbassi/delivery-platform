package com.swiftdeliver.backend.service;

import com.swiftdeliver.backend.entity.Bid;
import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.DriverPerson;
import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.repository.BidRepository;
import com.swiftdeliver.backend.repository.DeliveryCompanyRepository;
import com.swiftdeliver.backend.repository.DriverPersonRepository;
import com.swiftdeliver.backend.repository.OrderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing bidding functionality for orders.
 * Persists bids in the database rather than in-memory storage so that bids
 * survive restarts and can be queried, filtered, and audited.
 */
@Service
@Transactional
public class BidService {

    private static final Logger logger = LoggerFactory.getLogger(BidService.class);

    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final DeliveryCompanyRepository deliveryCompanyRepository;
    private final DriverPersonRepository driverPersonRepository;
    private final OrderPoolService orderPoolService;

    public BidService(BidRepository bidRepository,
                      OrderRepository orderRepository,
                      DeliveryCompanyRepository deliveryCompanyRepository,
                      DriverPersonRepository driverPersonRepository,
                      OrderPoolService orderPoolService) {
        this.bidRepository = bidRepository;
        this.orderRepository = orderRepository;
        this.deliveryCompanyRepository = deliveryCompanyRepository;
        this.driverPersonRepository = driverPersonRepository;
        this.orderPoolService = orderPoolService;
    }

    /**
     * Submit a bid for an order
     */
    public Bid submitBid(BidRequest bidRequest) {
        validateBidRequest(bidRequest);

        Order order = orderRepository.findById(bidRequest.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        boolean isIndependentDriver = bidRequest.getBidderType() == Bid.BidderType.INDEPENDENT_DRIVER;

        Bid bid = new Bid();
        bid.setBidId(generateBidId());
        bid.setOrderId(bidRequest.getOrderId());
        bid.setBidderType(isIndependentDriver ? Bid.BidderType.INDEPENDENT_DRIVER : Bid.BidderType.COMPANY);
        bid.setDeliveryCompanyId(isIndependentDriver ? null : bidRequest.getDeliveryCompanyId());
        bid.setDriverId(isIndependentDriver ? bidRequest.getDriverId() : null);
        bid.setBidAmount(bidRequest.getBidAmount());
        bid.setEstimatedDeliveryTime(bidRequest.getEstimatedDeliveryTime());
        bid.setMessage(bidRequest.getMessage());
        bid.setStatus(Bid.BidStatus.SUBMITTED);
        bid.setSubmittedAt(LocalDateTime.now());

        if (isIndependentDriver) {
            DriverPerson driver = driverPersonRepository.findById(bid.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
            if (driver.getDeliveryCompany() != null) {
                throw new IllegalStateException(
                        "Drivers employed by a delivery company cannot bid directly. "
                        + "Only the delivery company owner may bid on their behalf.");
            }
            if (!Boolean.TRUE.equals(driver.getIsVerified())) {
                throw new IllegalStateException("Driver must be verified to bid");
            }
            if (!Boolean.TRUE.equals(driver.getIsAvailable())) {
                throw new IllegalStateException("Driver must be available to bid");
            }
            if (!isOrderAvailableForBidding(order)) {
                throw new IllegalStateException("Order is no longer available for bidding");
            }
            if (!canDriverBidOnOrder(order, driver.getId())) {
                throw new IllegalStateException("Driver has already submitted a bid on this order");
            }
            logger.info("Bid {} submitted by independent driver {} for order {} with amount {}",
                       bid.getBidId(), driver.getId(), order.getId(), bid.getBidAmount());
        } else {
            DeliveryCompany company = deliveryCompanyRepository.findById(bidRequest.getDeliveryCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Delivery company not found"));

            if (!isOrderAvailableForBidding(order)) {
                throw new IllegalStateException("Order is no longer available for bidding");
            }
            if (!canCompanyBidOnOrder(company, order)) {
                throw new IllegalStateException("Company is not eligible to bid on this order");
            }
            logger.info("Bid {} submitted by company {} for order {} with amount {}",
                       bid.getBidId(), company.getId(), order.getId(), bid.getBidAmount());
        }

        bid = bidRepository.save(bid);
        return bid;
    }

    /**
     * Get all bids for an order, ordered by submission date descending
     */
    @Transactional(readOnly = true)
    public List<Bid> getBidsForOrder(Long orderId) {
        return bidRepository.findByOrderIdOrderBySubmittedAtDesc(orderId);
    }

    /**
     * Get bids submitted by a delivery company
     */
    @Transactional(readOnly = true)
    public Page<Bid> getBidsByCompany(Long deliveryCompanyId, Pageable pageable) {
        return bidRepository.findByDeliveryCompanyIdOrderBySubmittedAtDesc(deliveryCompanyId, pageable);
    }

    /**
     * Get bids submitted by an independent driver
     */
    @Transactional(readOnly = true)
    public Page<Bid> getBidsByDriver(Long driverId, Pageable pageable) {
        return bidRepository.findByDriverIdOrderBySubmittedAtDesc(driverId, pageable);
    }

    /**
     * Accept a bid
     */
    public Bid acceptBid(String bidId, String acceptanceMessage) {
        Bid bid = getBid(bidId);

        if (bid.getStatus() != Bid.BidStatus.SUBMITTED) {
            throw new IllegalStateException("Bid is not in submitted status");
        }

        Order order = orderRepository.findById(bid.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!isOrderAvailableForBidding(order)) {
            throw new IllegalStateException("Order is no longer available for bidding");
        }

        bid.setStatus(Bid.BidStatus.ACCEPTED);
        bid.setResponseMessage(acceptanceMessage);
        bid.setRespondedAt(LocalDateTime.now());
        bidRepository.save(bid);

        assignOrderToBid(order, bid);
        rejectOtherBids(bid.getOrderId(), bid.getBidId());
        orderPoolService.removeOrderFromPools(order.getId());

        logger.info("Bid {} accepted for order {}", bidId, order.getId());

        return bid;
    }

    /**
     * Reject a bid
     */
    public Bid rejectBid(String bidId, String rejectionMessage) {
        Bid bid = getBid(bidId);

        if (bid.getStatus() != Bid.BidStatus.SUBMITTED) {
            throw new IllegalStateException("Bid is not in submitted status");
        }

        bid.setStatus(Bid.BidStatus.REJECTED);
        bid.setResponseMessage(rejectionMessage);
        bid.setRespondedAt(LocalDateTime.now());
        bidRepository.save(bid);

        logger.info("Bid {} rejected for order {}", bidId, bid.getOrderId());

        return bid;
    }

    /**
     * Withdraw a bid (by delivery company)
     */
    public Bid withdrawBid(String bidId) {
        Bid bid = getBid(bidId);

        if (bid.getStatus() != Bid.BidStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted bids can be withdrawn");
        }

        bid.setStatus(Bid.BidStatus.WITHDRAWN);
        bid.setRespondedAt(LocalDateTime.now());
        bidRepository.save(bid);

        logger.info("Bid {} withdrawn by company {}", bidId, bid.getDeliveryCompanyId());

        return bid;
    }

    /**
     * Get bid statistics for a delivery company
     */
    @Transactional(readOnly = true)
    public BidStatistics getBidStatistics(Long deliveryCompanyId) {
        BidStatistics stats = new BidStatistics();
        stats.setBidsSubmitted(bidRepository.countByDeliveryCompanyId(deliveryCompanyId));
        stats.setBidsAccepted(
                bidRepository.countByDeliveryCompanyIdAndStatus(deliveryCompanyId, Bid.BidStatus.ACCEPTED));
        stats.setBidsRejected(
                bidRepository.countByDeliveryCompanyIdAndStatus(deliveryCompanyId, Bid.BidStatus.REJECTED));
        stats.setBidsWithdrawn(
                bidRepository.countByDeliveryCompanyIdAndStatus(deliveryCompanyId, Bid.BidStatus.WITHDRAWN));
        stats.setBidsExpired(
                bidRepository.countByDeliveryCompanyIdAndStatus(deliveryCompanyId, Bid.BidStatus.EXPIRED));
        return stats;
    }

    /**
     * Get bid statistics for an independent driver
     */
    @Transactional(readOnly = true)
    public BidStatistics getDriverBidStatistics(Long driverId) {
        BidStatistics stats = new BidStatistics();
        stats.setBidsSubmitted(bidRepository.countByDriverId(driverId));
        stats.setBidsAccepted(bidRepository.countByDriverIdAndStatus(driverId, Bid.BidStatus.ACCEPTED));
        stats.setBidsRejected(bidRepository.countByDriverIdAndStatus(driverId, Bid.BidStatus.REJECTED));
        stats.setBidsWithdrawn(bidRepository.countByDriverIdAndStatus(driverId, Bid.BidStatus.WITHDRAWN));
        stats.setBidsExpired(bidRepository.countByDriverIdAndStatus(driverId, Bid.BidStatus.EXPIRED));
        return stats;
    }

    /**
     * Get bid rankings for an order
     */
    @Transactional(readOnly = true)
    public List<BidRanking> getBidRankings(Long orderId) {
        List<Bid> bids = getBidsForOrder(orderId);

        return bids.stream()
            .filter(bid -> bid.getStatus() == Bid.BidStatus.SUBMITTED)
            .map(this::calculateBidRanking)
            .sorted(Comparator.comparing(BidRanking::getScore).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Auto-expire old submitted bids
     */
    public void expireOldBids() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        List<Bid> expiredBids = bidRepository.findByStatusAndSubmittedAtBefore(Bid.BidStatus.SUBMITTED, cutoff);

        LocalDateTime now = LocalDateTime.now();
        for (Bid bid : expiredBids) {
            bid.setStatus(Bid.BidStatus.EXPIRED);
            bid.setRespondedAt(now);
            bidRepository.save(bid);
        }

        logger.info("Expired {} old bids", expiredBids.size());
    }

    /**
     * Get bidding activity summary for a time range
     */
    @Transactional(readOnly = true)
    public BiddingActivitySummary getBiddingActivitySummary(LocalDateTime from, LocalDateTime to) {
        List<Bid> bidsInPeriod = bidRepository.findBySubmittedAtBetween(from, to);

        long totalBids = bidsInPeriod.size();
        long acceptedBids = bidsInPeriod.stream()
            .filter(bid -> bid.getStatus() == Bid.BidStatus.ACCEPTED)
            .count();

        BigDecimal totalBidValue = bidsInPeriod.stream()
            .map(Bid::getBidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageBidAmount = totalBids > 0 ?
            totalBidValue.divide(BigDecimal.valueOf(totalBids), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;

        Map<Long, Long> bidsByCompany = bidsInPeriod.stream()
            .collect(Collectors.groupingBy(Bid::getDeliveryCompanyId, Collectors.counting()));

        double acceptanceRate = totalBids > 0 ? (double) acceptedBids / totalBids * 100 : 0;

        return new BiddingActivitySummary(
            totalBids,
            acceptedBids,
            totalBidValue,
            averageBidAmount,
            acceptanceRate,
            bidsByCompany
        );
    }

    // Private helper methods

    private Bid getBid(String bidId) {
        return bidRepository.findByBidId(bidId)
            .orElseThrow(() -> new IllegalArgumentException("Bid not found"));
    }

    /**
     * Public accessor for a single bid by its bid id.
     */
    @Transactional(readOnly = true)
    public Bid getBidByBidId(String bidId) {
        return getBid(bidId);
    }

    private void validateBidRequest(BidRequest request) {
        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (request.getBidAmount() == null || request.getBidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid bid amount is required");
        }
        if (request.getEstimatedDeliveryTime() == null) {
            throw new IllegalArgumentException("Estimated delivery time is required");
        }
        if (request.getBidderType() == null) {
            request.setBidderType(Bid.BidderType.COMPANY); // backward compatible default
        }
        if (request.getBidderType() == Bid.BidderType.COMPANY && request.getDeliveryCompanyId() == null) {
            throw new IllegalArgumentException("Delivery company ID is required for company bids");
        }
        if (request.getBidderType() == Bid.BidderType.INDEPENDENT_DRIVER && request.getDriverId() == null) {
            throw new IllegalArgumentException("Driver ID is required for independent driver bids");
        }
    }

    private boolean isOrderAvailableForBidding(Order order) {
        return order.getStatus() == Order.OrderStatus.OPEN_FOR_BID
               && order.getDeliveryCompany() == null;
    }

    private boolean canCompanyBidOnOrder(DeliveryCompany company, Order order) {
        if (!company.getIsActive() || !company.getIsLicensed()) {
            return false;
        }

        List<Bid> existingBids = bidRepository
                .findByOrderIdAndStatusAndDeliveryCompanyId(order.getId(), Bid.BidStatus.SUBMITTED, company.getId());

        return existingBids.isEmpty();
    }

    private boolean canDriverBidOnOrder(Order order, Long driverId) {
        List<Bid> existingBids = bidRepository
                .findByOrderIdAndStatusAndDriverId(order.getId(), Bid.BidStatus.SUBMITTED, driverId);

        return existingBids.isEmpty();
    }

    private String generateBidId() {
        return "BID_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void assignOrderToBid(Order order, Bid bid) {
        order.setStatus(Order.OrderStatus.ASSIGNED);
        order.setDeliveryFee(bid.getBidAmount());
        order.setAssignedAt(LocalDateTime.now());

        if (bid.getBidderType() == Bid.BidderType.COMPANY) {
            DeliveryCompany company = deliveryCompanyRepository.findById(bid.getDeliveryCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Delivery company not found"));
            order.setDeliveryCompany(company);
        }

        if (bid.getDriverId() != null) {
            DriverPerson driver = driverPersonRepository.findById(bid.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
            order.setDriverPerson(driver);
            driver.setIsAvailable(false);
            driverPersonRepository.save(driver);
        }

        orderRepository.save(order);
    }

    private void rejectOtherBids(Long orderId, String acceptedBidId) {
        List<Bid> otherBids = bidRepository.findByOrderIdAndStatus(orderId, Bid.BidStatus.SUBMITTED)
            .stream()
            .filter(bid -> !bid.getBidId().equals(acceptedBidId))
            .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        for (Bid bid : otherBids) {
            bid.setStatus(Bid.BidStatus.REJECTED);
            bid.setResponseMessage("Order assigned to another bidder");
            bid.setRespondedAt(now);
            bidRepository.save(bid);
        }
    }

    private BidRanking calculateBidRanking(Bid bid) {
        BigDecimal score = BigDecimal.ZERO;

        BigDecimal priceScore = BigDecimal.valueOf(100)
            .subtract(bid.getBidAmount().multiply(BigDecimal.valueOf(0.1)));
        score = score.add(priceScore.multiply(BigDecimal.valueOf(0.4)));

        long hoursToDelivery = java.time.Duration.between(LocalDateTime.now(), bid.getEstimatedDeliveryTime()).toHours();
        BigDecimal timeScore = BigDecimal.valueOf(Math.max(0, 100 - hoursToDelivery * 2));
        score = score.add(timeScore.multiply(BigDecimal.valueOf(0.3)));

        DeliveryCompany company = bid.getDeliveryCompanyId() != null
                ? deliveryCompanyRepository.findById(bid.getDeliveryCompanyId()).orElse(null)
                : null;
        DriverPerson driver = bid.getDriverId() != null
                ? driverPersonRepository.findById(bid.getDriverId()).orElse(null)
                : null;
        BigDecimal ratingScore = null;
        if (company != null && company.getRating() != null) {
            ratingScore = company.getRating().multiply(BigDecimal.valueOf(20));
        } else if (driver != null && driver.getRating() != null) {
            ratingScore = driver.getRating().multiply(BigDecimal.valueOf(20));
        }
        if (ratingScore != null) {
            score = score.add(ratingScore.multiply(BigDecimal.valueOf(0.3)));
        }

        return new BidRanking(bid, score, generateRankingReasons(bid, company, driver));
    }

    private List<String> generateRankingReasons(Bid bid, DeliveryCompany company, DriverPerson driver) {
        List<String> reasons = new ArrayList<>();

        if (bid.getBidAmount().compareTo(BigDecimal.valueOf(50)) < 0) {
            reasons.add("Competitive pricing");
        }

        if (bid.getEstimatedDeliveryTime().isBefore(LocalDateTime.now().plusHours(2))) {
            reasons.add("Fast delivery");
        }

        if (company != null && company.getRating() != null &&
            company.getRating().compareTo(BigDecimal.valueOf(4.0)) > 0) {
            reasons.add("High-rated company");
        }

        if (driver != null && driver.getRating() != null &&
            driver.getRating().compareTo(BigDecimal.valueOf(4.0)) > 0) {
            reasons.add("High-rated driver");
        }

        if (bid.getMessage() != null && !bid.getMessage().trim().isEmpty()) {
            reasons.add("Detailed proposal");
        }

        return reasons;
    }

    // Request / response DTOs

    public static class BidRequest {
        private Long orderId;
        private Bid.BidderType bidderType;
        private Long deliveryCompanyId;
        private Long driverId;
        private BigDecimal bidAmount;
        private LocalDateTime estimatedDeliveryTime;
        private String message;

        public BidRequest() {}

        public BidRequest(Long orderId, Bid.BidderType bidderType, Long deliveryCompanyId, Long driverId,
                         BigDecimal bidAmount, LocalDateTime estimatedDeliveryTime, String message) {
            this.orderId = orderId;
            this.bidderType = bidderType;
            this.deliveryCompanyId = deliveryCompanyId;
            this.driverId = driverId;
            this.bidAmount = bidAmount;
            this.estimatedDeliveryTime = estimatedDeliveryTime;
            this.message = message;
        }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public Bid.BidderType getBidderType() { return bidderType; }
        public void setBidderType(Bid.BidderType bidderType) { this.bidderType = bidderType; }

        public Long getDeliveryCompanyId() { return deliveryCompanyId; }
        public void setDeliveryCompanyId(Long deliveryCompanyId) { this.deliveryCompanyId = deliveryCompanyId; }

        public Long getDriverId() { return driverId; }
        public void setDriverId(Long driverId) { this.driverId = driverId; }

        public BigDecimal getBidAmount() { return bidAmount; }
        public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }

        public LocalDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
        public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class BidRanking {
        private final Bid bid;
        private final BigDecimal score;
        private final List<String> reasons;

        public BidRanking(Bid bid, BigDecimal score, List<String> reasons) {
            this.bid = bid;
            this.score = score;
            this.reasons = reasons;
        }

        public Bid getBid() { return bid; }
        public BigDecimal getScore() { return score; }
        public List<String> getReasons() { return reasons; }
    }

    public static class BidStatistics {
        private long bidsSubmitted = 0;
        private long bidsAccepted = 0;
        private long bidsRejected = 0;
        private long bidsWithdrawn = 0;
        private long bidsExpired = 0;

        public void setBidsSubmitted(long value) { this.bidsSubmitted = value; }
        public void setBidsAccepted(long value) { this.bidsAccepted = value; }
        public void setBidsRejected(long value) { this.bidsRejected = value; }
        public void setBidsWithdrawn(long value) { this.bidsWithdrawn = value; }
        public void setBidsExpired(long value) { this.bidsExpired = value; }

        public double getAcceptanceRate() {
            return bidsSubmitted > 0 ? (double) bidsAccepted / bidsSubmitted * 100 : 0;
        }

        public long getBidsSubmitted() { return bidsSubmitted; }
        public long getBidsAccepted() { return bidsAccepted; }
        public long getBidsRejected() { return bidsRejected; }
        public long getBidsWithdrawn() { return bidsWithdrawn; }
        public long getBidsExpired() { return bidsExpired; }
    }

    public static class BiddingActivitySummary {
        private final long totalBids;
        private final long acceptedBids;
        private final BigDecimal totalBidValue;
        private final BigDecimal averageBidAmount;
        private final double acceptanceRate;
        private final Map<Long, Long> bidsByCompany;

        public BiddingActivitySummary(long totalBids, long acceptedBids, BigDecimal totalBidValue,
                                    BigDecimal averageBidAmount, double acceptanceRate,
                                    Map<Long, Long> bidsByCompany) {
            this.totalBids = totalBids;
            this.acceptedBids = acceptedBids;
            this.totalBidValue = totalBidValue;
            this.averageBidAmount = averageBidAmount;
            this.acceptanceRate = acceptanceRate;
            this.bidsByCompany = bidsByCompany;
        }

        public long getTotalBids() { return totalBids; }
        public long getAcceptedBids() { return acceptedBids; }
        public BigDecimal getTotalBidValue() { return totalBidValue; }
        public BigDecimal getAverageBidAmount() { return averageBidAmount; }
        public double getAcceptanceRate() { return acceptanceRate; }
        public Map<Long, Long> getBidsByCompany() { return bidsByCompany; }
    }
}
