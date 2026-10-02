package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummarySeatholdDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatHoldCreateDto;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.service.SeatHoldService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SeatHoldEndpointTest implements TestData {

    private static final String HOLDS_BASE_URI = BASE_URI + "/holds";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private SeatHoldService seatHoldService;

    private ApplicationUser testUser;

    @BeforeEach
    public void beforeEach() {
        ticketRepository.deleteAll();
        seatHoldRepository.deleteAll();
        orderRepository.deleteAll();
        performanceRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        seatRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        artistRepository.deleteAll();

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
        ticketRepository.deleteAll();
        seatHoldRepository.deleteAll();
        orderRepository.deleteAll();
        performanceRepository.deleteAll();
        userRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
    }


    @Test
    public void cancelAllHoldsDeletesOnlyUserHoldsForSelectedPerformance() throws Exception {
        Performance perf1 = setupFullHierarchy("Perf 1");
        Performance perf2 = setupFullHierarchy("Perf 2");

        // Sektoren sicher aus DB laden
        Sector sector1 = sectorRepository.findAll().stream()
            .filter(s -> s.getHall().getId().equals(perf1.getHall().getId())).findFirst().get();
        Sector sector2 = sectorRepository.findAll().stream()
            .filter(s -> s.getHall().getId().equals(perf2.getHall().getId())).findFirst().get();

        ApplicationUser otherUser = new ApplicationUser("Other", "User", "other@test.at", "password", UserRole.ROLE_USER);
        userRepository.save(otherUser);

        SeatHold hold1 = createCustomHold(perf1, sector1, testUser);
        SeatHold hold2 = createCustomHold(perf2, sector2, testUser);
        SeatHold hold3 = createCustomHold(perf1, sector1, otherUser);

        this.mockMvc.perform(delete(HOLDS_BASE_URI + "/cancel-all/{performanceId}", perf1.getId())
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andExpect(result -> assertEquals(HttpStatus.NO_CONTENT.value(), result.getResponse().getStatus()));

        assertAll(
            () -> assertFalse(seatHoldRepository.existsById(hold1.getId()), "Hold for current user and perf1 should be deleted"),
            () -> assertTrue(seatHoldRepository.existsById(hold2.getId()), "Hold for different performance should persist"),
            () -> assertTrue(seatHoldRepository.existsById(hold3.getId()), "Hold for different user should persist")
        );
    }

    private SeatHold createCustomHold(Performance perf, Sector sector, ApplicationUser user) {
        SeatHold hold = new SeatHold();
        hold.setPerformance(perf);
        hold.setSector(sector);
        hold.setUser(user);
        hold.setQuantity(1);
        hold.setActive(true);
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        return seatHoldRepository.save(hold);
    }

    @Test
    public void createHoldSucceedsForSeatingAndStanding() throws Exception {
        Performance perf = setupFullHierarchy("Performance A");

        Sector seatingSector = sectorRepository.findAll().stream()
            .filter(s -> s.getType() == SectorType.SEATING).findFirst().get();
        Seat seat = seatRepository.findAll().stream()
            .filter(s -> s.getSector().getId().equals(seatingSector.getId())).findFirst().get();

        Sector standingSector = sectorRepository.findAll().stream()
            .filter(s -> s.getType() == SectorType.STANDING).findFirst().get();

        SeatHoldCreateDto.HoldItemRequest item1 = new SeatHoldCreateDto.HoldItemRequest(seatingSector.getId(), seat.getId(), 1);
        SeatHoldCreateDto.HoldItemRequest item2 = new SeatHoldCreateDto.HoldItemRequest(standingSector.getId(), null, 2);

        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item1, item2));
        String body = jsonMapper.writeValueAsString(dto);

        MvcResult mvcResult = this.mockMvc.perform(post(HOLDS_BASE_URI + "/{id}", perf.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.CREATED.value(), mvcResult.getResponse().getStatus());
        List<Long> holdIds = jsonMapper.readValue(mvcResult.getResponse().getContentAsString(),
            jsonMapper.getTypeFactory().constructCollectionType(List.class, Long.class));

        assertEquals(2, holdIds.size());
        assertEquals(2, seatHoldRepository.count());
    }

    @Test
    public void createHoldReturnsUnprocessableEntityWhenSeatAlreadySold() throws Exception {
        Performance perf = setupFullHierarchy("Conflict Perf");
        Sector sector = sectorRepository.findAll().get(0);
        Seat seat = seatRepository.findAll().get(0);

        Ticket ticket = new Ticket();
        ticket.setPerformance(perf);
        ticket.setSector(sector);
        ticket.setSeat(seat);
        ticket.setFinalPrice(BigDecimal.TEN);
        ticket.setStatus(TicketStatus.PURCHASED);
        ticketRepository.save(ticket);

        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(sector.getId(), seat.getId(), 1);
        String body = jsonMapper.writeValueAsString(new SeatHoldCreateDto(List.of(item)));

        this.mockMvc.perform(post(HOLDS_BASE_URI + "/{id}", perf.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andExpect(mvcResult -> assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), mvcResult.getResponse().getStatus()));
    }

    @Test
    public void getCheckoutSummaryReturnsCorrectMetadata() throws Exception {
        Performance perf = setupFullHierarchy("Summary Perf");
        Sector sector = sectorRepository.findAll().get(0);

        SeatHold hold = new SeatHold();
        hold.setPerformance(perf);
        hold.setSector(sector);
        hold.setUser(testUser);
        hold.setQuantity(2);
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        seatHoldRepository.save(hold);

        MvcResult mvcResult = this.mockMvc.perform(get(HOLDS_BASE_URI + "/{id}", perf.getId())
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        CheckoutSummarySeatholdDto summary = jsonMapper.readValue(mvcResult.getResponse().getContentAsString(), CheckoutSummarySeatholdDto.class);

        assertAll(
            () -> assertEquals("Test Event", summary.getEventName()),
            () -> assertEquals("Summary Perf Venue", summary.getVenueName()),
            () -> assertEquals(1, summary.getArtists().size()),
            () -> assertEquals(new BigDecimal("20.00"), summary.getTotalAmount())
        );
    }

    @Test
    public void releaseHoldDeletesSpecificHold() throws Exception {
        Performance perf = setupFullHierarchy("Release Perf");
        SeatHold hold = new SeatHold();
        hold.setPerformance(perf);
        hold.setSector(sectorRepository.findAll().get(0));
        hold.setUser(testUser);
        hold.setQuantity(1);
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        hold = seatHoldRepository.save(hold);

        this.mockMvc.perform(delete(HOLDS_BASE_URI + "/{id}", hold.getId())
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andExpect(mvcResult -> assertEquals(HttpStatus.NO_CONTENT.value(), mvcResult.getResponse().getStatus()));

        assertFalse(seatHoldRepository.existsById(hold.getId()));
    }

    @Test
    public void concurrentCreateHoldForSameSeatOnlySucceedsOnce() throws Exception {
        Performance perf = setupFullHierarchy("Concurrent Perf");
        Sector sector = sectorRepository.findAll().stream()
            .filter(s -> s.getType() == SectorType.SEATING).findFirst().get();
        Seat seat = seatRepository.findAll().stream()
            .filter(s -> s.getSector().getId().equals(sector.getId())).findFirst().get();

        ApplicationUser user2 = new ApplicationUser();
        user2.setFirstName("Second");
        user2.setLastName("User");
        user2.setEmail("second@test.at");
        user2.setPassword("Password123!");
        user2.setRole(UserRole.ROLE_USER);
        user2 = userRepository.save(user2);

        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(sector.getId(), seat.getId(), 1);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        List<String> emails = List.of(testUser.getEmail(), user2.getEmail());
        for (int i = 0; i < threadCount; i++) {
            String email = emails.get(i);
            futures.add(executor.submit(() -> {
                latch.await();
                try {
                    seatHoldService.createHold(perf.getId(), email, dto);
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
        assertEquals(1, successCount, "Nur einer der parallelen Holds darf erfolgreich sein. Ergebnisse: " + results);

        assertEquals(1, seatHoldRepository.count(), "Es darf nur genau ein SeatHold existieren");
    }

    private Performance setupFullHierarchy(String venueName) {
        Venue venue = new Venue();
        venue.setName(venueName + " Venue");
        venue.setStreet("Street 1");
        venue.setCity("Vienna");
        venue.setZipCode("1010");
        venue.setCountry("AT");
        venue = venueRepository.save(venue);

        Hall hall = new Hall();
        hall.setName("Test Hall");
        hall.setWidth(10); hall.setLength(10);
        hall.setVenue(venue);
        hall = hallRepository.save(hall);

        Sector s1 = new Sector();
        s1.setName("Seating");
        s1.setType(SectorType.SEATING);
        s1.setHall(hall);
        s1.setColor("TestFarbe");
        sectorRepository.save(s1);

        Seat seat = new Seat();
        seat.setSector(s1); seat.setRowNumber(1); seat.setSeatNumber(1); seat.setPositionX(0); seat.setPositionY(0);
        seatRepository.save(seat);

        Sector s2 = new Sector();
        s2.setName("Standing"); s2.setType(SectorType.STANDING); s2.setCapacity(100); s2.setHall(hall);
        s2.setColor("TestFarbe2");
        sectorRepository.save(s2);

        Event event = new Event();
        event.setTitle("Test Event");
        event.setGenre("Genre");
        event.setStartTime(LocalDateTime.now());
        event.setEndTime(LocalDateTime.now().plusHours(2));
        event = eventRepository.save(event);

        Artist artist = new Artist();
        artist.setArtistName("Artist for " + venueName);
        artist.setFirstName("A"); artist.setLastName("B");
        artist = artistRepository.save(artist);


        Performance perf = new Performance();
        perf.setPerformanceName("Seat Hold Performance " + venueName);
        perf.setEvent(event);
        perf.setHall(hall);
        perf.setStartTime(LocalDateTime.now().plusDays(1));
        perf.setEndTime(perf.getStartTime().plusHours(2));
        perf.setStartPrice(BigDecimal.TEN);
        perf.setArtists(Set.of(artist));
        return performanceRepository.save(perf);
    }
}
