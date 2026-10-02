package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.repository.SeatHoldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

@Component
public class SeatHoldCleanupScheduler {

    private final SeatHoldRepository seatHoldRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public SeatHoldCleanupScheduler(SeatHoldRepository seatHoldRepository) {
        this.seatHoldRepository = seatHoldRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredHolds() {
        LOGGER.info("Cleaning up expired holds");
        seatHoldRepository.deleteExpiredHolds(LocalDateTime.now());
    }
}
