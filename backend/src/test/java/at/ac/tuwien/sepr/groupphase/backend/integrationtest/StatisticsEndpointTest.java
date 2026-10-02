package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventMonthlySalesDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.OrderStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.PaymentMethod;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.TicketStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.OrderRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class StatisticsEndpointTest implements TestData {

    private static final String STATISTICS_BASE_URI = BASE_URI + "/statistics";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    private ApplicationUser testUser;
    private Venue testVenue;
    private Hall testHall;
    private Sector testSector;

    @BeforeEach
    public void beforeEach() {
        ticketRepository.deleteAll();
        orderRepository.deleteAll();
        performanceRepository.deleteAll();
        eventRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(
            new ApplicationUser("Test", "User", "test@statistics.com", "password", UserRole.ROLE_USER)
        );
        testVenue = persistVenue("Statistics Venue");
        testHall = persistHall("Statistics Hall", testVenue);
        testSector = persistSector("Floor", testHall);
    }

    @AfterEach
    public void afterEach() {
        ticketRepository.deleteAll();
        orderRepository.deleteAll();
        performanceRepository.deleteAll();
        eventRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void getMonthlyEventSalesReturns200WithValidData() throws Exception {
        Event rockEvent = persistEvent("Rock Concert", "Rock");
        Event jazzEvent = persistEvent("Jazz Night", "Jazz");
        Performance rockPerformance = persistPerformance(rockEvent, testHall);
        Performance jazzPerformance = persistPerformance(jazzEvent, testHall);

        LocalDateTime purchaseDate = LocalDateTime.of(2026, 6, 5, 10, 0);
        Order order = persistOrder(purchaseDate);

        persistTicket(rockPerformance, order, TicketStatus.PURCHASED);
        persistTicket(rockPerformance, order, TicketStatus.PURCHASED);
        persistTicket(jazzPerformance, order, TicketStatus.USED);

        MvcResult mvcResult = this.mockMvc.perform(get(STATISTICS_BASE_URI + "/event-sales")
                .param("year", "2026")
                .param("month", "6"))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        EventMonthlySalesDto result = jsonMapper.readValue(response.getContentAsString(), EventMonthlySalesDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertEquals(2026, result.getYear()),
            () -> assertEquals(6, result.getMonth()),
            () -> assertNotNull(result.getTopOverall()),
            () -> assertEquals(2, result.getTopOverall().size()),
            () -> assertEquals("Rock Concert", result.getTopOverall().get(0).getEventTitle()),
            () -> assertEquals(2L, result.getTopOverall().get(0).getTicketsSold()),
            () -> assertEquals("Jazz Night", result.getTopOverall().get(1).getEventTitle()),
            () -> assertEquals(1L, result.getTopOverall().get(1).getTicketsSold()),
            () -> assertNotNull(result.getTopByGenre()),
            () -> assertTrue(result.getTopByGenre().containsKey("Rock")),
            () -> assertTrue(result.getTopByGenre().containsKey("Jazz"))
        );
    }

    @Test
    public void getMonthlyEventSalesFiltersByGenres() throws Exception {
        Event rockEvent = persistEvent("Rock Concert", "Rock");
        Event jazzEvent = persistEvent("Jazz Night", "Jazz");
        Performance rockPerformance = persistPerformance(rockEvent, testHall);
        Performance jazzPerformance = persistPerformance(jazzEvent, testHall);

        LocalDateTime purchaseDate = LocalDateTime.of(2026, 6, 5, 10, 0);
        Order order = persistOrder(purchaseDate);

        persistTicket(rockPerformance, order, TicketStatus.PURCHASED);
        persistTicket(jazzPerformance, order, TicketStatus.PURCHASED);

        MvcResult mvcResult = this.mockMvc.perform(get(STATISTICS_BASE_URI + "/event-sales")
                .param("year", "2026")
                .param("month", "6")
                .param("genres", "Rock"))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        EventMonthlySalesDto result = jsonMapper.readValue(response.getContentAsString(), EventMonthlySalesDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(1, result.getTopByGenre().size()),
            () -> assertTrue(result.getTopByGenre().containsKey("Rock")),
            () -> assertEquals(1, result.getTopByGenre().get("Rock").size()),
            () -> assertEquals("Rock Concert", result.getTopByGenre().get("Rock").getFirst().getEventTitle())
        );
    }

    @Test
    public void getMonthlyEventSalesReturns200WithoutAuthentication() throws Exception {
        Event event = persistEvent("Public Event", "Rock");
        Performance performance = persistPerformance(event, testHall);
        Order order = persistOrder(LocalDateTime.of(2026, 6, 5, 10, 0));
        persistTicket(performance, order, TicketStatus.PURCHASED);

        MvcResult mvcResult = this.mockMvc.perform(get(STATISTICS_BASE_URI + "/event-sales")
                .param("year", "2026")
                .param("month", "6"))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    public void getMonthlyEventSalesReturns200WhenNoParamsProvided() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(STATISTICS_BASE_URI + "/event-sales"))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        EventMonthlySalesDto result = jsonMapper.readValue(response.getContentAsString(), EventMonthlySalesDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertNotNull(result.getYear()),
            () -> assertNotNull(result.getMonth()),
            () -> assertNotNull(result.getTopOverall()),
            () -> assertNotNull(result.getTopByGenre())
        );
    }

    @Test
    public void getMonthlyEventSalesExcludesCancelledAndReservedTickets() throws Exception {
        Event event = persistEvent("Main Event", "Rock");
        Performance performance = persistPerformance(event, testHall);
        Order order = persistOrder(LocalDateTime.of(2026, 6, 5, 10, 0));

        persistTicket(performance, order, TicketStatus.PURCHASED);
        persistTicket(performance, order, TicketStatus.CANCELLED);
        persistTicket(performance, order, TicketStatus.RESERVED);

        MvcResult mvcResult = this.mockMvc.perform(get(STATISTICS_BASE_URI + "/event-sales")
                .param("year", "2026")
                .param("month", "6"))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        EventMonthlySalesDto result = jsonMapper.readValue(response.getContentAsString(), EventMonthlySalesDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(1, result.getTopOverall().size()),
            () -> assertEquals(1L, result.getTopOverall().getFirst().getTicketsSold())
        );
    }

    private Venue persistVenue(String name) {
        Venue venue = new Venue();
        venue.setName(name);
        venue.setStreet("Test Street");
        venue.setCity("Vienna");
        venue.setCountry("AT");
        venue.setZipCode("1010");
        return venueRepository.save(venue);
    }

    private Hall persistHall(String name, Venue venue) {
        Hall hall = new Hall();
        hall.setName(name);
        hall.setWidth(30);
        hall.setLength(20);
        hall.setVenue(venue);
        return hallRepository.save(hall);
    }

    private Sector persistSector(String name, Hall hall) {
        Sector sector = new Sector();
        sector.setName(name);
        sector.setType(SectorType.SEATING);
        sector.setColor("#FF0000");
        sector.setHall(hall);
        return sectorRepository.save(sector);
    }

    private Event persistEvent(String title, String genre) {
        Event event = new Event();
        event.setTitle(title);
        event.setGenre(genre);
        event.setDescription("Description for " + title);
        event.setStartTime(LocalDateTime.of(2026, 6, 1, 18, 0));
        event.setEndTime(LocalDateTime.of(2026, 6, 1, 23, 0));
        return eventRepository.save(event);
    }

    private Performance persistPerformance(Event event, Hall hall) {
        Performance performance = new Performance();
        performance.setPerformanceName(event.getTitle() + " Performance");
        performance.setStartTime(event.getStartTime());
        performance.setEndTime(event.getEndTime());
        performance.setStartPrice(new BigDecimal("50.00"));
        performance.setHall(hall);
        performance.setEvent(event);
        return performanceRepository.save(performance);
    }

    private Order persistOrder(LocalDateTime purchaseDate) {
        Order order = new Order();
        order.setPurchaseDate(purchaseDate);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.PURCHASED);
        order.setTotalPrice(new BigDecimal("100.00"));
        order.setUser(testUser);
        return orderRepository.save(order);
    }

    private Ticket persistTicket(Performance performance, Order order, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setStatus(status);
        ticket.setFinalPrice(new BigDecimal("50.00"));
        ticket.setPerformance(performance);
        ticket.setSector(testSector);
        ticket.setOrder(order);
        return ticketRepository.save(ticket);
    }
}
