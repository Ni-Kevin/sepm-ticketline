package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ReservationRepository reservationRepository;
    private final EmailService emailService;

    public ReservationCleanupScheduler(ReservationRepository reservationRepository, EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanUpExpiredReservations() {
        LOGGER.info("Running expired reservations cleanup...");
        LocalDateTime now = LocalDateTime.now();

        List<Reservation> expiredReservations = reservationRepository.findByReservedUntilBefore(now);

        for (Reservation reservation : expiredReservations) {
            LOGGER.info("Reservation {} has expired. Cleaning up...", reservation.getReservationNumber());

            if (reservation.getUser() != null) {
                emailService.sendReservationExpiredEmail(
                    reservation.getUser().getEmail(),
                    reservation.getUser().getLastName(),
                    reservation
                );
            }

            reservationRepository.delete(reservation);
        }
        LOGGER.info("Cleanup finished. Removed {} expired reservations.", expiredReservations.size());
    }
}
