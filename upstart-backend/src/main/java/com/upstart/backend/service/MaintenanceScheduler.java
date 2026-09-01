package com.upstart.backend.service;

import com.upstart.backend.entity.DriverPerson;
import com.upstart.backend.repository.DriverPersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Runs periodic maintenance jobs: expiring bids, clearing stale pooled orders,
 * expiring partnerships, cleaning token blacklist/audit data, and (when enabled)
 * a driver watchdog that marks stale drivers as unavailable.
 */
@Component
public class MaintenanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceScheduler.class);

    private final BidService bidService;
    private final OrderPoolService orderPoolService;
    private final PartnershipService partnershipService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AssignmentAuditService assignmentAuditService;
    private final DriverPersonRepository driverPersonRepository;

    @Value("${upstart.assignment.driver.watchdog-enabled:false}")
    private boolean driverWatchdogEnabled;

    @Value("${upstart.assignment.driver.inactivity-minutes:30}")
    private long driverInactivityMinutes;

    public MaintenanceScheduler(BidService bidService,
                                OrderPoolService orderPoolService,
                                PartnershipService partnershipService,
                                TokenBlacklistService tokenBlacklistService,
                                AssignmentAuditService assignmentAuditService,
                                DriverPersonRepository driverPersonRepository) {
        this.bidService = bidService;
        this.orderPoolService = orderPoolService;
        this.partnershipService = partnershipService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.assignmentAuditService = assignmentAuditService;
        this.driverPersonRepository = driverPersonRepository;
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000) // hourly, first run after 1 min
    public void expireOldBids() {
        try {
            bidService.expireOldBids();
        } catch (Exception e) {
            logger.error("Failed to expire old bids", e);
        }
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 120000) // hourly
    public void clearExpiredOrders() {
        try {
            orderPoolService.clearExpiredOrders();
        } catch (Exception e) {
            logger.error("Failed to clear expired pooled orders", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // daily at 03:00
    public void updateExpiredPartnerships() {
        try {
            partnershipService.updateExpiredPartnerships();
        } catch (Exception e) {
            logger.error("Failed to update expired partnerships", e);
        }
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 180000) // hourly
    public void cleanupTokenBlacklist() {
        try {
            tokenBlacklistService.cleanupExpiredTokens();
        } catch (Exception e) {
            logger.error("Failed to cleanup expired tokens", e);
        }
    }

    @Scheduled(cron = "0 30 3 * * *") // daily at 03:30
    public void cleanupAuditData() {
        try {
            assignmentAuditService.clearOldAuditData(LocalDateTime.now().minusDays(90));
        } catch (Exception e) {
            logger.error("Failed to cleanup audit data", e);
        }
    }

    @Scheduled(fixedDelay = 600000, initialDelay = 240000) // every 10 minutes
    public void driverWatchdog() {
        if (!driverWatchdogEnabled) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(driverInactivityMinutes);
        List<DriverPerson> staleDrivers = driverPersonRepository.findAll().stream()
                .filter(driver -> Boolean.TRUE.equals(driver.getIsAvailable()))
                .filter(driver -> driver.getLastActive() != null && driver.getLastActive().isBefore(cutoff))
                .toList();
        for (DriverPerson driver : staleDrivers) {
            driver.setIsAvailable(false);
            driverPersonRepository.save(driver);
        }
        if (!staleDrivers.isEmpty()) {
            logger.info("Driver watchdog marked {} stale drivers unavailable", staleDrivers.size());
        }
    }
}
