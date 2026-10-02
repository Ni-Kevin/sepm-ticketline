package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithSeatholdIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithTicketIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummaryTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.OrderDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.TicketItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.*;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.*;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.BookingServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private ApplicationUser user;
    private Performance performance;
    private Sector sector;
    private SeatHold hold;

    @BeforeEach
    void setUp() {
        user = new ApplicationUser();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setLastName("Doe");

        performance = new Performance();
        performance.setId(1L);
        performance.setStartTime(LocalDateTime.now().plusDays(1));
        performance.setStartPrice(BigDecimal.TEN);

        sector = new Sector();
        sector.setId(1L);
        sector.setName("Sektor A");

        hold = new SeatHold();
        hold.setId(1L);
        hold.setUser(user);
        hold.setPerformance(performance);
        hold.setSector(sector);
        hold.setQuantity(1);
        hold.setActive(true);
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    }

    @Test
    public void purchaseSucceedsWithValidRequest() throws ValidationException {
        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/26");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(sectorRepository.findByIdWithVenue(1L)).thenReturn(Optional.of(sector));

        LocalDateTime testDate = LocalDateTime.now();
        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(BigDecimal.TEN);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setPurchaseDate(testDate);
        when(orderRepository.save(any())).thenReturn(order);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        when(ticketRepository.save(any())).thenReturn(ticket);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1-123");
        invoice.setType(InvoiceType.PURCHASED);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(BigDecimal.TEN);
        when(invoiceRepository.save(any())).thenReturn(invoice);

        OrderDto result = bookingService.purchase(request, 1L);

        assertAll("Order DTO Properties",
            () -> assertNotNull(result),
            () -> assertEquals(testDate, result.getPurchaseDate()),
            () -> assertEquals(BigDecimal.TEN, result.getTotalPrice()),
            () -> assertEquals(PaymentMethod.CREDIT_CARD, result.getPaymentMethod()),
            () -> assertEquals("PURCHASED", result.getStatus())
        );
        verify(emailService).sendOrderConfirmationEmail(anyString(), anyString(), any());
    }

    @Test
    public void purchaseThrowsValidationExceptionWhenPerformanceStarted() {
        performance.setStartTime(LocalDateTime.now().minusDays(1));
        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(1L);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        assertThrows(ValidationException.class, () -> bookingService.purchase(request, 1L));
    }

    @Test
    public void purchaseThrowsValidationExceptionWhenHoldExpired() {
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));

        assertThrows(ValidationException.class, () -> bookingService.purchase(request, 1L));
    }

    @Test
    public void purchaseThrowsValidationExceptionWhenPaymentDetailsInvalid() {
        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setPaymentDetails(Map.of("cardNumber", "invalid"));

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));

        assertThrows(ValidationException.class, () -> bookingService.purchase(request, 1L));
    }

    @Test
    public void reserveSucceedsWithValidRequest() throws ValidationException {
        ReservationRequestDto request = new ReservationRequestDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationNumber("RES-TEST");
        reservation.setReservedUntil(performance.getStartTime().minusMinutes(30));
        when(reservationRepository.save(any())).thenReturn(reservation);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        when(ticketRepository.save(any())).thenReturn(ticket);

        ReservationResponseDto result = bookingService.reserve(request, 1L);

        assertAll("Reservation Response Properties",
            () -> assertNotNull(result),
            () -> assertEquals("RES-TEST", result.getReservationNumber()),
            () -> assertEquals(performance.getStartTime().minusMinutes(30), result.getReservedUntil()),
            () -> verify(emailService).sendReservationConfirmationEmail(anyString(), anyString(), any()),
            () -> verify(seatHoldRepository).save(argThat(h -> !h.isActive()))
        );

    }

    @Test
    public void reserveThrowsValidationExceptionWhenPerformanceStarted() {
        performance.setStartTime(LocalDateTime.now().minusDays(1));
        ReservationRequestDto request = new ReservationRequestDto();
        request.setPerformanceId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        assertThrows(ValidationException.class, () -> bookingService.reserve(request, 1L));
    }

    @Test
    public void reserveThrowsValidationExceptionWhenHoldExpired() {
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        ReservationRequestDto request = new ReservationRequestDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));

        assertThrows(ValidationException.class, () -> bookingService.reserve(request, 1L));
    }

    @Test
    public void cancelReservedTicketSucceedsWithValidRequest() throws ValidationException {
        Ticket ticket = buildReservedTicket(user);
        when(ticketRepository.findAllById(List.of(1L))).thenReturn(List.of(ticket));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        bookingService.cancelReservedTickets(List.of(1L), 1L);

        assertEquals(TicketStatus.CANCELLED, ticket.getStatus());
        verify(ticketRepository).save(ticket);
        verify(emailService).sendCancellationEmail(eq("test@example.com"), eq("Doe"), eq(ticket));
    }

    @Test
    public void cancelReservedTicketThrowsNotFoundWhenTicketDoesNotExist() {
        when(ticketRepository.findAllById(List.of(99L)))
            .thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> bookingService.cancelReservedTickets(List.of(99L), 1L));
    }

    @Test
    public void cancelReservedTicketThrowsValidationExceptionWhenTicketNotReserved() {
        Ticket ticket = buildReservedTicket(user);
        ticket.setStatus(TicketStatus.PURCHASED);
        when(ticketRepository.findAllById(List.of(1L)))
            .thenReturn(List.of(ticket));

        assertThrows(ValidationException.class, () -> bookingService.cancelReservedTickets(List.of(1L), 1L));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    public void cancelReservedTicketThrowsValidationExceptionWhenTicketAlreadyCancelled() {
        Ticket ticket = buildReservedTicket(user);
        ticket.setStatus(TicketStatus.CANCELLED);
        when(ticketRepository.findAllById(List.of(1L)))
            .thenReturn(List.of(ticket));

        assertThrows(ValidationException.class, () -> bookingService.cancelReservedTickets(List.of(1L), 1L));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    public void cancelReservedTicketThrowsValidationExceptionWhenTicketBelongsToOtherUser() {
        ApplicationUser otherUser = new ApplicationUser();
        otherUser.setId(99L);
        otherUser.setEmail("other@example.com");
        otherUser.setLastName("Other");

        Ticket ticket = buildReservedTicket(otherUser);
        when(ticketRepository.findAllById(List.of(1L)))
            .thenReturn(List.of(ticket));

        assertThrows(ValidationException.class, () -> bookingService.cancelReservedTickets(List.of(1L), 1L));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    public void cancelReservedTicketSendsEmailAfterSuccessfulCancellation() throws ValidationException {
        Ticket ticket = buildReservedTicket(user);
        when(ticketRepository.findAllById(List.of(1L))).thenReturn(List.of(ticket));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        bookingService.cancelReservedTickets(List.of(1L), 1L);

        verify(emailService, times(1)).sendCancellationEmail("test@example.com", "Doe", ticket);
    }

    @Test
    public void cancelReservedTicketDoesNotSendEmailWhenValidationFails() {
        Ticket ticket = buildReservedTicket(user);
        ticket.setStatus(TicketStatus.PURCHASED);
        when(ticketRepository.findAllById(List.of(1L)))
            .thenReturn(List.of(ticket));

        assertThrows(ValidationException.class, () -> bookingService.cancelReservedTickets(List.of(1L), 1L));
        verify(emailService, never()).sendCancellationEmail(any(), any(), any());
    }

    @Test
    public void cancelReservedTicketReleasesSeatedHold() throws ValidationException {
        Ticket ticket = buildReservedTicket(user);

        SeatHold hold = new SeatHold();
        hold.setId(1L);

        Seat seat = new Seat();
        seat.setId(1L);
        ticket.setSeat(seat);

        when(ticketRepository.findAllById(List.of(1L))).thenReturn(List.of(ticket));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(seatHoldRepository.findActiveHoldBySeat(eq(1L), eq(1L), any()))
            .thenReturn(Optional.of(hold));

        bookingService.cancelReservedTickets(List.of(1L), 1L);

        verify(seatHoldRepository).delete(hold);
    }

    @Test
    public void cancelReservedTicketReleasesStandingHold() throws ValidationException {
        Ticket ticket = buildReservedTicket(user);

        SeatHold hold = new SeatHold();
        hold.setId(1L);

        when(ticketRepository.findAllById(List.of(1L))).thenReturn(List.of(ticket));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(seatHoldRepository.findActiveHoldsByUserSectorAndPerformance(eq(1L), eq(1L), eq(1L), any()))
            .thenReturn(List.of(hold));

        bookingService.cancelReservedTickets(List.of(1L), 1L);

        verify(seatHoldRepository).delete(hold);
    }

    @Test
    public void purchaseFromReservationSucceedsWithValidRequest() throws ValidationException {
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(1L);
        request.setTicketIds(List.of(1L, 2L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/30");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        Ticket ticket1 = buildReservedTicket(user);
        ticket1.setId(1L);
        Ticket ticket2 = buildReservedTicket(user);
        ticket2.setId(2L);
        ticket2.setFinalPrice(BigDecimal.TEN);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(ticketRepository.findReservedTicketIdsWithLock(List.of(1L, 2L), 1L))
            .thenReturn(List.of(1L, 2L));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L, 2L), 1L))
            .thenReturn(List.of(ticket1, ticket2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LocalDateTime testDate = LocalDateTime.now();
        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(new BigDecimal("20.00"));
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setPurchaseDate(testDate);
        when(orderRepository.save(any())).thenReturn(order);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1-123");
        invoice.setType(InvoiceType.PURCHASED);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(new BigDecimal("20.00"));
        when(invoiceRepository.save(any())).thenReturn(invoice);

        when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.existsByReservationId(anyLong())).thenReturn(false);

        OrderDto result = bookingService.purchaseFromReservation(request, 1L);

        assertAll("Order DTO Properties",
            () -> assertNotNull(result),
            () -> assertEquals(testDate, result.getPurchaseDate()),
            () -> assertEquals(new BigDecimal("20.00"), result.getTotalPrice()),
            () -> assertEquals(PaymentMethod.CREDIT_CARD, result.getPaymentMethod()),
            () -> assertEquals("PURCHASED", result.getStatus())
        );
        verify(emailService).sendOrderConfirmationEmail(anyString(), anyString(), any());
        verify(ticketRepository, times(2)).save(any());
        verify(reservationRepository).delete(any());
    }

    @Test
    public void purchaseFromReservationThrowsValidationExceptionWhenPerformanceTooClose() {
        performance.setStartTime(LocalDateTime.now().plusMinutes(10));
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(1L);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        assertThrows(ValidationException.class, () -> bookingService.purchaseFromReservation(request, 1L));
    }

    @Test
    public void purchaseFromReservationThrowsNotFoundExceptionWhenPerformanceNotFound() {
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(999L);

        when(performanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.purchaseFromReservation(request, 1L));
    }

    @Test
    public void purchaseFromReservationThrowsValidationExceptionWhenSomeTicketsNotFound() {
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(1L);
        request.setTicketIds(List.of(1L, 2L));

        Ticket ticket = buildReservedTicket(user);
        ticket.setId(1L);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(ticketRepository.findReservedTicketIdsWithLock(List.of(1L, 2L), 1L))
            .thenReturn(List.of(1L));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> bookingService.purchaseFromReservation(request, 1L));
        assertTrue(ex.getMessage().contains("could not be found"));
    }

    @Test
    public void purchaseFromReservationThrowsValidationExceptionWhenTicketForWrongPerformance() {
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(1L);
        request.setTicketIds(List.of(1L));

        Performance otherPerformance = new Performance();
        otherPerformance.setId(999L);

        Ticket ticket = buildReservedTicket(user);
        ticket.setPerformance(otherPerformance);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(ticketRepository.findReservedTicketIdsWithLock(List.of(1L), 1L))
            .thenReturn(List.of(1L));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> bookingService.purchaseFromReservation(request, 1L));
        assertTrue(ex.getMessage().contains("does not belong to the selected performance"));
    }

    @Test
    public void purchaseFromReservationThrowsValidationExceptionWhenPaymentFails() {
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(1L);
        request.setTicketIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setPaymentDetails(Map.of("cardNumber", "short"));

        Ticket ticket = buildReservedTicket(user);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(ticketRepository.findReservedTicketIdsWithLock(List.of(1L), 1L))
            .thenReturn(List.of(1L));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));

        assertThrows(ValidationException.class, () -> bookingService.purchaseFromReservation(request, 1L));
        verify(orderRepository, never()).save(any());
        verify(emailService, never()).sendOrderConfirmationEmail(any(), any(), any());
    }

    @Test
    public void purchaseFromReservationDeletesEmptyReservation() throws ValidationException {
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(1L);
        request.setTicketIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/30");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        Ticket ticket = buildReservedTicket(user);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(ticketRepository.findReservedTicketIdsWithLock(List.of(1L), 1L))
            .thenReturn(List.of(1L));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LocalDateTime testDate = LocalDateTime.now();
        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(BigDecimal.TEN);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setPurchaseDate(testDate);
        when(orderRepository.save(any())).thenReturn(order);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1-123");
        invoice.setType(InvoiceType.PURCHASED);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(BigDecimal.TEN);
        when(invoiceRepository.save(any())).thenReturn(invoice);

        when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.existsByReservationId(anyLong())).thenReturn(false);

        bookingService.purchaseFromReservation(request, 1L);

        verify(reservationRepository).delete(any());
    }

    @Test
    public void purchaseFromReservationKeepsReservationWhenTicketsRemain() throws ValidationException {
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(1L);
        request.setTicketIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/30");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        Ticket ticket = buildReservedTicket(user);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(ticketRepository.findReservedTicketIdsWithLock(List.of(1L), 1L))
            .thenReturn(List.of(1L));
        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LocalDateTime testDate = LocalDateTime.now();
        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(BigDecimal.TEN);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setPurchaseDate(testDate);
        when(orderRepository.save(any())).thenReturn(order);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1-123");
        invoice.setType(InvoiceType.PURCHASED);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(BigDecimal.TEN);
        when(invoiceRepository.save(any())).thenReturn(invoice);

        when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.existsByReservationId(anyLong())).thenReturn(true);

        bookingService.purchaseFromReservation(request, 1L);

        verify(reservationRepository, never()).delete(any());
    }

    @Test
    public void getCheckoutSummaryFromTicketsSucceedsWithValidRequest() throws ValidationException {
        Hall hall = new Hall();
        hall.setName("Test Hall");
        Venue venue = new Venue();
        venue.setName("Test Venue");
        hall.setVenue(venue);

        Event event = new Event();
        event.setTitle("Test Event");

        Artist artist = new Artist();
        artist.setArtistName("Test Artist");

        performance.setPerformanceName("Test Perf");
        performance.setHall(hall);
        performance.setEvent(event);
        performance.setArtists(Set.of(artist));

        Ticket ticket1 = buildReservedTicket(user);
        ticket1.setId(1L);
        Ticket ticket2 = buildReservedTicket(user);
        ticket2.setId(2L);

        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L, 2L), 1L))
            .thenReturn(List.of(ticket1, ticket2));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        CheckoutSummaryTicketDto result = bookingService.getCheckoutSummaryFromTickets(1L, List.of(1L, 2L), 1L);

        assertAll("Checkout summary properties",
            () -> assertEquals(2, result.getItems().size()),
            () -> assertEquals(new BigDecimal("20"), result.getTotalAmount()),
            () -> assertEquals("Test Event", result.getEventName()),
            () -> assertTrue(result.getArtists().contains("Test Artist")),
            () -> assertEquals("Test Perf", result.getPerformanceName()),
            () -> assertEquals(performance.getStartTime(), result.getPerformanceStartTime()),
            () -> assertEquals("Test Hall", result.getHallName()),
            () -> assertEquals("Test Venue", result.getVenueName())
        );
        TicketItemDto item1 = result.getItems().get(0);
        assertEquals(1L, item1.getTicketId());
        assertEquals("Sektor A", item1.getSectorName());
        assertEquals(1, item1.getQuantity());
        assertEquals(BigDecimal.TEN, item1.getPrice());
    }

    @Test
    public void getCheckoutSummaryFromTicketsThrowsNotFoundExceptionWhenTicketsNotFound() {
        Ticket ticket = buildReservedTicket(user);

        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L, 2L), 1L))
            .thenReturn(List.of(ticket));

        assertThrows(NotFoundException.class,
            () -> bookingService.getCheckoutSummaryFromTickets(1L, List.of(1L, 2L), 1L));
    }

    @Test
    public void getCheckoutSummaryFromTicketsThrowsNotFoundExceptionWhenPerformanceNotFound() {
        Ticket ticket = buildReservedTicket(user);

        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(performanceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> bookingService.getCheckoutSummaryFromTickets(1L, List.of(1L), 1L));
    }

    @Test
    public void getCheckoutSummaryFromTicketsThrowsValidationExceptionWhenTicketForWrongPerformance() {
        Performance otherPerformance = new Performance();
        otherPerformance.setId(999L);

        Ticket ticket = buildReservedTicket(user);
        ticket.setPerformance(otherPerformance);

        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> bookingService.getCheckoutSummaryFromTickets(1L, List.of(1L), 1L));
        assertTrue(ex.getMessage().contains("does not belong to performance"));
    }

    @Test
    public void getCheckoutSummaryFromTicketsIncludesSeatName() throws ValidationException {
        Seat seat = new Seat();
        seat.setId(1L);
        seat.setRowNumber(5);
        seat.setSeatNumber(12);

        performance.setPerformanceName("Test");
        performance.setHall(new Hall() {{ setName("H"); setVenue(new Venue() {{ setName("V"); }}); }});

        Ticket ticket = buildReservedTicket(user);
        ticket.setSeat(seat);

        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        CheckoutSummaryTicketDto result = bookingService.getCheckoutSummaryFromTickets(1L, List.of(1L), 1L);

        assertEquals("Row 5, Seat 12", result.getItems().get(0).getSeatName());
    }

    @Test
    public void getCheckoutSummaryFromTicketsSeatNameIsNullForStandingTicket() throws ValidationException {
        performance.setPerformanceName("Test");
        performance.setHall(new Hall() {{ setName("H"); setVenue(new Venue() {{ setName("V"); }}); }});

        Ticket ticket = buildReservedTicket(user);

        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        CheckoutSummaryTicketDto result = bookingService.getCheckoutSummaryFromTickets(1L, List.of(1L), 1L);

        assertNull(result.getItems().get(0).getSeatName());
    }

    @Test
    public void getCheckoutSummaryFromTicketsUsesPerformanceNameWhenEventIsNull() throws ValidationException {
        performance.setPerformanceName("Fallback Name");
        performance.setHall(new Hall() {{ setName("H"); setVenue(new Venue() {{ setName("V"); }}); }});

        Ticket ticket = buildReservedTicket(user);

        when(ticketRepository.findReservedTicketsWithAllRelations(List.of(1L), 1L))
            .thenReturn(List.of(ticket));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        CheckoutSummaryTicketDto result = bookingService.getCheckoutSummaryFromTickets(1L, List.of(1L), 1L);

        assertEquals("Fallback Name", result.getEventName());
    }

    @Test
    public void cancelPurchasedTicketThrowsValidationExceptionWhenPerformanceStarted() {
        performance.setStartTime(LocalDateTime.now().minusMinutes(1));
        Ticket ticket = buildPurchasedTicket(user);

        when(ticketRepository.findPurchasedTicketsByIdsAndUserId(List.of(1L), 1L)).thenReturn(List.of(ticket));

        assertThrows(ValidationException.class, () -> bookingService.cancelPurchasedTickets(List.of(1L), 1L));
        verify(ticketRepository, never()).save(any());
        verify(emailService, never()).sendPurchaseCancellationEmail(any(), any(), any(), any());
    }

    @Test
    public void cancelPurchasedTicketSucceedsWithValidRequest() throws ValidationException {
        Ticket ticket = buildPurchasedTicket(user);
        Order order = ticket.getOrder();
        order.setTickets(List.of(ticket));

        when(ticketRepository.findPurchasedTicketsByIdsAndUserId(List.of(1L), 1L)).thenReturn(List.of(ticket));
        when(orderRepository.findByTicketIdWithTickets(1L)).thenReturn(Optional.of(order));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(orderRepository.save(any())).thenReturn(order);

        when(invoiceRepository.findFirstByOrderIdAndTypeOrderByIdDesc(1L, InvoiceType.CANCELLED))
            .thenReturn(Optional.empty());
        Invoice cancellationInvoice = new Invoice();
        cancellationInvoice.setInvoiceNumber("CINV-1-123");
        cancellationInvoice.setType(InvoiceType.CANCELLED);
        cancellationInvoice.setCreatedAt(LocalDateTime.now());
        cancellationInvoice.setTotalAmount(BigDecimal.TEN);
        when(invoiceRepository.save(any())).thenReturn(cancellationInvoice);

        bookingService.cancelPurchasedTickets(List.of(1L), 1L);

        assertAll(
            () -> assertEquals(TicketStatus.CANCELLED, ticket.getStatus()),
            () -> assertEquals(OrderStatus.CANCELLED, order.getStatus()),
            () -> verify(ticketRepository).save(ticket),
            () -> verify(orderRepository).save(order),
            () -> verify(emailService).sendPurchaseCancellationEmail("test@example.com", "Doe", order, List.of(ticket))
        );
    }

    @Test
    public void cancelPurchasedTicketThrowsValidationExceptionWhenTicketDoesNotBelongToUser() {
        when(ticketRepository.findPurchasedTicketsByIdsAndUserId(List.of(1L), 1L)).thenReturn(List.of());

        assertThrows(ValidationException.class, () -> bookingService.cancelPurchasedTickets(List.of(1L), 1L));
        verify(ticketRepository, never()).save(any());
        verify(emailService, never()).sendPurchaseCancellationEmail(any(), any(), any(), any());
    }

    @Test
    public void reserveThrowsValidationExceptionWhenExceedsLimit() {
        ReservationRequestDto request = new ReservationRequestDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));
        when(ticketRepository.countTicketsForUserAndPerformance(eq(1L), eq(1L))).thenReturn(10);

        ValidationException ex = assertThrows(ValidationException.class,
            () -> bookingService.reserve(request, 1L));
        assertTrue(ex.getMessage().contains("Maximum of 10 tickets"));
        verify(reservationRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    public void purchaseThrowsValidationExceptionWhenExceedsLimit() {
        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/26");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));
        when(ticketRepository.countTicketsForUserAndPerformance(eq(1L), eq(1L))).thenReturn(10);

        ValidationException ex = assertThrows(ValidationException.class,
            () -> bookingService.purchase(request, 1L));
        assertTrue(ex.getMessage().contains("Maximum of 10 tickets"));
        verify(orderRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    public void purchaseSucceedsWhenAtExactLimit() throws ValidationException {
        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/26");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));
        when(ticketRepository.countTicketsForUserAndPerformance(eq(1L), eq(1L))).thenReturn(9);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(sectorRepository.findByIdWithVenue(1L)).thenReturn(Optional.of(sector));

        LocalDateTime testDate = LocalDateTime.now();
        Order order = new Order();
        order.setId(1L);
        order.setTotalPrice(BigDecimal.TEN);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setPurchaseDate(testDate);
        when(orderRepository.save(any())).thenReturn(order);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        when(ticketRepository.save(any())).thenReturn(ticket);

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-1-123");
        invoice.setType(InvoiceType.PURCHASED);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(BigDecimal.TEN);
        when(invoiceRepository.save(any())).thenReturn(invoice);

        OrderDto result = bookingService.purchase(request, 1L);

        assertNotNull(result);
        assertEquals(BigDecimal.TEN, result.getTotalPrice());
        verify(emailService).sendOrderConfirmationEmail(anyString(), anyString(), any());
    }

    @Test
    public void purchaseThrowsWhenRequestedQuantityExceedsLimit() {
        hold.setQuantity(11);
        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/26");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));
        when(ticketRepository.countTicketsForUserAndPerformance(eq(1L), eq(1L))).thenReturn(0);

        ValidationException ex = assertThrows(ValidationException.class,
            () -> bookingService.purchase(request, 1L));
        assertTrue(ex.getMessage().contains("Maximum of 10 tickets"));
        verify(orderRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    public void reserveSucceedsWhenAtExactLimit() throws ValidationException {
        ReservationRequestDto request = new ReservationRequestDto();
        request.setPerformanceId(1L);
        request.setHoldIds(List.of(1L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(seatHoldRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(hold));
        when(ticketRepository.countTicketsForUserAndPerformance(eq(1L), eq(1L))).thenReturn(9);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationNumber("RES-TEST");
        reservation.setReservedUntil(performance.getStartTime().minusMinutes(30));
        when(reservationRepository.save(any())).thenReturn(reservation);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        when(ticketRepository.save(any())).thenReturn(ticket);

        ReservationResponseDto result = bookingService.reserve(request, 1L);

        assertNotNull(result);
        assertEquals("RES-TEST", result.getReservationNumber());
        verify(emailService).sendReservationConfirmationEmail(anyString(), anyString(), any());
        verify(seatHoldRepository).save(argThat(h -> !h.isActive()));
    }

    private Ticket buildReservedTicket(ApplicationUser owner) {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(owner);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setReservation(reservation);
        ticket.setPerformance(performance);
        ticket.setSector(sector);
        return ticket;
    }

    private Ticket buildPurchasedTicket(ApplicationUser owner) {
        Order order = new Order();
        order.setId(1L);
        order.setUser(owner);
        order.setStatus(OrderStatus.PURCHASED);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.PURCHASED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setOrder(order);
        ticket.setPerformance(performance);
        ticket.setSector(sector);
        return ticket;
    }
}
