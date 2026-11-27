package com.bazaarhub.backend.inventory.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bazaarhub.inventory.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final StockReservationService stockReservationService;

    public ReservationExpiryScheduler(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @Scheduled(fixedDelayString = "${bazaarhub.inventory.reservation-expiry.interval-ms:60000}",
               initialDelayString = "${bazaarhub.inventory.reservation-expiry.initial-delay-ms:10000}")
    public void sweepExpiredReservations() {
        try {
            int expiredCount = stockReservationService.expirePendingReservations();
            if (expiredCount > 0) {
                log.info("[RESERVATION SCHEDULER] Released and expired {} pending checkout reservations", expiredCount);
            }
        } catch (Exception ex) {
            log.error("[RESERVATION SCHEDULER] Error during periodic reservation expiry check: {}", ex.getMessage());
        }
    }
}
