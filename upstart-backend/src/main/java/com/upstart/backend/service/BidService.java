package com.upstart.backend.service;

import com.upstart.backend.entity.Order;
import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.DriverPerson;
import com.upstart.backend.repository.OrderRepository;
import com.upstart.backend.repository.DeliveryCompanyRepository;
import com.upstart.backend.repository.DriverPersonRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for managing bidding functionality for orders
 * Handles bid creation, evaluation, acceptance, and rejection
 */
@Service
@Transactional
public class BidService {

    private static final Logger logger = LoggerFactory.getLogger(BidService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryCompanyRepository deliveryCompanyRepository;

    @Autowired
    private DriverPersonRepository driverPersonRepository;



    @Autowired
    private OrderPoolService orderPoolService;

    // In-memory storage for bids (in production, use database)
    private final Map<Long, List<Bid>> orderBids = new ConcurrentHashMap<>();
    private final Map<String, Bid> bidStorage = new ConcurrentHashMap<>();
    private final Map<Long, BidStatistics> companyBidStats = new ConcurrentHashMap<>();

    /**
     * Submit a bid for an order
     */
    public Bid submitBid(BidRequest bidRequest) {
        // Validate bid request
        validateBidRequest(bidRequest);

        Order order = orderRepository.findById(bidRequest.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        DeliveryCompany company = deliveryCompanyRepository.findById(bidRequest.getDeliveryCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("Delivery company not found"));

        // Check if order is still available for bidding
        if (!isOrderAvailableForBidding(order)) {
            throw new IllegalStateException("Order is no longer available for bidding");
        }

        // Check if company can bid on this order
        if (!canCompanyBidOnOrder(company, order)) {
            throw new IllegalStateException("Company is not eligible to bid on this order");
        }

        // Create bid
        Bid bid = new Bid(
            generateBidId(),
            bidRequest.getOrderId(),
            bidRequest.getDeliveryCompanyId(),
            bidRequest.getDriverId(),
            bidRequest.getBidAmount(),
            bidRequest.getEstimatedDeliveryTime(),
            bidRequest.getMessage(),
            BidStatus.SUBMITTED,
            LocalDateTime.now(),
            null,
            null
        );

        // Store bid
        storeBid(bid);
        updateBidStatistics(company.getId(), "bids_submitted", 1);

        logger.info("Bid {} submitted by company {} for order {} with amount {}", 
                   bid.getBidId(), company.getId(), order.getId(), bid.getBidAmount());

        return bid;
    }

    /**
     * Get all bids for an order
     */
    public List<Bid> getBidsForOrder(Long orderId) {
        return orderBids.getOrDefault(orderId, new ArrayList<>())
            .stream()
            .sorted(Comparator.comparing(Bid::getSubmittedAt).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get bids submitted by a delivery company
     */
    public Page<Bid> getBidsByCompany(Long deliveryCompanyId, Pageable pageable) {
        List<Bid> companyBids = bidStorage.values().stream()
            .filter(bid -> bid.getDeliveryCompanyId().equals(deliveryCompanyId))
            .sorted(Comparator.comparing(Bid::getSubmittedAt).reversed())
            .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), companyBids.size());
        
        List<Bid> pageContent = start < companyBids.size() ? 
            companyBids.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, companyBids.size());
    }

    /**
     * Accept a bid
     */
    public Bid acceptBid(String bidId, String acceptanceMessage) {
        Bid bid = bidStorage.get(bidId);
        if (bid == null) {
            throw new IllegalArgumentException("Bid not found");
        }

        if (bid.getStatus() != BidStatus.SUBMITTED) {
            throw new IllegalStateException("Bid is not in submitted status");
        }

        Order order = orderRepository.findById(bid.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!isOrderAvailableForBidding(order)) {
            throw new IllegalStateException("Order is no longer available for bidding");
        }

        // Update bid status
        bid.setStatus(BidStatus.ACCEPTED);
        bid.setResponseMessage(acceptanceMessage);
        bid.setRespondedAt(LocalDateTime.now());

        // Assign order to delivery company
        assignOrderToBid(order, bid);

        // Reject all other bids for this order
        rejectOtherBids(bid.getOrderId(), bidId);

        // Remove order from pools
        orderPoolService.removeOrderFromPools(order.getId());

        updateBidStatistics(bid.getDeliveryCompanyId(), "bids_accepted", 1);

        logger.info("Bid {} accepted for order {}", bidId, order.getId());

        return bid;
    }

    /**
     * Reject a bid
     */
    public Bid rejectBid(String bidId, String rejectionMessage) {
        Bid bid = bidStorage.get(bidId);
        if (bid == null) {
            throw new IllegalArgumentException("Bid not found");
        }

        if (bid.getStatus() != BidStatus.SUBMITTED) {
            throw new IllegalStateException("Bid is not in submitted status");
        }

        bid.setStatus(BidStatus.REJECTED);
        bid.setResponseMessage(rejectionMessage);
        bid.setRespondedAt(LocalDateTime.now());

        updateBidStatistics(bid.getDeliveryCompanyId(), "bids_rejected", 1);

        logger.info("Bid {} rejected for order {}", bidId, bid.getOrderId());

        return bid;
    }

    /**
     * Withdraw a bid (by delivery company)
     */
    public Bid withdrawBid(String bidId) {
        Bid bid = bidStorage.get(bidId);
        if (bid == null) {
            throw new IllegalArgumentException("Bid not found");
        }

        if (bid.getStatus() != BidStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted bids can be withdrawn");
        }

        bid.setStatus(BidStatus.WITHDRAWN);
        bid.setRespondedAt(LocalDateTime.now());

        updateBidStatistics(bid.getDeliveryCompanyId(), "bids_withdrawn", 1);

        logger.info("Bid {} withdrawn by company {}", bidId, bid.getDeliveryCompanyId());

        return bid;
    }

    /**
     * Get bid statistics for a delivery company
     */
    public BidStatistics getBidStatistics(Long deliveryCompanyId) {
        return companyBidStats.getOrDefault(deliveryCompanyId, new BidStatistics());
    }

    /**
     * Get bid rankings for an order
     */
    public List<BidRanking> getBidRankings(Long orderId) {
        List<Bid> bids = getBidsForOrder(orderId);
        
        return bids.stream()
            .filter(bid -> bid.getStatus() == BidStatus.SUBMITTED)
            .map(this::calculateBidRanking)
            .sorted(Comparator.comparing(BidRanking::getScore).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Auto-expire old bids
     */
    public void expireOldBids() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        List<Bid> expiredBids = bidStorage.values().stream()
            .filter(bid -> bid.getStatus() == BidStatus.SUBMITTED)
            .filter(bid -> bid.getSubmittedAt().isBefore(cutoff))
            .collect(Collectors.toList());
        
        for (Bid bid : expiredBids) {
            bid.setStatus(BidStatus.EXPIRED);
            bid.setRespondedAt(LocalDateTime.now());
            updateBidStatistics(bid.getDeliveryCompanyId(), "bids_expired", 1);
        }
        
        logger.info("Expired {} old bids", expiredBids.size());
    }

    /**
     * Get bidding activity summary
     */
    public BiddingActivitySummary getBiddingActivitySummary(LocalDateTime from, LocalDateTime to) {
        List<Bid> bidsInPeriod = bidStorage.values().stream()
            .filter(bid -> bid.getSubmittedAt().isAfter(from) && bid.getSubmittedAt().isBefore(to))
            .collect(Collectors.toList());
        
        long totalBids = bidsInPeriod.size();
        long acceptedBids = bidsInPeriod.stream()
            .mapToLong(bid -> bid.getStatus() == BidStatus.ACCEPTED ? 1 : 0)
            .sum();
        
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

    private void validateBidRequest(BidRequest request) {
        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (request.getDeliveryCompanyId() == null) {
            throw new IllegalArgumentException("Delivery company ID is required");
        }
        if (request.getBidAmount() == null || request.getBidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid bid amount is required");
        }
        if (request.getEstimatedDeliveryTime() == null) {
            throw new IllegalArgumentException("Estimated delivery time is required");
        }
    }

    private boolean isOrderAvailableForBidding(Order order) {
        return order.getStatus() == Order.OrderStatus.PENDING && 
               order.getDeliveryCompany() == null;
    }

    private boolean canCompanyBidOnOrder(DeliveryCompany company, Order order) {
        // Check if company is active and licensed
        if (!company.getIsActive() || !company.getIsLicensed()) {
            return false;
        }
        
        // Check if company hasn't already bid on this order
        List<Bid> existingBids = orderBids.getOrDefault(order.getId(), new ArrayList<>());
        boolean alreadyBid = existingBids.stream()
            .anyMatch(bid -> bid.getDeliveryCompanyId().equals(company.getId()) && 
                           bid.getStatus() == BidStatus.SUBMITTED);
        
        return !alreadyBid;
    }

    private String generateBidId() {
        return "BID_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void storeBid(Bid bid) {
        bidStorage.put(bid.getBidId(), bid);
        orderBids.computeIfAbsent(bid.getOrderId(), k -> new CopyOnWriteArrayList<>())
                 .add(bid);
    }

    private void assignOrderToBid(Order order, Bid bid) {
        DeliveryCompany company = deliveryCompanyRepository.findById(bid.getDeliveryCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("Delivery company not found"));
        
        order.setDeliveryCompany(company);
        order.setStatus(Order.OrderStatus.ASSIGNED);
        order.setDeliveryFee(bid.getBidAmount());
        
        if (bid.getDriverId() != null) {
            DriverPerson driver = driverPersonRepository.findById(bid.getDriverId()).orElse(null);
            if (driver != null) {
                order.setDriverPerson(driver);
            }
        }
        
        orderRepository.save(order);
    }

    private void rejectOtherBids(Long orderId, String acceptedBidId) {
        List<Bid> otherBids = orderBids.getOrDefault(orderId, new ArrayList<>())
            .stream()
            .filter(bid -> !bid.getBidId().equals(acceptedBidId) && 
                          bid.getStatus() == BidStatus.SUBMITTED)
            .collect(Collectors.toList());
        
        for (Bid bid : otherBids) {
            bid.setStatus(BidStatus.REJECTED);
            bid.setResponseMessage("Order assigned to another bidder");
            bid.setRespondedAt(LocalDateTime.now());
            updateBidStatistics(bid.getDeliveryCompanyId(), "bids_rejected", 1);
        }
    }

    private BidRanking calculateBidRanking(Bid bid) {
        // Simple scoring algorithm - can be enhanced
        BigDecimal score = BigDecimal.ZERO;
        
        // Price score (lower is better)
        BigDecimal priceScore = BigDecimal.valueOf(100)
            .subtract(bid.getBidAmount().multiply(BigDecimal.valueOf(0.1)));
        score = score.add(priceScore.multiply(BigDecimal.valueOf(0.4)));
        
        // Time score (faster is better)
        long hoursToDelivery = java.time.Duration.between(LocalDateTime.now(), bid.getEstimatedDeliveryTime()).toHours();
        BigDecimal timeScore = BigDecimal.valueOf(Math.max(0, 100 - hoursToDelivery * 2));
        score = score.add(timeScore.multiply(BigDecimal.valueOf(0.3)));
        
        // Company rating score
        DeliveryCompany company = deliveryCompanyRepository.findById(bid.getDeliveryCompanyId()).orElse(null);
        if (company != null && company.getRating() != null) {
            BigDecimal ratingScore = company.getRating().multiply(BigDecimal.valueOf(20));
            score = score.add(ratingScore.multiply(BigDecimal.valueOf(0.3)));
        }
        
        return new BidRanking(bid, score, generateRankingReasons(bid, company));
    }

    private List<String> generateRankingReasons(Bid bid, DeliveryCompany company) {
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
        
        if (bid.getMessage() != null && !bid.getMessage().trim().isEmpty()) {
            reasons.add("Detailed proposal");
        }
        
        return reasons;
    }

    private void updateBidStatistics(Long companyId, String metric, long value) {
        BidStatistics stats = companyBidStats.computeIfAbsent(companyId, k -> new BidStatistics());
        
        switch (metric) {
            case "bids_submitted":
                stats.incrementBidsSubmitted(value);
                break;
            case "bids_accepted":
                stats.incrementBidsAccepted(value);
                break;
            case "bids_rejected":
                stats.incrementBidsRejected(value);
                break;
            case "bids_withdrawn":
                stats.incrementBidsWithdrawn(value);
                break;
            case "bids_expired":
                stats.incrementBidsExpired(value);
                break;
        }
    }

    // Inner classes and enums

    public enum BidStatus {
        SUBMITTED, ACCEPTED, REJECTED, WITHDRAWN, EXPIRED
    }

    public static class BidRequest {
        private Long orderId;
        private Long deliveryCompanyId;
        private Long driverId;
        private BigDecimal bidAmount;
        private LocalDateTime estimatedDeliveryTime;
        private String message;

        // Constructors
        public BidRequest() {}

        public BidRequest(Long orderId, Long deliveryCompanyId, Long driverId,
                         BigDecimal bidAmount, LocalDateTime estimatedDeliveryTime, String message) {
            this.orderId = orderId;
            this.deliveryCompanyId = deliveryCompanyId;
            this.driverId = driverId;
            this.bidAmount = bidAmount;
            this.estimatedDeliveryTime = estimatedDeliveryTime;
            this.message = message;
        }

        // Getters and Setters
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

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

    public static class Bid {
        private String bidId;
        private Long orderId;
        private Long deliveryCompanyId;
        private Long driverId;
        private BigDecimal bidAmount;
        private LocalDateTime estimatedDeliveryTime;
        private String message;
        private BidStatus status;
        private LocalDateTime submittedAt;
        private LocalDateTime respondedAt;
        private String responseMessage;

        // Constructor
        public Bid(String bidId, Long orderId, Long deliveryCompanyId, Long driverId,
                  BigDecimal bidAmount, LocalDateTime estimatedDeliveryTime, String message,
                  BidStatus status, LocalDateTime submittedAt, LocalDateTime respondedAt,
                  String responseMessage) {
            this.bidId = bidId;
            this.orderId = orderId;
            this.deliveryCompanyId = deliveryCompanyId;
            this.driverId = driverId;
            this.bidAmount = bidAmount;
            this.estimatedDeliveryTime = estimatedDeliveryTime;
            this.message = message;
            this.status = status;
            this.submittedAt = submittedAt;
            this.respondedAt = respondedAt;
            this.responseMessage = responseMessage;
        }

        // Getters and Setters
        public String getBidId() { return bidId; }
        public void setBidId(String bidId) { this.bidId = bidId; }

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

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

        public BidStatus getStatus() { return status; }
        public void setStatus(BidStatus status) { this.status = status; }

        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

        public LocalDateTime getRespondedAt() { return respondedAt; }
        public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

        public String getResponseMessage() { return responseMessage; }
        public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
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

        // Getters
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

        public void incrementBidsSubmitted(long count) { this.bidsSubmitted += count; }
        public void incrementBidsAccepted(long count) { this.bidsAccepted += count; }
        public void incrementBidsRejected(long count) { this.bidsRejected += count; }
        public void incrementBidsWithdrawn(long count) { this.bidsWithdrawn += count; }
        public void incrementBidsExpired(long count) { this.bidsExpired += count; }

        public double getAcceptanceRate() {
            return bidsSubmitted > 0 ? (double) bidsAccepted / bidsSubmitted * 100 : 0;
        }

        // Getters
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

        // Getters
        public long getTotalBids() { return totalBids; }
        public long getAcceptedBids() { return acceptedBids; }
        public BigDecimal getTotalBidValue() { return totalBidValue; }
        public BigDecimal getAverageBidAmount() { return averageBidAmount; }
        public double getAcceptanceRate() { return acceptanceRate; }
        public Map<Long, Long> getBidsByCompany() { return bidsByCompany; }
    }
}