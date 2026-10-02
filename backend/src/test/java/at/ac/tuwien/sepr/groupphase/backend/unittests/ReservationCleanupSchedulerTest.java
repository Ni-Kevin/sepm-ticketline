package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.ReservationCleanupScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationCleanupSchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReservationCleanupScheduler scheduler;

    private ApplicationUser user;

    @BeforeEach
    void setUp() {
        user = new ApplicationUser();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setLastName("Doe");
    }

    @Test
    public void cleanUpRemovesExpiredReservationsAndSendsEmail() {
        Reservation expired = new Reservation();
        expired.setId(1L);
        expired.setReservationNumber("RES-EXPIRED");
        expired.setReservedUntil(LocalDateTime.now().minusHours(1));
        expired.setUser(user);

        Reservation active = new Reservation();
        expired.setId(1L);
        active.setReservationNumber("RES-ACTIVE");
        active.setReservedUntil(LocalDateTime.now().plusHours(1));
        active.setUser(user);

        when(reservationRepository.findByReservedUntilBefore(any())).thenReturn(List.of(expired));

        scheduler.cleanUpExpiredReservations();

        assertAll("Behaviour when reservation expired",
            () -> verify(reservationRepository).delete(expired),
            () -> verify(emailService).sendReservationExpiredEmail(eq("test@example.com"), eq("Doe"), eq(expired)),
            () -> verify(reservationRepository, never()).delete(active)
        );
    }

    @Test
    public void cleanUpDoesNothingWhenNoExpiredReservations() {
        when(reservationRepository.findByReservedUntilBefore(any())).thenReturn(List.of());

        scheduler.cleanUpExpiredReservations();

        assertAll("Behaviour when nothing expired",
            () -> verify(reservationRepository, never()).delete(any()),
            () -> verify(emailService, never()).sendReservationExpiredEmail(anyString(), anyString(), any())
        );
    }
}
