package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CancelPurchaseRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.MyTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.OrderDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithSeatholdIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithTicketIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummaryTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.service.BookingService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * REST endpoint for booking and purchase operations.
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final BookingService bookingService;
    private final UserService userService;

    public BookingEndpoint(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    /**
     * Finalizes the purchase of held seats/sectors.
     *
     * @param request purchase request data
     * @param auth    authentication of the current user
     * @return the created order as a DTO
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/purchases")
    @Operation(summary = "Purchase held seats", security = @SecurityRequirement(name = "apiKey"))
    public OrderDto purchase(@Valid @RequestBody PurchaseRequestWithSeatholdIdsDto request, Authentication auth) throws ValidationException {
        LOGGER.info("POST /api/v1/bookings/purchases body: {}", request);

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());

        return bookingService.purchase(request, user.getId());
    }

    /**
     * Finalizes the reservation of held seats/sectors.
     *
     * @param request reserve request data
     * @param auth    authentication of the current user
     * @return the created reservation as a DTO
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/reservations")
    @Operation(summary = "Reserve held seats", security = @SecurityRequirement(name = "apiKey"))
    public ReservationResponseDto reserve(@Valid @RequestBody ReservationRequestDto request, Authentication auth) throws ValidationException {
        LOGGER.info("POST /api/v1/bookings/reservations body: {}", request);

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());

        return bookingService.reserve(request, user.getId());
    }


    /**
     * Gets all tickets (purchased, reserved, cancelled) for the authenticated user.
     *
     * @param auth authentication of the current user
     * @return a list of MyTicketDto objects representing the user's tickets
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/my-tickets")
    @Operation(summary = "Get all tickets for the authenticated user", security = @SecurityRequirement(name = "apiKey"))
    public List<MyTicketDto> getAllTickets(Authentication auth) {
        LOGGER.info("GET /api/v1/bookings/my-tickets for user: {}", auth.getName());

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        return bookingService.getAllTicketsForUser(user.getId());
    }

    /**
     * Gets the checkout summary for a performance based on specific reserved tickets.
     *
     * @param performanceId performance id
     * @param ticketIds     list of ticket ids to include in the summary
     * @param auth          authentication of the current user
     * @return a checkout summary DTO containing information about the selected tickets
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/checkout/{performanceId}")
    @Operation(summary = "Get checkout summary from reserved tickets", security = @SecurityRequirement(name = "apiKey"))
    public CheckoutSummaryTicketDto getCheckoutSummaryFromTickets(
            @PathVariable("performanceId") Long performanceId,
            @RequestParam("ticketIds") List<Long> ticketIds,
            Authentication auth) throws ValidationException {
        LOGGER.info("GET /api/v1/bookings/checkout/{} with ticketIds: {} for user: {}", performanceId, ticketIds, auth.getName());

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        return bookingService.getCheckoutSummaryFromTickets(performanceId, ticketIds, user.getId());
    }

    /**
     * Purchases reserved tickets (converts them from reserved to purchased).
     *
     * @param request purchase request with ticket IDs
     * @param auth    authentication of the current user
     * @return the created order as a DTO
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/purchases-from-reservation")
    @Operation(summary = "Purchase reserved tickets", security = @SecurityRequirement(name = "apiKey"))
    public OrderDto purchaseFromReservation(@Valid @RequestBody PurchaseRequestWithTicketIdsDto request, Authentication auth)
        throws ValidationException {
        LOGGER.info("POST /api/v1/bookings/purchases-from-reservation body: {}", request);

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        return bookingService.purchaseFromReservation(request, user.getId());
    }

    /**
     * Cancels reserved tickets. Only tickets that are currently reserved and belong to the authenticated user can be cancelled.
     *
     * @param request request containing the IDs of the tickets to cancel
     * @param auth     authentication of the current user
     * @throws ValidationException if the ticket cannot be canceled (e.g., if it does not belong to the user, is not reserved, or has already been cancelled)
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @DeleteMapping("/tickets/cancel")
    @Operation(summary = "Cancel reserved ticket", security = @SecurityRequirement(name = "apiKey"))
    public void cancelReservedTickets(@Valid @RequestBody CancelPurchaseRequestDto request, Authentication auth) throws ValidationException {
        LOGGER.info("DELETE /api/v1/bookings/tickets/cancel body: {} by user {}", request.getTicketIds(), auth.getName());

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        bookingService.cancelReservedTickets(request.getTicketIds(), user.getId());
    }

    /**
     * Cancels purchased tickets. Only purchased tickets belonging to the authenticated user can be cancelled.
     *
     * @param request request containing the IDs of the tickets to cancel
     * @param auth    authentication of the current user
     * @throws ValidationException if the tickets cannot be cancelled
     */
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @PostMapping("/purchases/cancel")
    @Operation(summary = "Cancel purchased tickets", security = @SecurityRequirement(name = "apiKey"))
    public void cancelPurchasedTickets(@Valid @RequestBody CancelPurchaseRequestDto request, Authentication auth) throws ValidationException {
        LOGGER.info("POST /api/v1/bookings/purchases/cancel body: {} by user {}", request.getTicketIds(), auth.getName());

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        bookingService.cancelPurchasedTickets(request.getTicketIds(), user.getId());
    }
}
