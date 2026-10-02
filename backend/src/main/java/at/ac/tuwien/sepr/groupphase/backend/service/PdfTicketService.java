package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;

public interface PdfTicketService {

    /**
     * Generate a pdf of a ticket.
     *
     * @param ticket ticket of which to generate pdf
     * @return byte[] version of a pdf
     * @throws Exception when order isn't found
     */
    byte[] generateTicketPdf(Ticket ticket) throws Exception;

    /**
     * Generate a pdf of all tickets to be sent in the order confirmation email.
     *
     * @param order order from which to pull the tickets
     * @return byte[] version of a pdf
     * @throws Exception when order isn't found
     */
    byte[] generateOrderConfirmationTicketsPdf(Order order) throws Exception;
}
