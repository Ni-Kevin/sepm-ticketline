package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.time.LocalDateTime;

public class ReservationResponseDto {

    private String reservationNumber;
    private LocalDateTime reservedUntil;

    public ReservationResponseDto() {
    }

    public ReservationResponseDto(String reservationNumber, LocalDateTime reservedUntil) {
        this.reservationNumber = reservationNumber;
        this.reservedUntil = reservedUntil;
    }

    public String getReservationNumber() {
        return reservationNumber;
    }

    public void setReservationNumber(String reservationNumber) {
        this.reservationNumber = reservationNumber;
    }

    public LocalDateTime getReservedUntil() {
        return reservedUntil;
    }

    public void setReservedUntil(LocalDateTime reservedUntil) {
        this.reservedUntil = reservedUntil;
    }
}
