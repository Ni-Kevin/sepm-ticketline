package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummaryTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.MyTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.OrderDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithSeatholdIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithTicketIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;

import java.util.List;


public interface BookingService {

    OrderDto purchase(PurchaseRequestWithSeatholdIdsDto request, Long userId) throws ValidationException;

    ReservationResponseDto reserve(ReservationRequestDto request, Long userId) throws ValidationException;

    /**
     * Cancels reserved tickets for a user.
     *
     * @param ticketId IDs of the reserved tickets to cancel
     * @param userId   ID of the user requesting the cancellation
     * @throws ValidationException if the tickets cannot be cancelled
     */
    void cancelReservedTickets(List<Long> ticketId, Long userId) throws ValidationException;

    /**
     * Cancels purchased tickets for a user.
     *
     * @param ticketIds IDs of the purchased tickets to cancel
     * @param userId    ID of the user requesting the cancellation
     * @throws ValidationException if the tickets cannot be cancelled
     */
    void cancelPurchasedTickets(List<Long> ticketIds, Long userId) throws ValidationException;

    /**
     * Returns a list of all the tickets for a user.
     *
     * @param userId -  ID of the user to get the tickets
     * @return a list with MyTicketDtos
     */
    List<MyTicketDto> getAllTicketsForUser(Long userId);

    /**
     * Returns a checkout summary for selected reserved tickets of a performance.
     *
     * @param performanceId the performance id
     * @param ticketIds     the ticket ids to include
     * @param userId        the user id
     * @return checkout summary DTO
     */
    CheckoutSummaryTicketDto getCheckoutSummaryFromTickets(Long performanceId, List<Long> ticketIds, Long userId) throws ValidationException;

    /**
     * Purchases reserved tickets, converting them from reserved to purchased.
     *
     * @param request purchase request with ticket IDs
     * @param userId  the user id
     * @return the created order
     */
    OrderDto purchaseFromReservation(PurchaseRequestWithTicketIdsDto request, Long userId) throws ValidationException;
}
