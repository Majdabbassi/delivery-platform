package com.upstart.backend.controller;

import com.upstart.backend.entity.Bid;
import com.upstart.backend.entity.DeliveryCompany;
import com.upstart.backend.entity.Order;
import com.upstart.backend.repository.DeliveryCompanyRepository;
import com.upstart.backend.repository.OrderRepository;
import com.upstart.backend.service.BidService;
import com.upstart.backend.service.SecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@Slf4j
public class BidController {

    private final BidService bidService;
    private final SecurityService securityService;
    private final OrderRepository orderRepository;
    private final DeliveryCompanyRepository deliveryCompanyRepository;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Bid> submitBid(@Valid @RequestBody BidService.BidRequest bidRequest) {
        log.info("Submitting bid for order {} by delivery company {}",
                bidRequest.getOrderId(), bidRequest.getDeliveryCompanyId());
        // A delivery owner may only bid for their own delivery company.
        securityService.getOwnedDeliveryCompanyOrThrow(bidRequest.getDeliveryCompanyId());
        Bid bid = bidService.submitBid(bidRequest);
        return new ResponseEntity<>(bid, HttpStatus.CREATED);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<List<Bid>> getBidsForOrder(@PathVariable Long orderId) {
        assertCanManageBidsForOrder(orderId);
        List<Bid> bids = bidService.getBidsForOrder(orderId);
        return ResponseEntity.ok(bids);
    }

    @GetMapping("/order/{orderId}/rankings")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<List<BidService.BidRanking>> getBidRankings(@PathVariable Long orderId) {
        assertCanManageBidsForOrder(orderId);
        List<BidService.BidRanking> rankings = bidService.getBidRankings(orderId);
        return ResponseEntity.ok(rankings);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Page<Bid>> getMyCompanyBids(
            @RequestParam(required = false) Long deliveryCompanyId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long companyId = resolveDeliveryCompanyId(deliveryCompanyId);
        Page<Bid> bids = bidService.getBidsByCompany(companyId, pageable);
        return ResponseEntity.ok(bids);
    }

    @PostMapping("/{bidId}/accept")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Bid> acceptBid(@PathVariable String bidId,
                                         @RequestParam(required = false) String message) {
        Bid bid = bidService.getBidByBidId(bidId);
        assertCanManageBidsForOrder(bid.getOrderId());
        Bid accepted = bidService.acceptBid(bidId, message);
        return ResponseEntity.ok(accepted);
    }

    @PostMapping("/{bidId}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER')")
    public ResponseEntity<Bid> rejectBid(@PathVariable String bidId,
                                         @RequestParam(required = false) String message) {
        Bid bid = bidService.getBidByBidId(bidId);
        assertCanManageBidsForOrder(bid.getOrderId());
        Bid rejected = bidService.rejectBid(bidId, message);
        return ResponseEntity.ok(rejected);
    }

    @PostMapping("/{bidId}/withdraw")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<Bid> withdrawBid(@PathVariable String bidId) {
        Bid bid = bidService.getBidByBidId(bidId);
        // A delivery owner may only withdraw their own company's bid.
        securityService.getOwnedDeliveryCompanyOrThrow(bid.getDeliveryCompanyId());
        Bid withdrawn = bidService.withdrawBid(bidId);
        return ResponseEntity.ok(withdrawn);
    }

    @GetMapping("/my/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<BidService.BidStatistics> getMyBidStats(
            @RequestParam(required = false) Long deliveryCompanyId) {
        Long companyId = resolveDeliveryCompanyId(deliveryCompanyId);
        return ResponseEntity.ok(bidService.getBidStatistics(companyId));
    }

    // Helpers

    private Long resolveDeliveryCompanyId(Long requested) {
        if (securityService.getCurrentRole() == com.upstart.backend.entity.User.Role.SUPER_ADMIN) {
            if (requested == null) {
                throw new IllegalArgumentException("deliveryCompanyId is required for SUPER_ADMIN");
            }
            return requested;
        }
        // Delivery owners operate on their own company only.
        return deliveryCompanyRepository.findByOwnerId(securityService.getCurrentDeliveryOwner().getId())
                .stream()
                .map(DeliveryCompany::getId)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Current user owns no delivery company"));
    }

    private void assertCanManageBidsForOrder(Long orderId) {
        if (securityService.getCurrentRole() == com.upstart.backend.entity.User.Role.SUPER_ADMIN) {
            return;
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        securityService.getOwnedVendorCompanyOrThrow(order.getVendorCompany().getId());
    }
}
