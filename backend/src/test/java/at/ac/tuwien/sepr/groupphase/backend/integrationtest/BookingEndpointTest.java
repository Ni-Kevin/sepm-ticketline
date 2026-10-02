package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CancelPurchaseRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.OrderDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithSeatholdIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithTicketIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummaryTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.*;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.*;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import at.ac.tuwien.sepr.groupphase.backend.service.BookingService;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class BookingEndpointTest implements TestData {

    private static final String BOOKING_BASE_URI = BASE_URI + "/bookings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    private ApplicationUser testUser;

    @BeforeEach
    public void beforeEach() {
        invoiceRepository.deleteAll();
        ticketRepository.deleteAll();
        seatHoldRepository.deleteAll();
        orderRepository.deleteAll();
        performanceRepository.deleteAll();
        eventRepository.deleteAll();
        artistRepository.deleteAll();
        userRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        reservationRepository.deleteAll();

        testUser = new ApplicationUser();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail(USER_AUTH_EMAIL);
        testUser.setPassword("Password123!");
        testUser.setRole(UserRole.ROLE_USER);
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    public void afterEach() {
        reservationRepository.deleteAll();
        invoiceRepository.deleteAll();
        ticketRepository.deleteAll();
        seatHoldRepository.deleteAll();
        orderRepository.deleteAll();
        performanceRepository.deleteAll();
        eventRepository.deleteAll();
        artistRepository.deleteAll();
        userRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    public void postPurchaseSucceedsWithValidRequest() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);
        BigDecimal pricePerTicket = savedPerformance.getStartPrice();

        SeatHold hold = new SeatHold();
        hold.setPerformance(savedPerformance);
        hold.setSector(savedSector);
        hold.setUser(testUser);
        hold.setQuantity(2);
        hold.setActive(true);
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        SeatHold savedHold = seatHoldRepository.save(hold);

        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(savedPerformance.getId());
        request.setHoldIds(List.of(savedHold.getId()));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/26");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        String body = jsonMapper.writeValueAsString(request);

        MvcResult mvcResult = this.mockMvc.perform(post(BOOKING_BASE_URI + "/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        OrderDto orderDto = jsonMapper.readValue(response.getContentAsString(), OrderDto.class);

        assertAll(
            () -> assertNotNull(orderDto.getId()),
            () -> assertEquals("PURCHASED", orderDto.getStatus()),
            () -> assertEquals(new BigDecimal("20.00"), orderDto.getTotalPrice()),
            () -> assertEquals(PaymentMethod.CREDIT_CARD, orderDto.getPaymentMethod()),
            () -> assertNotNull(orderDto.getPurchaseDate())
        );

    }

    @Test
    public void postPurchaseReturnsUnprocessableEntityWhenPaymentDetailsInvalid() throws Exception {
        Performance performance = setupBasicPerformance();

        PurchaseRequestWithSeatholdIdsDto request = new PurchaseRequestWithSeatholdIdsDto();
        request.setPerformanceId(performance.getId());
        request.setHoldIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setPaymentDetails(Map.of("cardNumber", "short"));

        String body = jsonMapper.writeValueAsString(request);

        MvcResult result = this.mockMvc.perform(post(BOOKING_BASE_URI + "/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getResponse().getStatus());
    }

    @Test
    public void postReserveSucceedsWithValidRequest() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        SeatHold hold = new SeatHold();
        hold.setPerformance(savedPerformance);
        hold.setSector(savedSector);
        hold.setUser(testUser);
        hold.setQuantity(1);
        hold.setActive(true);
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        SeatHold savedHold = seatHoldRepository.save(hold);

        ReservationRequestDto request = new ReservationRequestDto();
        request.setPerformanceId(savedPerformance.getId());
        request.setHoldIds(List.of(savedHold.getId()));

        String body = jsonMapper.writeValueAsString(request);

        MvcResult mvcResult = this.mockMvc.perform(post(BOOKING_BASE_URI + "/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        ReservationResponseDto responseDto = jsonMapper.readValue(response.getContentAsString(), ReservationResponseDto.class);

        assertAll(
            () -> assertNotNull(responseDto.getReservationNumber()),
            () -> assertNotNull(responseDto.getReservedUntil())
        );

    }

    @Test
    public void postReserveReturnsNotFoundWhenPerformanceNotFound() throws Exception {
        ReservationRequestDto request = new ReservationRequestDto();
        request.setPerformanceId(999L);
        request.setHoldIds(List.of(1L));

        String body = jsonMapper.writeValueAsString(request);

        MvcResult result = this.mockMvc.perform(post(BOOKING_BASE_URI + "/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }


    @Test
    public void postPurchaseFromReservationSucceedsWithValidRequest() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        Reservation reservation = new Reservation();
        reservation.setUser(testUser);
        reservation.setReservedUntil(LocalDateTime.now().plusMinutes(15));
        reservation = reservationRepository.save(reservation);

        Ticket ticket1 = new Ticket();
        ticket1.setStatus(TicketStatus.RESERVED);
        ticket1.setFinalPrice(BigDecimal.TEN);
        ticket1.setPerformance(savedPerformance);
        ticket1.setSector(savedSector);
        ticket1.setReservation(reservation);
        ticket1 = ticketRepository.save(ticket1);

        Ticket ticket2 = new Ticket();
        ticket2.setStatus(TicketStatus.RESERVED);
        ticket2.setFinalPrice(BigDecimal.TEN);
        ticket2.setPerformance(savedPerformance);
        ticket2.setSector(savedSector);
        ticket2.setReservation(reservation);
        ticket2 = ticketRepository.save(ticket2);

        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(savedPerformance.getId());
        request.setTicketIds(List.of(ticket1.getId(), ticket2.getId()));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/30");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        String body = jsonMapper.writeValueAsString(request);

        MvcResult mvcResult = this.mockMvc.perform(post(BOOKING_BASE_URI + "/purchases-from-reservation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        OrderDto orderDto = jsonMapper.readValue(response.getContentAsString(), OrderDto.class);

        assertAll(
            () -> assertNotNull(orderDto.getId()),
            () -> assertEquals("PURCHASED", orderDto.getStatus()),
            () -> assertEquals(new BigDecimal("20.00"), orderDto.getTotalPrice()),
            () -> assertEquals(PaymentMethod.CREDIT_CARD, orderDto.getPaymentMethod()),
            () -> assertNotNull(orderDto.getPurchaseDate())
        );

        Ticket updatedTicket1 = ticketRepository.findById(ticket1.getId()).orElseThrow();
        assertEquals(TicketStatus.PURCHASED, updatedTicket1.getStatus());
        assertNull(updatedTicket1.getReservation());
        assertNotNull(updatedTicket1.getOrder());

        Ticket updatedTicket2 = ticketRepository.findById(ticket2.getId()).orElseThrow();
        assertEquals(TicketStatus.PURCHASED, updatedTicket2.getStatus());
        assertNull(updatedTicket2.getReservation());
        assertNotNull(updatedTicket2.getOrder());
    }

    @Test
    public void postPurchaseFromReservationReturns422WhenTicketBelongsToOtherUser() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        ApplicationUser otherUser = new ApplicationUser();
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setEmail("other2@example.com");
        otherUser.setPassword("Password123!");
        otherUser.setRole(UserRole.ROLE_USER);
        otherUser = userRepository.save(otherUser);

        Reservation reservation = new Reservation();
        reservation.setUser(otherUser);
        reservation.setReservedUntil(LocalDateTime.now().plusMinutes(15));
        reservation = reservationRepository.save(reservation);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(savedPerformance);
        ticket.setSector(savedSector);
        ticket.setReservation(reservation);
        ticket = ticketRepository.save(ticket);

        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(savedPerformance.getId());
        request.setTicketIds(List.of(ticket.getId()));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/30");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        String body = jsonMapper.writeValueAsString(request);

        MvcResult result = this.mockMvc.perform(post(BOOKING_BASE_URI + "/purchases-from-reservation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getResponse().getStatus());
    }

    @Test
    public void postPurchaseFromReservationReturns404WhenPerformanceNotFound() throws Exception {
        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(999L);
        request.setTicketIds(List.of(1L));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setPaymentDetails(Map.of("cardNumber", "1234567890123", "expiry", "12/30", "cvv", "123"));

        String body = jsonMapper.writeValueAsString(request);

        MvcResult result = this.mockMvc.perform(post(BOOKING_BASE_URI + "/purchases-from-reservation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void postPurchaseFromReservationReturns422WhenPaymentDetailsInvalid() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        Reservation reservation = new Reservation();
        reservation.setUser(testUser);
        reservation.setReservedUntil(LocalDateTime.now().plusMinutes(15));
        reservation = reservationRepository.save(reservation);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(savedPerformance);
        ticket.setSector(savedSector);
        ticket.setReservation(reservation);
        ticket = ticketRepository.save(ticket);

        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(savedPerformance.getId());
        request.setTicketIds(List.of(ticket.getId()));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setPaymentDetails(Map.of("cardNumber", "short"));

        String body = jsonMapper.writeValueAsString(request);

        MvcResult result = this.mockMvc.perform(post(BOOKING_BASE_URI + "/purchases-from-reservation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getResponse().getStatus());
    }

    @Test
    public void getCheckoutSummaryFromTicketsSucceedsWithValidRequest() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        Reservation reservation = new Reservation();
        reservation.setUser(testUser);
        reservation.setReservedUntil(LocalDateTime.now().plusMinutes(15));
        reservation = reservationRepository.save(reservation);

        Ticket ticket1 = new Ticket();
        ticket1.setStatus(TicketStatus.RESERVED);
        ticket1.setFinalPrice(BigDecimal.TEN);
        ticket1.setPerformance(savedPerformance);
        ticket1.setSector(savedSector);
        ticket1.setReservation(reservation);
        ticket1 = ticketRepository.save(ticket1);

        Ticket ticket2 = new Ticket();
        ticket2.setStatus(TicketStatus.RESERVED);
        ticket2.setFinalPrice(BigDecimal.TEN);
        ticket2.setPerformance(savedPerformance);
        ticket2.setSector(savedSector);
        ticket2.setReservation(reservation);
        ticket2 = ticketRepository.save(ticket2);

        MvcResult mvcResult = this.mockMvc.perform(get(BOOKING_BASE_URI + "/checkout/" + savedPerformance.getId()
                + "?ticketIds=" + ticket1.getId() + "," + ticket2.getId())
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        CheckoutSummaryTicketDto result = jsonMapper.readValue(response.getContentAsString(), CheckoutSummaryTicketDto.class);

        assertAll(
            () -> assertEquals(2, result.getItems().size()),
            () -> assertEquals(new BigDecimal("20.00"), result.getTotalAmount()),
            () -> assertEquals("Basic Event", result.getEventName()),
            () -> assertTrue(result.getArtists().contains("Basic Artist")),
            () -> assertEquals("Basic Performance", result.getPerformanceName()),
            () -> assertNotNull(result.getPerformanceStartTime()),
            () -> assertEquals("Basic Hall", result.getHallName()),
            () -> assertEquals("Basic Venue", result.getVenueName())
        );
    }

    @Test
    public void getCheckoutSummaryFromTicketsReturns404WhenPerformanceNotFound() throws Exception {
        MvcResult result = this.mockMvc.perform(get(BOOKING_BASE_URI + "/checkout/999?ticketIds=1")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void getCheckoutSummaryFromTicketsReturns404WhenTicketsNotFound() throws Exception {
        Performance savedPerformance = setupBasicPerformance();

        MvcResult result = this.mockMvc.perform(get(BOOKING_BASE_URI + "/checkout/" + savedPerformance.getId() + "?ticketIds=99999")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void getCheckoutSummaryFromTicketsReturns403WhenNotAuthenticated() throws Exception {
        MvcResult result = this.mockMvc.perform(get(BOOKING_BASE_URI + "/checkout/1?ticketIds=1"))
            .andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), result.getResponse().getStatus());
    }

    private Performance setupBasicPerformance() {
        Venue venue = new Venue();
        venue.setName("Basic Venue");
        venue.setStreet("Street"); venue.setCity("City"); venue.setZipCode("123"); venue.setCountry("AT");
        venue = venueRepository.save(venue);

        Hall hall = new Hall();
        hall.setName("Basic Hall");
        hall.setWidth(10); hall.setLength(10);
        hall.setVenue(venue);
        hall = hallRepository.save(hall);

        Sector sector = new Sector();
        sector.setName("Basic Sector");
        sector.setType(SectorType.STANDING);
        sector.setCapacity(100);
        sector.setHall(hall);
        sector.setColor("Blue");
        sector = sectorRepository.save(sector);

        hall.setSectors(List.of(sector));

        Event event = new Event();
        event.setTitle("Basic Event");
        event.setGenre("Pop");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        event = eventRepository.save(event);

        Artist artist = new Artist();
        artist.setArtistName("Basic Artist");
        artist.setFirstName("Basic");
        artist.setLastName("Artist");
        artist = artistRepository.save(artist);

        Performance perf = new Performance();
        perf.setPerformanceName("Basic Performance");
        perf.setStartTime(LocalDateTime.now().plusDays(1));
        perf.setEndTime(perf.getStartTime().plusHours(2));
        perf.setStartPrice(BigDecimal.TEN);
        perf.setHall(hall);
        perf.setEvent(event);
        perf.setArtists(new HashSet<>(Set.of(artist)));
        return performanceRepository.save(perf);
    }

    @Test
    public void deleteTicketReturns200WhenTicketIsReservedAndBelongsToUser() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        Reservation reservation = new Reservation();
        reservation.setUser(testUser);
        reservation.setReservedUntil(LocalDateTime.now().plusMinutes(15));
        reservation = reservationRepository.save(reservation);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(savedPerformance);
        ticket.setSector(savedSector);
        ticket.setReservation(reservation);
        ticket = ticketRepository.save(ticket);

        CancelPurchaseRequestDto request = new CancelPurchaseRequestDto();
        request.setTicketIds(List.of(ticket.getId()));

        MvcResult result = this.mockMvc.perform(delete(BOOKING_BASE_URI + "/tickets/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(TicketStatus.CANCELLED, updated.getStatus());
    }

    @Test
    public void deleteTicketReturns404WhenTicketDoesNotExist() throws Exception {
        CancelPurchaseRequestDto request = new CancelPurchaseRequestDto();
        request.setTicketIds(List.of(99999L));

        MvcResult result = this.mockMvc.perform(delete(BOOKING_BASE_URI + "/tickets/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

    @Test
    public void deleteTicketReturns422WhenTicketIsNotReserved() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        Reservation reservation = new Reservation();
        reservation.setUser(testUser);
        reservation.setReservedUntil(LocalDateTime.now().plusMinutes(15));
        reservation = reservationRepository.save(reservation);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.PURCHASED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(savedPerformance);
        ticket.setSector(savedSector);
        ticket.setReservation(reservation);
        ticket = ticketRepository.save(ticket);

        CancelPurchaseRequestDto request = new CancelPurchaseRequestDto();
        request.setTicketIds(List.of(ticket.getId()));

        MvcResult result = this.mockMvc.perform(delete(BOOKING_BASE_URI + "/tickets/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getResponse().getStatus());
    }

    @Test
    public void deleteTicketReturns422WhenTicketBelongsToOtherUser() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        ApplicationUser otherUser = new ApplicationUser();
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("Password123!");
        otherUser.setRole(UserRole.ROLE_USER);
        otherUser = userRepository.save(otherUser);

        Reservation reservation = new Reservation();
        reservation.setUser(otherUser);
        reservation.setReservedUntil(LocalDateTime.now().plusMinutes(15));
        reservation = reservationRepository.save(reservation);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(savedPerformance);
        ticket.setSector(savedSector);
        ticket.setReservation(reservation);
        ticket = ticketRepository.save(ticket);

        CancelPurchaseRequestDto request = new CancelPurchaseRequestDto();
        request.setTicketIds(List.of(ticket.getId()));

        MvcResult result = this.mockMvc.perform(delete(BOOKING_BASE_URI + "/tickets/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getResponse().getStatus());
    }

    @Test
    public void deleteTicketReturns403WhenNotAuthenticated() throws Exception {
        CancelPurchaseRequestDto request = new CancelPurchaseRequestDto();
        request.setTicketIds(List.of());

        MvcResult result = this.mockMvc.perform(delete(BOOKING_BASE_URI + "/tickets/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .header("Authorization", "")) // Invalid auth
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), result.getResponse().getStatus());
    }

    @Test
    public void concurrentPurchaseFromReservationBySameUserOnlySucceedsOnce() throws Exception {
        Performance perf = setupBasicPerformance();
        Long perfId = perf.getId();
        Sector sector = sectorRepository.findAll().get(0);

        Reservation reservation = new Reservation();
        reservation.setUser(testUser);
        reservation.setReservedUntil(LocalDateTime.now().plusMinutes(15));
        Reservation savedReservation = reservationRepository.save(reservation);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(perf);
        ticket.setSector(sector);
        ticket.setReservation(savedReservation);
        Ticket savedTicket = ticketRepository.save(ticket);
        Long ticketId = savedTicket.getId();

        PurchaseRequestWithTicketIdsDto request = new PurchaseRequestWithTicketIdsDto();
        request.setPerformanceId(perfId);
        request.setTicketIds(List.of(ticketId));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        Map<String, String> details = new HashMap<>();
        details.put("cardNumber", "1234567890123");
        details.put("expiry", "12/30");
        details.put("cvv", "123");
        request.setPaymentDetails(details);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                try {
                    bookingService.purchaseFromReservation(request, testUser.getId());
                    return "SUCCESS";
                } catch (ValidationException e) {
                    return "VALIDATION_FAILED: " + e.getMessage();
                } catch (Exception e) {
                    return "ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage();
                }
            }));
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        List<String> results = futures.stream()
            .map(f -> { try { return f.get(); } catch (Exception e) { return "FUTURE_ERROR"; } })
            .toList();

        long successCount = results.stream().filter(r -> r.equals("SUCCESS")).count();
        assertEquals(1, successCount, "Nur einer der parallelen Käufe darf erfolgreich sein. Ergebnisse: " + results);

        Ticket updated = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatus.PURCHASED, updated.getStatus());
        assertNotNull(updated.getOrder());
    }

    @Test
    public void postCancelPurchaseReturns200AndCancelsTicketAndOrder() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        Sector savedSector = sectorRepository.findAll().get(0);

        Order order = new Order();
        order.setUser(testUser);
        order.setPurchaseDate(LocalDateTime.now().minusDays(1));
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.PURCHASED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(savedPerformance);
        ticket.setSector(savedSector);
        ticket.setOrder(order);
        ticket = ticketRepository.save(ticket);

        CancelPurchaseRequestDto request = new CancelPurchaseRequestDto();
        request.setTicketIds(List.of(ticket.getId()));

        MvcResult result = this.mockMvc.perform(post(BOOKING_BASE_URI + "/purchases/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());

        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertAll(
            () -> assertEquals(TicketStatus.CANCELLED, updatedTicket.getStatus()),
            () -> assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus())
        );
    }

    @Test
    public void postCancelPurchaseReturns422WhenPerformanceAlreadyStarted() throws Exception {
        Performance savedPerformance = setupBasicPerformance();
        savedPerformance.setStartTime(LocalDateTime.now().minusMinutes(1));
        savedPerformance.setEndTime(LocalDateTime.now().plusHours(1));
        savedPerformance = performanceRepository.save(savedPerformance);
        Sector savedSector = sectorRepository.findAll().get(0);

        Order order = new Order();
        order.setUser(testUser);
        order.setPurchaseDate(LocalDateTime.now().minusDays(1));
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.PURCHASED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(savedPerformance);
        ticket.setSector(savedSector);
        ticket.setOrder(order);
        ticket = ticketRepository.save(ticket);

        CancelPurchaseRequestDto request = new CancelPurchaseRequestDto();
        request.setTicketIds(List.of(ticket.getId()));

        MvcResult result = this.mockMvc.perform(post(BOOKING_BASE_URI + "/purchases/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), result.getResponse().getStatus());
        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertEquals(TicketStatus.PURCHASED, updated.getStatus());
    }

}
