package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceHallLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceSectorPriceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatStatusDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallAreaType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.OrderStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.PaymentMethod;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeatHold;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.TicketStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallAreaRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.OrderRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatHoldRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PerformanceEndpointTest implements TestData {

    private static final String PERFORMANCE_BASE_URI = BASE_URI + "/performances";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private HallAreaRepository hallAreaRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @BeforeEach
    public void beforeEach() {
        ticketRepository.deleteAll();
        seatHoldRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        performanceRepository.deleteAll();
        hallAreaRepository.deleteAll();
        seatRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        artistRepository.deleteAll();
    }

    @AfterEach
    public void afterEach() {
        ticketRepository.deleteAll();
        seatHoldRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        performanceRepository.deleteAll();
        hallAreaRepository.deleteAll();
        seatRepository.deleteAll();
        sectorRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        artistRepository.deleteAll();
    }

    @Test
    public void postPerformanceCreatesPerformanceWithArtistsWhenPayloadIsValid() throws Exception {
        Artist artist = new Artist();
        artist.setFirstName("First");
        artist.setLastName("Last");
        artist.setArtistName("StageName");
        Artist savedArtist = artistRepository.save(artist);

        PerformanceCreateDto createDto = new PerformanceCreateDto();
        createDto.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 15));
        createDto.setDurationHours(2);
        createDto.setDurationMinutes(0);
        createDto.setPerformanceName("Stage Performance");
        createDto.setStartPrice(new BigDecimal("44.50"));
        Hall hall = persistHall("Main Hall");
        createDto.setHallId(hall.getId());
        createDto.setArtistIds(List.of(savedArtist.getId()));
        String body = jsonMapper.writeValueAsString(createDto);

        MvcResult mvcResult = this.mockMvc.perform(post(PERFORMANCE_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        PerformanceDetailDto responseBody = jsonMapper.readValue(response.getContentAsString(), PerformanceDetailDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.CREATED.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertNotNull(responseBody.getId()),
            () -> assertEquals(LocalDateTime.of(2026, 10, 1, 20, 15), responseBody.getStartTime()),
            () -> assertEquals(new BigDecimal("44.50"), responseBody.getStartPrice()),
            () -> assertEquals(hall.getId(), responseBody.getHallId()),
            () -> assertEquals("Main Hall", responseBody.getHallName()),
            () -> assertEquals(1, responseBody.getArtistIds().size()),
            () -> assertEquals(savedArtist.getId(), responseBody.getArtistIds().get(0))
        );
    }

    @Test
    public void postPerformanceReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        PerformanceCreateDto createDto = new PerformanceCreateDto();
        createDto.setStartTime(null);
        createDto.setDurationHours(1);
        createDto.setDurationMinutes(0);
        createDto.setStartPrice(new BigDecimal("-1.00"));
        createDto.setHallId(null);
        String body = jsonMapper.writeValueAsString(createDto);

        MvcResult mvcResult = this.mockMvc.perform(post(PERFORMANCE_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void postPerformanceReturnsBadRequestWhenHallIdIsMissing() throws Exception {
        Artist artist = new Artist();
        artist.setFirstName("First");
        artist.setLastName("Last");
        artist.setArtistName("StageName");
        Artist savedArtist = artistRepository.save(artist);

        PerformanceCreateDto createDto = new PerformanceCreateDto();
        createDto.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 15));
        createDto.setDurationHours(2);
        createDto.setDurationMinutes(0);
        createDto.setPerformanceName("Stage Performance");
        createDto.setStartPrice(new BigDecimal("44.50"));
        createDto.setHallId(null);
        createDto.setArtistIds(List.of(savedArtist.getId()));
        String body = jsonMapper.writeValueAsString(createDto);

        MvcResult mvcResult = this.mockMvc.perform(post(PERFORMANCE_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void postPerformancePersistsAndReturnsSectorPrices() throws Exception {
        Artist artist = new Artist();
        artist.setFirstName("First");
        artist.setLastName("Last");
        artist.setArtistName("StageName");
        Artist savedArtist = artistRepository.save(artist);

        Hall hall = persistHall("Main Hall");
        PerformanceCreateDto createDto = new PerformanceCreateDto();
        createDto.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 15));
        createDto.setDurationHours(2);
        createDto.setDurationMinutes(0);
        createDto.setPerformanceName("Stage Performance");
        createDto.setStartPrice(new BigDecimal("44.50"));
        createDto.setHallId(hall.getId());
        createDto.setArtistIds(List.of(savedArtist.getId()));

        PerformanceSectorPriceDto sectorPrice = new PerformanceSectorPriceDto();
        sectorPrice.setSectorId(77L);
        sectorPrice.setSectorName("Standing A");
        sectorPrice.setSectorType("STANDING");
        sectorPrice.setPrice(new BigDecimal("59.90"));
        createDto.setSectorPrices(List.of(sectorPrice));

        String body = jsonMapper.writeValueAsString(createDto);
        MvcResult mvcResult = this.mockMvc.perform(post(PERFORMANCE_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        PerformanceDetailDto responseBody = jsonMapper.readValue(response.getContentAsString(), PerformanceDetailDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.CREATED.value(), response.getStatus()),
            () -> assertNotNull(responseBody.getSectorPrices()),
            () -> assertEquals(1, responseBody.getSectorPrices().size()),
            () -> assertEquals(77L, responseBody.getSectorPrices().get(0).getSectorId()),
            () -> assertEquals("Standing A", responseBody.getSectorPrices().get(0).getSectorName()),
            () -> assertEquals("STANDING", responseBody.getSectorPrices().get(0).getSectorType()),
            () -> assertEquals(new BigDecimal("59.90"), responseBody.getSectorPrices().get(0).getPrice())
        );
    }

    @Test
    public void postPerformanceReturnsBadRequestWhenArtistIdsAreEmpty() throws Exception {
        PerformanceCreateDto createDto = new PerformanceCreateDto();
        createDto.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 15));
        createDto.setDurationHours(2);
        createDto.setDurationMinutes(0);
        createDto.setPerformanceName("Stage Performance");
        createDto.setStartPrice(new BigDecimal("44.50"));
        Hall hall = persistHall("Main Hall");
        createDto.setHallId(hall.getId());
        createDto.setArtistIds(List.of());
        String body = jsonMapper.writeValueAsString(createDto);

        MvcResult mvcResult = this.mockMvc.perform(post(PERFORMANCE_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void deletePerformanceReturnsNoContentWhenPerformanceExists() throws Exception {
        Performance performance = new Performance();
        Hall hall = persistHall("Delete Hall");
        performance.setPerformanceName("Delete Test Performance");
        performance.setStartTime(LocalDateTime.of(2026, 11, 2, 19, 0));
        performance.setEndTime(LocalDateTime.of(2026, 11, 2, 21, 0));
        performance.setStartPrice(new BigDecimal("30.00"));
        performance.setHall(hall);
        Performance savedPerformance = performanceRepository.save(performance);

        MvcResult mvcResult = this.mockMvc.perform(delete(PERFORMANCE_BASE_URI + "/{id}", savedPerformance.getId())
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus()),
            () -> assertFalse(performanceRepository.existsById(savedPerformance.getId()))
        );
    }

    @Test
    public void deletePerformanceReturnsNotFoundWhenPerformanceDoesNotExist() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(delete(PERFORMANCE_BASE_URI + "/{id}", 9999L)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus()),
            () -> assertTrue(response.getContentAsString().contains("Could not find performance with id 9999"))
        );
    }

    @Test
    public void getPerformancesByIdsReturnsDetailedPerformanceData() throws Exception {
        Artist artistOne = new Artist();
        artistOne.setFirstName("Bruce");
        artistOne.setLastName("Wayne");
        artistOne.setArtistName("Batman Live");
        artistOne = artistRepository.save(artistOne);

        Artist artistTwo = new Artist();
        artistTwo.setFirstName("Clark");
        artistTwo.setLastName("Kent");
        artistTwo.setArtistName("Superman Live");
        artistTwo = artistRepository.save(artistTwo);

        Hall hall = persistHall("Hero Hall");
        Performance performance = new Performance();
        performance.setPerformanceName("Hero Clash");
        performance.setStartTime(LocalDateTime.of(2026, 11, 15, 20, 30));
        performance.setEndTime(LocalDateTime.of(2026, 11, 15, 22, 30));
        performance.setStartPrice(new BigDecimal("55.00"));
        performance.setHall(hall);
        performance.setArtists(new HashSet<>(List.of(artistOne, artistTwo)));

        PerformanceSectorPrice standingPrice = new PerformanceSectorPrice();
        standingPrice.setSectorId(101L);
        standingPrice.setSectorName("Standing A");
        standingPrice.setSectorType("STANDING");
        standingPrice.setPrice(new BigDecimal("55.00"));
        standingPrice.setPerformance(performance);
        performance.setSectorPrices(List.of(standingPrice));

        Performance savedPerformance = performanceRepository.save(performance);

        MvcResult mvcResult = this.mockMvc.perform(get(PERFORMANCE_BASE_URI)
            .param("ids", savedPerformance.getId().toString())
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(DEFAULT_USER, USER_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        List<PerformanceDetailDto> responseBody = Arrays.asList(jsonMapper.readValue(response.getContentAsString(), PerformanceDetailDto[].class));

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(1, responseBody.size()),
            () -> assertEquals(savedPerformance.getId(), responseBody.get(0).getId()),
            () -> assertEquals("Hero Clash", responseBody.get(0).getPerformanceName()),
            () -> assertEquals(hall.getId(), responseBody.get(0).getHallId()),
            () -> assertEquals("Hero Hall", responseBody.get(0).getHallName()),
            () -> assertEquals(2, responseBody.get(0).getArtistNames().size()),
            () -> assertTrue(responseBody.get(0).getArtistNames().contains("Batman Live")),
            () -> assertTrue(responseBody.get(0).getArtistNames().contains("Superman Live")),
            () -> assertTrue(responseBody.get(0).getSectorPrices().size() >= 1),
            () -> assertTrue(responseBody.get(0).getSectorPrices().stream().anyMatch(sp -> "Standing A".equals(sp.getSectorName())))
        );
    }

    @Test
    public void getPerformancesByIdsReturnsPerformanceWithManyArtists() throws Exception {
        Hall hall = persistHall("All Star Hall");

        Artist batman = createArtist("Bruce", "Wayne", "Batman Live");
        Artist superman = createArtist("Clark", "Kent", "Superman Live");
        Artist wonderWoman = createArtist("Diana", "Prince", "Wonder Woman Live");
        Artist spiderMan = createArtist("Peter", "Parker", "Spider-Man Live");
        Artist ironMan = createArtist("Tony", "Stark", "Iron Man Live");
        Artist captainAmerica = createArtist("Steve", "Rogers", "Captain America Live");
        Artist blackWidow = createArtist("Natasha", "Romanoff", "Black Widow Live");
        Artist hulk = createArtist("Bruce", "Banner", "Hulk Live");
        Artist flash = createArtist("Barry", "Allen", "Flash Live");
        Artist aquaman = createArtist("Arthur", "Curry", "Aquaman Live");
        Artist thor = createArtist("Thor", "Odinson", "Thor Live");
        Artist loki = createArtist("Loki", "Laufeyson", "Loki Live");
        Artist greenLantern = createArtist("Hal", "Jordan", "Green Lantern Live");
        Artist robin = createArtist("Dick", "Grayson", "Robin Live");
        Artist nightwing = createArtist("Dick", "Grayson", "Nightwing Live");
        Artist catwoman = createArtist("Selina", "Kyle", "Catwoman Live");
        Artist joker = createArtist("Jack", "Napier", "Joker Live");
        Artist deadpool = createArtist("Wade", "Wilson", "Deadpool Live");
        Artist doctorStrange = createArtist("Stephen", "Strange", "Doctor Strange Live");
        Artist blackPanther = createArtist("T'Challa", "Udaku", "Black Panther Live");

        Performance performance = new Performance();
        performance.setPerformanceName("All Star Finale");
        performance.setStartTime(LocalDateTime.of(2027, 1, 15, 21, 0));
        performance.setEndTime(LocalDateTime.of(2027, 1, 15, 23, 0));
        performance.setStartPrice(new BigDecimal("89.90"));
        performance.setHall(hall);
        performance.setArtists(new HashSet<>(List.of(
            batman, superman, wonderWoman, spiderMan, ironMan, captainAmerica, blackWidow, hulk, flash, aquaman,
            thor, loki, greenLantern, robin, nightwing, catwoman, joker, deadpool, doctorStrange, blackPanther
        )));

        Performance savedPerformance = performanceRepository.save(performance);

        MvcResult mvcResult = this.mockMvc.perform(get(PERFORMANCE_BASE_URI)
            .param("ids", savedPerformance.getId().toString())
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(DEFAULT_USER, USER_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        List<PerformanceDetailDto> responseBody = Arrays.asList(jsonMapper.readValue(response.getContentAsString(), PerformanceDetailDto[].class));

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(1, responseBody.size()),
            () -> assertEquals("All Star Finale", responseBody.get(0).getPerformanceName()),
            () -> assertEquals(20, responseBody.get(0).getArtistNames().size()),
            () -> assertTrue(responseBody.get(0).getArtistNames().contains("Batman Live")),
            () -> assertTrue(responseBody.get(0).getArtistNames().contains("Black Panther Live"))
        );
    }

    @Test
    public void postPerformanceReturnsBadRequestWhenArtistHasOverlappingPerformance() throws Exception {
        Artist artist = new Artist();
        artist.setFirstName("First");
        artist.setLastName("Last");
        artist.setArtistName("StageName");
        Artist savedArtist = artistRepository.save(artist);

        Hall hall = persistHall("Main Hall");

        Performance existingPerformance = new Performance();
        existingPerformance.setPerformanceName("Existing Performance");
        existingPerformance.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 0));
        existingPerformance.setEndTime(LocalDateTime.of(2026, 10, 1, 22, 0));
        existingPerformance.setStartPrice(new BigDecimal("49.90"));
        existingPerformance.setHall(hall);
        existingPerformance.setArtists(new HashSet<>(List.of(savedArtist)));
        performanceRepository.save(existingPerformance);

        PerformanceCreateDto createDto = new PerformanceCreateDto();
        createDto.setStartTime(LocalDateTime.of(2026, 10, 1, 21, 0));
        createDto.setDurationHours(1);
        createDto.setDurationMinutes(30);
        createDto.setPerformanceName("Overlapping Performance");
        createDto.setStartPrice(new BigDecimal("44.50"));
        createDto.setHallId(hall.getId());
        createDto.setArtistIds(List.of(savedArtist.getId()));
        String body = jsonMapper.writeValueAsString(createDto);

        MvcResult mvcResult = this.mockMvc.perform(post(PERFORMANCE_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () -> assertTrue(response.getContentAsString().contains("already has a performance"))
        );
    }

    private Artist createArtist(String firstName, String lastName, String artistName) {
        Artist artist = new Artist();
        artist.setFirstName(firstName);
        artist.setLastName(lastName);
        artist.setArtistName(artistName);
        return artistRepository.save(artist);
    }

    @Test
    public void getHallLayoutReturnsComprehensiveDto() throws Exception {
        Venue venue = new Venue();
        venue.setName("Layout Venue");
        venue.setStreet("Test Street 1");
        venue.setCity("Vienna");
        venue.setZipCode("1010");
        venue.setCountry("AT");
        venue = venueRepository.save(venue);

        Hall hall = new Hall();
        hall.setName("Comprehensive Hall");
        hall.setWidth(100);
        hall.setLength(200);
        hall.setVenue(venue);
        hall = hallRepository.save(hall);

        Sector seatingSector = new Sector();
        seatingSector.setName("Premium Seating");
        seatingSector.setType(SectorType.SEATING);
        seatingSector.setColor("#FF0000");
        seatingSector.setHall(hall);
        seatingSector = sectorRepository.save(seatingSector);

        HallArea seatingArea = new HallArea();
        seatingArea.setHall(hall);
        seatingArea.setSector(seatingSector);
        seatingArea.setType(HallAreaType.SEATING);
        seatingArea.setPositionX(10); seatingArea.setPositionY(10);
        seatingArea.setWidth(50); seatingArea.setLength(50);
        hallAreaRepository.save(seatingArea);

        Sector standingSector = new Sector();
        standingSector.setName("General Standing");
        standingSector.setType(SectorType.STANDING);
        standingSector.setColor("#00FF00");
        standingSector.setCapacity(500);
        standingSector.setHall(hall);
        standingSector = sectorRepository.save(standingSector);

        HallArea standingArea = new HallArea();
        standingArea.setHall(hall);
        standingArea.setSector(standingSector);
        standingArea.setType(HallAreaType.STANDING);
        standingArea.setPositionX(10); standingArea.setPositionY(70);
        standingArea.setWidth(50); standingArea.setLength(100);
        hallAreaRepository.save(standingArea);

        HallArea stageArea = new HallArea();
        stageArea.setHall(hall);
        stageArea.setType(HallAreaType.STAGE);
        stageArea.setPositionX(70); stageArea.setPositionY(10);
        stageArea.setWidth(20); stageArea.setLength(180);
        hallAreaRepository.save(stageArea);

        Performance performance = new Performance();
        performance.setPerformanceName("Comprehensive Layout Performance");
        performance.setHall(hall);
        performance.setStartTime(LocalDateTime.now().plusDays(7));
        performance.setEndTime(performance.getStartTime().plusHours(2));
        performance.setStartPrice(new BigDecimal("40.00"));

        List<PerformanceSectorPrice> prices = new ArrayList<>();
        prices.add(createPrice(performance, seatingSector.getId(), "Premium Seating", "SEATING", "120.00"));
        prices.add(createPrice(performance, standingSector.getId(), "General Standing", "STANDING", "60.00"));
        performance.setSectorPrices(prices);
        performance = performanceRepository.save(performance);

        MvcResult mvcResult = this.mockMvc.perform(MockMvcRequestBuilders.get(PERFORMANCE_BASE_URI + "/{id}/layout", performance.getId())
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        PerformanceHallLayoutDto result = jsonMapper.readValue(mvcResult.getResponse().getContentAsString(), PerformanceHallLayoutDto.class);

        assertAll(
            () -> assertEquals("Comprehensive Hall", result.getHallName()),
            () -> assertEquals("Layout Venue", result.getVenueName()),
            () -> assertEquals("1010", result.getVenueZip()),
            () -> assertEquals(3, result.getSectors().size()),

            () -> {
                var dto = result.getSectors().stream().filter(s -> "Premium Seating".equals(s.getName())).findFirst().get();
                assertEquals(new BigDecimal("120.00"), dto.getPrice());
                assertEquals("SEATING", dto.getType());
                assertEquals(10, dto.getPositionX());
                assertEquals(50, dto.getWidth());
            },

            () -> {
                var dto = result.getSectors().stream().filter(s -> "General Standing".equals(s.getName())).findFirst().get();
                assertEquals(new BigDecimal("60.00"), dto.getPrice());
                assertEquals(500, dto.getCapacity());
            },

            () -> {
                var dto = result.getSectors().stream().filter(s -> "Stage".equals(s.getName())).findFirst().get();
                assertNull(dto.getSectorId());
                assertEquals(BigDecimal.ZERO, dto.getPrice());
                assertEquals("STAGE", dto.getType());
                assertEquals(70, dto.getPositionX());
            }
        );
    }

    @Test
    public void getSeatsForSectorReturnsCorrectStatusesAndSorting() throws Exception {
        Venue venue = new Venue();
        venue.setName("Seat Venue");
        venue.setStreet("Street"); venue.setCity("City"); venue.setZipCode("123"); venue.setCountry("AT");
        venue = venueRepository.save(venue);

        Hall hall = new Hall();
        hall.setName("Seat Hall");
        hall.setWidth(10); hall.setLength(10);
        hall.setVenue(venue);
        hall = hallRepository.save(hall);

        Sector sector = new Sector();
        sector.setName("Status Sector");
        sector.setType(SectorType.SEATING);
        sector.setHall(hall);
        sector.setColor("Farbe");
        sector = sectorRepository.save(sector);

        Seat s1 = createSeat(sector, 1, 1, 0, 0);
        Seat s2 = createSeat(sector, 1, 2, 10, 0);
        Seat s3 = createSeat(sector, 2, 1, 0, 10);

        Performance perf = new Performance();
        perf.setPerformanceName("Seat Status Performance");
        perf.setHall(hall);
        perf.setStartTime(LocalDateTime.now().plusDays(1));
        perf.setEndTime(perf.getStartTime().plusHours(2));
        perf.setStartPrice(new BigDecimal("50.00"));
        perf = performanceRepository.save(perf);


        ApplicationUser other = userRepository.save(new ApplicationUser("Other", "User", "other@test.at", "password", UserRole.ROLE_USER));
        ApplicationUser current = userRepository.save(new ApplicationUser("Current", "User", USER_AUTH_EMAIL, "password", UserRole.ROLE_USER));

        Order order = new Order();
        order.setUser(other); order.setPurchaseDate(LocalDateTime.now());
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD); order.setStatus(OrderStatus.PURCHASED);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        Ticket t = new Ticket();
        t.setPerformance(perf); t.setSector(sector); t.setSeat(s1);
        t.setFinalPrice(BigDecimal.TEN); t.setStatus(TicketStatus.PURCHASED); t.setOrder(order);
        ticketRepository.save(t);

        SeatHold holdOther = new SeatHold();
        holdOther.setPerformance(perf); holdOther.setUser(other); holdOther.setSector(sector); holdOther.setSeat(s2);
        holdOther.setQuantity(1); holdOther.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        seatHoldRepository.save(holdOther);

        SeatHold holdCurrent = new SeatHold();
        holdCurrent.setPerformance(perf); holdCurrent.setUser(current); holdCurrent.setSector(sector); holdCurrent.setSeat(s3);
        holdCurrent.setQuantity(1); holdCurrent.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        seatHoldRepository.save(holdCurrent);

        MvcResult mvcResult = this.mockMvc.perform(MockMvcRequestBuilders.get(PERFORMANCE_BASE_URI + "/{id}/sectors/{sectorId}/seats", perf.getId(), sector.getId())
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(USER_AUTH_EMAIL, USER_ROLES)))
            .andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
        List<SeatStatusDto> result = jsonMapper.readValue(mvcResult.getResponse().getContentAsString(),
            jsonMapper.getTypeFactory().constructCollectionType(List.class, SeatStatusDto.class));

        assertEquals(3, result.size());

        var dto1 = result.stream().filter(s -> s.getSeatId().equals(s1.getId())).findFirst().get();
        assertEquals("SOLD", dto1.getStatus());

        var dto2 = result.stream().filter(s -> s.getSeatId().equals(s2.getId())).findFirst().get();
        assertEquals("HELD", dto2.getStatus());

        var dto3 = result.stream().filter(s -> s.getSeatId().equals(s3.getId())).findFirst().get();
        assertEquals("AVAILABLE", dto3.getStatus(), "Own hold must be marked as AVAILABLE");

        assertEquals(s1.getId(), result.get(0).getSeatId());
        assertEquals(s2.getId(), result.get(1).getSeatId());
        assertEquals(s3.getId(), result.get(2).getSeatId());
    }

    private PerformanceSectorPrice createPrice(Performance p, Long sId, String name, String type, String price) {
        PerformanceSectorPrice psp = new PerformanceSectorPrice();
        psp.setPerformance(p); psp.setSectorId(sId); psp.setSectorName(name); psp.setSectorType(type);
        psp.setPrice(new BigDecimal(price));
        return psp;
    }

    private Seat createSeat(Sector sector, int row, int num, int x, int y) {
        Seat seat = new Seat();
        seat.setSector(sector); seat.setRowNumber(row); seat.setSeatNumber(num);
        seat.setPositionX(x); seat.setPositionY(y);
        return seatRepository.save(seat);
    }

    private Hall persistHall(String hallName) {
        Venue venue = new Venue();
        venue.setName("Test Venue " + hallName);
        venue.setStreet("Street 1");
        venue.setCity("Vienna");
        venue.setCountry("AT");
        venue.setZipCode("1010");
        Venue savedVenue = venueRepository.save(venue);

        Hall hall = new Hall();
        hall.setName(hallName);
        hall.setWidth(30);
        hall.setLength(20);
        hall.setVenue(savedVenue);
        return hallRepository.save(hall);
    }
}
