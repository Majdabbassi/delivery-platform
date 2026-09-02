package com.swiftdeliver.backend.controller;

import com.swiftdeliver.backend.entity.Bid;
import com.swiftdeliver.backend.entity.DeliveryCompany;
import com.swiftdeliver.backend.entity.Order;
import com.swiftdeliver.backend.repository.DeliveryCompanyRepository;
import com.swiftdeliver.backend.repository.OrderRepository;
import com.swiftdeliver.backend.service.BidService;
import com.swiftdeliver.backend.service.SecurityService;
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
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER') or hasRole('DRIVER')")
    public ResponseEntity<Bid> submitBid(@Valid @RequestBody BidService.BidRequest bidRequest) {
        log.info("Submitting bid for order {} by {}",
                bidRequest.getOrderId(), bidRequest.getBidderType());
        if (bidRequest.getBidderType() == Bid.BidderType.INDEPENDENT_DRIVER) {
            if (securityService.getCurrentRole() != com.swiftdeliver.backend.entity.User.Role.DRIVER) {
                throw new AccessDeniedException("Only drivers may submit independent driver bids");
            }
            com.swiftdeliver.backend.entity.DriverPerson currentDriver = securityService.getCurrentDriverPerson();
            if (currentDriver.getDeliveryCompany() != null) {
                throw new AccessDeniedException(
                        "Drivers employed by a delivery company cannot bid directly. "
                        + "Only the delivery company owner may bid on their behalf.");
            }
            Long currentDriverId = currentDriver.getId();
            if (bidRequest.getDriverId() != null && !currentDriverId.equals(bidRequest.getDriverId())) {
                throw new AccessDeniedException("Drivers may only bid on their own behalf");
            }
            bidRequest.setDriverId(currentDriverId);
        } else {
            if (securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.DRIVER) {
                throw new AccessDeniedException("Drivers cannot bid on behalf of a delivery company");
            }
            securityService.getOwnedDeliveryCompanyOrThrow(bidRequest.getDeliveryCompanyId());
        }
        Bid bid = bidService.submitBid(bidRequest);
        return new ResponseEntity<>(bid, HttpStatus.CREATED);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('CLIENT')")
    public ResponseEntity<List<Bid>> getBidsForOrder(@PathVariable Long orderId) {
        assertCanManageBidsForOrder(orderId);
        List<Bid> bids = bidService.getBidsForOrder(orderId);
        return ResponseEntity.ok(bids);
    }

    @GetMapping("/order/{orderId}/rankings")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('CLIENT')")
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
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('CLIENT')")
    public ResponseEntity<Bid> acceptBid(@PathVariable String bidId,
                                         @RequestParam(required = false) String message) {
        Bid bid = bidService.getBidByBidId(bidId);
        assertCanManageBidsForOrder(bid.getOrderId());
        Bid accepted = bidService.acceptBid(bidId, message);
        return ResponseEntity.ok(accepted);
    }

    @PostMapping("/{bidId}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('VENDOR_OWNER') or hasRole('CLIENT')")
    public ResponseEntity<Bid> rejectBid(@PathVariable String bidId,
                                         @RequestParam(required = false) String message) {
        Bid bid = bidService.getBidByBidId(bidId);
        assertCanManageBidsForOrder(bid.getOrderId());
        Bid rejected = bidService.rejectBid(bidId, message);
        return ResponseEntity.ok(rejected);
    }

    @PostMapping("/{bidId}/withdraw")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER') or hasRole('DRIVER')")
    public ResponseEntity<Bid> withdrawBid(@PathVariable String bidId) {
        Bid bid = bidService.getBidByBidId(bidId);
        if (bid.getBidderType() == Bid.BidderType.INDEPENDENT_DRIVER) {
            // A driver may only withdraw their own bid.
            if (securityService.getCurrentRole() != com.swiftdeliver.backend.entity.User.Role.SUPER_ADMIN
                    && securityService.getCurrentRole() != com.swiftdeliver.backend.entity.User.Role.DRIVER) {
                throw new AccessDeniedException("Only the bidding driver may withdraw this bid");
            }
            if (securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.DRIVER
                    && !securityService.getCurrentDriverPerson().getId().equals(bid.getDriverId())) {
                throw new AccessDeniedException("You may only withdraw your own bids");
            }
        } else {
            // A delivery owner may only withdraw their own company's bid.
            if (securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.SUPER_ADMIN) {
                // allowed
            } else {
                securityService.getOwnedDeliveryCompanyOrThrow(bid.getDeliveryCompanyId());
            }
        }
        Bid withdrawn = bidService.withdrawBid(bidId);
        return ResponseEntity.ok(withdrawn);
    }

    @GetMapping("/my/driver")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DRIVER')")
    public ResponseEntity<Page<Bid>> getMyDriverBids(
            @RequestParam(required = false) Long driverId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long resolvedDriverId;
        if (securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.SUPER_ADMIN) {
            if (driverId == null) {
                throw new IllegalArgumentException("driverId is required for SUPER_ADMIN");
            }
            resolvedDriverId = driverId;
        } else {
            resolvedDriverId = securityService.getCurrentDriverPerson().getId();
        }
        Page<Bid> bids = bidService.getBidsByDriver(resolvedDriverId, pageable);
        return ResponseEntity.ok(bids);
    }

    @GetMapping("/my/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DELIVERY_OWNER')")
    public ResponseEntity<BidService.BidStatistics> getMyBidStats(
            @RequestParam(required = false) Long deliveryCompanyId) {
        Long companyId = resolveDeliveryCompanyId(deliveryCompanyId);
        return ResponseEntity.ok(bidService.getBidStatistics(companyId));
    }

    @GetMapping("/my/stats/driver")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DRIVER')")
    public ResponseEntity<BidService.BidStatistics> getMyDriverBidStats(
            @RequestParam(required = false) Long driverId) {
        Long resolvedDriverId;
        if (securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.SUPER_ADMIN) {
            if (driverId == null) {
                throw new IllegalArgumentException("driverId is required for SUPER_ADMIN");
            }
            resolvedDriverId = driverId;
        } else {
            resolvedDriverId = securityService.getCurrentDriverPerson().getId();
        }
        return ResponseEntity.ok(bidService.getDriverBidStatistics(resolvedDriverId));
    }

    // Helpers

    private Long resolveDeliveryCompanyId(Long requested) {
        if (securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.SUPER_ADMIN) {
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
        if (securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.SUPER_ADMIN) {
            return;
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getVendorCompany() != null) {
            securityService.getOwnedVendorCompanyOrThrow(order.getVendorCompany().getId());
            return;
        }
        if (order.getCustomerUser() != null
                && securityService.getCurrentRole() == com.swiftdeliver.backend.entity.User.Role.CLIENT
                && securityService.getCurrentUser().getId().equals(order.getCustomerUser().getId())) {
            return; // the sender (client) may award bids on their own general-delivery order
        }
        throw new AccessDeniedException("You do not have permission to manage bids for this order");
    }
}
