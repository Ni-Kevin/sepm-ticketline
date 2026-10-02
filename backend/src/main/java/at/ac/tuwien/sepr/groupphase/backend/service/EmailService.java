package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;

import java.util.List;


public interface EmailService {

    void sendPasswordResetEmail(String recipientEmail, String resetLink);

    void sendOrderConfirmationEmail(String recipientEmail, String name, Order order);

    void sendInitialPasswordEmail(String recipientEmail, String name, String password);

    void sendReservationConfirmationEmail(String recipientEmail, String name, Reservation reservation);

    void sendReservationExpiredEmail(String recipientEmail, String name, Reservation reservation);

    void sendCancellationEmail(String recipientMail, String name, Ticket cancelledTicket);

    void sendPurchaseCancellationEmail(String recipientMail, String name, Order order, List<Ticket> cancelledTickets);
}
