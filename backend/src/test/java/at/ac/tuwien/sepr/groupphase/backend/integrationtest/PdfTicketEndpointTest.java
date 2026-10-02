package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PdfTicketEndpointTest implements TestData {

    private static final String TICKETS_BASE_URI = BASE_URI + "/tickets";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private ApplicationUser testUser;

    @BeforeEach
    public void beforeEach() {
        invoiceRepository.deleteAll();
        ticketRepository.deleteAll();
        orderRepository.deleteAll();
        performanceRepository.deleteAll();
        eventRepository.deleteAll();
        artistRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();

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
        invoiceRepository.deleteAll();
        ticketRepository.deleteAll();
        orderRepository.deleteAll();
        performanceRepository.deleteAll();
        eventRepository.deleteAll();
        artistRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Ticket setupPurchasedTicket() {
        Venue venue = new Venue();
        venue.setName("Test Venue");
        venue.setStreet("Street"); venue.setCity("City"); venue.setZipCode("1234"); venue.setCountry("AT");
        venue = venueRepository.save(venue);

        Hall hall = new Hall();
        hall.setName("Test Hall");
        hall.setWidth(10); hall.setLength(10);
        hall.setVenue(venue);
        hall = hallRepository.save(hall);

        Sector sector = new Sector();
        sector.setName("Test Sector");
        sector.setType(SectorType.STANDING);
        sector.setCapacity(100);
        sector.setHall(hall);
        sector.setColor("Blue");
        sector = sectorRepository.save(sector);

        Artist artist = new Artist();
        artist.setArtistName("Test Artist");
        artist.setFirstName("Test");
        artist.setLastName("Artist");
        artist = artistRepository.save(artist);

        Event event = new Event();
        event.setTitle("Test Event");
        event.setGenre("Pop");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        event = eventRepository.save(event);

        Performance performance = new Performance();
        performance.setPerformanceName("Test Performance");
        performance.setStartTime(LocalDateTime.now().plusDays(1));
        performance.setEndTime(performance.getStartTime().plusHours(2));
        performance.setStartPrice(BigDecimal.TEN);
        performance.setHall(hall);
        performance.setEvent(event);
        performance.setArtists(new HashSet<>(Set.of(artist)));
        performance = performanceRepository.save(performance);

        Order order = new Order();
        order.setUser(testUser);
        order.setPurchaseDate(LocalDateTime.now());
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.PURCHASED);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setPerformance(performance);
        ticket.setSector(sector);
        ticket.setOrder(order);
        ticket.setQrCode("O1-T1-TESTCODE");
        return ticketRepository.save(ticket);
    }

    @Test
    public void getTicketPdfReturns200AndPdfForPurchasedTicket() throws Exception {
        Ticket ticket = setupPurchasedTicket();

        MvcResult mvcResult = this.mockMvc.perform(get(TICKETS_BASE_URI + "/" + ticket.getId() + "/pdf")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("application/pdf", response.getContentType());
        assertTrue(response.getContentAsByteArray().length > 0);
        assertTrue(Objects.requireNonNull(response.getHeader("Content-Disposition")).contains("ticket-" + ticket.getId()));
    }

    @Test
    public void getTicketPdfReturns404WhenTicketNotFound() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(TICKETS_BASE_URI + "/99999/pdf")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andDo(print())
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), mvcResult.getResponse().getStatus());
    }
}