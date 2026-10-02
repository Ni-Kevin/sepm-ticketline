package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceType;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfInvoiceService;
import at.ac.tuwien.sepr.groupphase.backend.service.PdfTicketService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ExecutorService emailExecutor = Executors.newCachedThreadPool();

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final PdfTicketService pdfTicketService;
    private final PdfInvoiceService pdfInvoiceService;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${app.password-reset.from-email}") String fromEmail,
                            PdfTicketService pdfTicketService,
                            PdfInvoiceService pdfInvoiceService) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.pdfTicketService = pdfTicketService;
        this.pdfInvoiceService = pdfInvoiceService;
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject("Reset your password");
        message.setText("A password reset was requested for your account.\n\n"
            + "Use the following link to set a new password:\n"
            + resetLink + "\n\n"
            + "If you did not request this, you can ignore this email.");

        LOGGER.info("Sending password reset email to {}", recipientEmail);
        sendAsync(message);
    }

    @Override
    public void sendOrderConfirmationEmail(String recipientEmail, String name, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Order Confirmation");

            String ticketList = buildTicketList(order != null ? order.getTickets() : null);

            String emailText = "Dear " + name + ",\n\n"
                + "Thank you for your purchase! Your order was successful.\n\n"
                + "Here is an overview of your purchased tickets:\n"
                + ticketList + "\n"
                + "Please note: Your official PDF invoice and PDF tickets are now ready for download. "
                + "You can access and download them at any time directly from your user profile on our platform.\n\n"
                + "We hope you enjoy the event!\n\n"
                + "Best regards,\n"
                + "Your Ticketline Team";

            helper.setText(emailText);

            if (order != null && order.getTickets() != null && !order.getTickets().isEmpty()) {
                byte[] pdf = pdfTicketService.generateOrderConfirmationTicketsPdf(order);
                helper.addAttachment(
                    "tickets-order-" + order.getId() + ".pdf",
                    new ByteArrayResource(pdf),
                    "application/pdf"
                );
            }

            if (order != null && order.getInvoices() != null && !order.getInvoices().isEmpty()) {
                Invoice inv = order.getInvoices().getFirst();
                try {
                    helper.addAttachment("invoice-" + inv.getInvoiceNumber() + ".pdf",
                        new ByteArrayResource(pdfInvoiceService.generateInvoicePdf(inv)), "application/pdf");
                } catch (Exception e) {
                    LOGGER.error("Failed to attach invoice for order #{}", order.getId(), e);
                }
            }

            LOGGER.info("Sending order confirmation email for order #{} to {}", order != null ? order.getId() : "null", recipientEmail);
            sendAsync(message);
        } catch (Exception e) {
            LOGGER.error("Failed to send order confirmation email for order #{} to {}", order != null ? order.getId() : "null", recipientEmail, e);
        }
    }

    @Override
    public void sendReservationConfirmationEmail(String recipientEmail, String name, Reservation reservation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipientEmail);

        message.setSubject("Order Confirmation");

        String ticketList = buildTicketList(reservation != null ? reservation.getTickets() : null);

        String emailText = "Dear " + name + ",\n\n"
            + "Thank you for your reservation! Your reservation was successful.\n\n"
            + "Reservation Number: " + (reservation != null ? reservation.getReservationNumber() : "N/A") + "\n"
            + "Valid until: " + (reservation != null ? reservation.getReservedUntil() : "N/A") + "\n\n"
            + "Here is an overview of your reserved tickets:\n"
            + ticketList + "\n"
            + "Please make sure to collect your tickets at least 30 minutes before the performance starts, otherwise the reservation will be invalid.\n\n"
            + "We hope you enjoy the event!\n\n"
            + "Best regards,\n"
            + "Your Ticketline Team";

        message.setText(emailText);

        LOGGER.info("Sending reservation confirmation email for reservation #{} to {}", reservation != null ? reservation.getReservationNumber() : "null", recipientEmail);
        sendAsync(message);
    }

    @Override
    public void sendReservationExpiredEmail(String recipientEmail, String name, Reservation reservation) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject("Reservation Expired");

        String ticketList = buildTicketList(reservation != null ? reservation.getTickets() : null);

        String emailText = "Dear " + name + ",\n\n"
            + "Unfortunately, your reservation " + (reservation != null ? reservation.getReservationNumber() : "N/A") + " has expired.\n\n"
            + "The following tickets have been released back into the system:\n"
            + ticketList + "\n"
            + "If you still wish to attend the event, please try to book the tickets again.\n\n"
            + "Best regards,\n"
            + "Your Ticketline Team";

        message.setText(emailText);

        LOGGER.info("Sending reservation expired email for reservation #{} to {}", reservation != null ? reservation.getReservationNumber() : "null", recipientEmail);
        sendAsync(message);
    }

    private void sendAsync(SimpleMailMessage message) {
        emailExecutor.execute(() -> {
            try {
                mailSender.send(message);
            } catch (Exception e) {
                LOGGER.error("Failed to send email asynchronously", e);
            }
        });
    }

    private void sendAsync(MimeMessage message) {
        emailExecutor.execute(() -> {
            try {
                mailSender.send(message);
            } catch (Exception e) {
                LOGGER.error("Failed to send email asynchronously", e);
            }
        });
    }

    private String buildTicketList(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return "No tickets found.";
        }

        StringBuilder ticketListBuilder = new StringBuilder();
        Map<Long, Integer> standingCounts = new HashMap<>();
        Map<Long, Ticket> standingSampleTickets = new HashMap<>();

        for (Ticket ticket : tickets) {
            if (ticket.getSeat() != null) {
                ticketListBuilder.append("- 1 x ");

                if (ticket.getPerformance() != null && ticket.getPerformance().getEvent() != null) {
                    ticketListBuilder.append("Event: ").append(ticket.getPerformance().getEvent().getTitle()).append("; ")
                        .append("Performance: ").append(ticket.getPerformance().getPerformanceName()).append(": ");
                }

                if (ticket.getSector() != null) {
                    ticketListBuilder.append(ticket.getSector().getName());
                }

                ticketListBuilder.append(" (Row ")
                    .append(ticket.getSeat().getRowNumber())
                    .append(", Seat ")
                    .append(ticket.getSeat().getSeatNumber())
                    .append(")\n");
            } else {
                if (ticket.getSector() != null) {
                    Long sectorId = ticket.getSector().getId();
                    standingCounts.put(sectorId, standingCounts.getOrDefault(sectorId, 0) + 1);
                    standingSampleTickets.put(sectorId, ticket);
                }
            }
        }

        for (Map.Entry<Long, Integer> entry : standingCounts.entrySet()) {
            Long sectorId = entry.getKey();
            int count = entry.getValue();
            Ticket sample = standingSampleTickets.get(sectorId);

            ticketListBuilder.append("- ").append(count).append(" x ");

            if (sample.getPerformance() != null && sample.getPerformance().getEvent() != null) {
                ticketListBuilder.append(sample.getPerformance().getEvent().getTitle()).append(": ");
            }

            if (sample.getSector() != null) {
                ticketListBuilder.append(sample.getSector().getName());
            }

            ticketListBuilder.append(" (Standing Area)\n");
        }

        return ticketListBuilder.toString();
    }

    @Override
    public void sendInitialPasswordEmail(String recipientEmail, String name, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject("Your initial password");
        message.setText("Dear " + name + ",\n\n"
            + "An account was created for you.\n\n"
            + "In this Mail you will find your automatically generated initial password.\n\n"
            + password + "\n\n"
            + "Please change your password immediately after your first login!\n\n"
            + "We hope you enjoy our service!\n\n"
            + "Best regards,\n"
            + "Your Ticketline Team");

        LOGGER.info("Sending initial password email to {}", recipientEmail);
        sendAsync(message);
    }

    @Override
    public void sendCancellationEmail(String recipientMail, String name, Ticket cancelledTicket) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipientMail);
            helper.setSubject("Cancellation Confirmation");

            if (cancelledTicket != null) {
                String eventTitle = (cancelledTicket.getPerformance() != null && cancelledTicket.getPerformance().getEvent() != null)
                    ? cancelledTicket.getPerformance().getEvent().getTitle()
                    : "Unknown Event";

                String performanceName = cancelledTicket.getPerformance() != null
                    ? cancelledTicket.getPerformance().getPerformanceName()
                    : "Unknown Performance";

                String sectorName = cancelledTicket.getSector() != null
                    ? cancelledTicket.getSector().getName()
                    : "Unknown Sector";

                String seatInfo;
                if (cancelledTicket.getSeat() != null) {
                    seatInfo = "Row " + cancelledTicket.getSeat().getRowNumber()
                        + ", Seat " + cancelledTicket.getSeat().getSeatNumber();
                } else {
                    seatInfo = "Standing Area";
                }

                String ticketDetails = "Event: " + eventTitle + "\n"
                    + "  Performance: " + performanceName + "\n"
                    + "  Sector: " + sectorName + "\n"
                    + "  Seat: " + seatInfo + "\n";

                String emailText = "Dear " + name + ",\n\n"
                    + "We are sorry to see you go! Your cancellation was successful.\n\n"
                    + "Here is an overview of your cancelled ticket:\n"
                    + ticketDetails + "\n"
                    + "Best regards,\n"
                    + "Your Ticketline Team";

                helper.setText(emailText);
            }

            LOGGER.info("Sending cancellation confirmation email for ticket #{} to {}", cancelledTicket.getId(), recipientMail);
            sendAsync(message);
        } catch (Exception e) {
            LOGGER.error("Failed to send cancellation email for ticket #{} to {}", cancelledTicket.getId(), recipientMail, e);
        }
    }

    @Override
    public void sendPurchaseCancellationEmail(String recipientMail, String name, Order order, List<Ticket> cancelledTickets) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipientMail);
            helper.setSubject("Purchase Cancellation Confirmation");

            String emailText = "Dear " + name + ",\n\n"
                + "Your ticket cancellation was successful. The selected tickets were returned and can be booked again.\n\n"
                + "Cancelled tickets:\n"
                + buildTicketList(cancelledTickets) + "\n"
                + "Best regards,\n"
                + "Your Ticketline Team";

            helper.setText(emailText);

            if (order != null && order.getInvoices() != null && !order.getInvoices().isEmpty()) {
                for (Invoice inv : order.getInvoices()) {
                    if (inv.getType() == InvoiceType.CANCELLED) {
                        try {
                            helper.addAttachment("cancellation-invoice-" + inv.getInvoiceNumber() + ".pdf",
                                new ByteArrayResource(pdfInvoiceService.generateCancellationInvoicePdf(inv)), "application/pdf");
                        } catch (Exception e) {
                            LOGGER.error("Failed to attach cancellation invoice for order #{}", order.getId(), e);
                        }
                        break;
                    }
                }
            }

            LOGGER.info("Sending purchase cancellation email for order #{} to {}", order != null ? order.getId() : "null", recipientMail);
            sendAsync(message);
        } catch (Exception e) {
            LOGGER.error("Failed to send purchase cancellation email for order #{} to {}", order != null ? order.getId() : "null", recipientMail, e);
        }
    }
}
