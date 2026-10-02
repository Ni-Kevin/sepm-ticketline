package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventPageDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class EventEndpointTest implements TestData {

    private static final String EVENT_BASE_URI = BASE_URI + "/events";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @BeforeEach
    public void beforeEach() {
        performanceRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    public void postEventCreatesAndLinksPerformanceWhenMultipartPayloadIsValid() throws Exception {
        Performance performance = persistPerformance("Open Air Hall", "Opening Performance");
        byte[] imageBytes = "test-image".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile imagePart = new MockMultipartFile("image", "event.png", "image/png", imageBytes);

        MockHttpServletResponse response = postEvent(validEventCreateDto(performance.getId()), imagePart);

        EventDetailDto responseBody = jsonMapper.readValue(response.getContentAsString(), EventDetailDto.class);
        Performance persistedPerformance = performanceRepository.findById(performance.getId()).orElseThrow();

        assertAll(
            () -> assertEquals(HttpStatus.CREATED.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertNotNull(responseBody.getId()),
            () -> assertEquals("Summer Festival", responseBody.getTitle()),
            () -> assertEquals("Festival", responseBody.getGenre()),
            () -> assertEquals("Festival Description", responseBody.getDescription()),
            () -> assertNotNull(responseBody.getImage()),
            () -> assertEquals(imageBytes.length, responseBody.getImage().length),
            () -> assertNotNull(persistedPerformance.getEvent()),
            () -> assertEquals(responseBody.getId(), persistedPerformance.getEvent().getId())
        );
    }

    @Test
    public void postEventReturnsBadRequestWhenMultipartPayloadIsInvalid() throws Exception {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle(" ");
        createDto.setGenre(null);

        assertBadRequest(postEvent(createDto));
    }

    @Test
    public void postEventReturnsBadRequestWhenImageTypeIsInvalid() throws Exception {
        Performance performance = persistPerformance("Open Air Hall", "Opening Performance");
        MockMultipartFile imagePart = new MockMultipartFile("image", "event.gif", MediaType.IMAGE_GIF_VALUE, "gif".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = postEvent(validEventCreateDto(performance.getId()), imagePart);

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, response.getContentType()),
            () -> assertEquals(true, response.getContentAsString().contains("Only PNG, JPEG and WebP images are allowed"))
        );
    }

    @Test
    public void postEventReturnsBadRequestWhenStartTimeIsMissing() throws Exception {
        Performance performance = persistPerformance("Open Air Hall", "Opening Performance");
        EventCreateDto createDto = validEventCreateDto(performance.getId());
        createDto.setStartTime(null);

        assertBadRequest(postEvent(createDto));
    }

    @Test
    public void postEventReturnsBadRequestWhenEventEndTimeIsBeforePerformanceEndTime() throws Exception {
        Performance performance = persistPerformance("Open Air Hall", "Opening Performance");
        EventCreateDto createDto = validEventCreateDto(performance.getId());
        createDto.setEndTime(LocalDateTime.of(2026, 8, 15, 20, 59));

        assertBadRequest(postEvent(createDto));
    }

    @Test
    public void postEventReturnsBadRequestWhenPerformanceIdsAreEmpty() throws Exception {
        EventCreateDto createDto = validEventCreateDto(null);
        createDto.setPerformanceIds(List.of());

        assertBadRequest(postEvent(createDto));
    }

    @Test
    public void findAllEventsReturnsSortedByEventStartTime() throws Exception {
        Event firstEvent = persistEventWithPerformance("First Event", "Genre A", LocalDateTime.of(2026, 9, 10, 18, 0), "First Performance");
        Event secondEvent = persistEventWithPerformance("Second Event", "Genre B", LocalDateTime.of(2026, 9, 1, 17, 0), "Second Performance");
        Event thirdEvent = persistEventWithPerformance("Third Event", "Genre C", LocalDateTime.of(2026, 10, 1, 17, 0), "Third Performance");

        MockHttpServletResponse response = performWithUserAuth(get(EVENT_BASE_URI));

        List<EventDetailDto> responseBody = jsonMapper.readValue(response.getContentAsString(), EventPageDto.class).getContent();

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertEquals(3, responseBody.size()),
            () -> assertEquals(secondEvent.getId(), responseBody.get(0).getId()),
            () -> assertEquals(firstEvent.getId(), responseBody.get(1).getId()),
            () -> assertEquals(thirdEvent.getId(), responseBody.get(2).getId())
        );
    }

    @Test
    public void findAllEventsFiltersByQueryAndGenre() throws Exception {
        Event matchingEvent = persistEventWithPerformance("Summer Festival", "Concert", LocalDateTime.of(2026, 9, 10, 18, 0), "Matching Performance");
        persistEventWithPerformance("Summer Festival Late", "Theater", LocalDateTime.of(2026, 9, 11, 18, 0), "Wrong Genre Performance");
        persistEventWithPerformance("Winter Festival", "Concert", LocalDateTime.of(2026, 9, 12, 18, 0), "Wrong Title Performance");

        MockHttpServletResponse response = performWithUserAuth(get(EVENT_BASE_URI)
                .param("q", "Summer")
                .param("genre", "Concert"));

        List<EventDetailDto> responseBody = jsonMapper.readValue(response.getContentAsString(), EventPageDto.class).getContent();

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(1, responseBody.size()),
            () -> assertEquals(matchingEvent.getId(), responseBody.getFirst().getId())
        );
    }

    @Test
    public void findEventByIdReturnsEventWithPerformanceIds() throws Exception {
        Event savedEvent = persistEvent("Summer Festival", "Festival", LocalDateTime.of(2026, 8, 15, 18, 0));
        savedEvent.setDescription("Festival Description");
        eventRepository.save(savedEvent);
        Performance savedPerformance = persistPerformance(savedEvent, "Opening Performance");

        MvcResult mvcResult = this.mockMvc.perform(get(EVENT_BASE_URI + "/" + savedEvent.getId()))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        EventDetailDto responseBody = jsonMapper.readValue(response.getContentAsString(), EventDetailDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertEquals(savedEvent.getId(), responseBody.getId()),
            () -> assertEquals("Summer Festival", responseBody.getTitle()),
            () -> assertEquals(List.of(savedPerformance.getId()), responseBody.getPerformanceIds())
        );
    }

    @Test
    public void findAllEventsReturnsBadRequestWhenStartPriceMinIsGreaterThanMax() throws Exception {
        MockHttpServletResponse response = perform(get(EVENT_BASE_URI)
            .param("startPriceMin", "50.00")
            .param("startPriceMax", "20.00"));

        assertBadRequest(response);
    }

    private MockHttpServletResponse postEvent(EventCreateDto createDto, MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart(EVENT_BASE_URI)
            .file(eventPart(createDto));
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        return perform(request.header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)));
    }

    private MockHttpServletResponse performWithUserAuth(MockHttpServletRequestBuilder request) throws Exception {
        return perform(request.header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(DEFAULT_USER, USER_ROLES)));
    }

    private MockHttpServletResponse perform(RequestBuilder request) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(request)
            .andDo(print())
            .andReturn();
        return mvcResult.getResponse();
    }

    private void assertBadRequest(MockHttpServletResponse response) {
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    private Hall persistHall(String hallName) {
        Venue venue = new Venue();
        venue.setName("Venue " + hallName);
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

    private EventCreateDto validEventCreateDto(Long performanceId) {
        EventCreateDto createDto = new EventCreateDto();
        createDto.setTitle("Summer Festival");
        createDto.setGenre("Festival");
        createDto.setDescription("Festival Description");
        createDto.setStartTime(LocalDateTime.of(2026, 8, 15, 18, 0));
        createDto.setEndTime(LocalDateTime.of(2026, 8, 15, 22, 0));
        if (performanceId != null) {
            createDto.setPerformanceIds(List.of(performanceId));
        }
        return createDto;
    }

    private MockMultipartFile eventPart(EventCreateDto createDto) throws Exception {
        return new MockMultipartFile(
            "event",
            "event",
            MediaType.APPLICATION_JSON_VALUE,
            jsonMapper.writeValueAsString(createDto).getBytes(StandardCharsets.UTF_8)
        );
    }

    private Event persistEvent(String title, String genre, LocalDateTime startTime) {
        Event event = new Event();
        event.setTitle(title);
        event.setGenre(genre);
        event.setStartTime(startTime);
        event.setEndTime(startTime.plusHours(5));
        return eventRepository.save(event);
    }

    private Event persistEventWithPerformance(String title, String genre, LocalDateTime startTime, String performanceName) {
        Event event = persistEvent(title, genre, startTime);
        persistPerformance(event, performanceName);
        return event;
    }

    private Performance persistPerformance(String hallName, String performanceName) {
        Performance performance = new Performance();
        performance.setHall(persistHall(hallName));
        performance.setPerformanceName(performanceName);
        performance.setStartTime(LocalDateTime.of(2026, 8, 15, 19, 0));
        performance.setEndTime(LocalDateTime.of(2026, 8, 15, 21, 0));
        performance.setStartPrice(new BigDecimal("49.90"));
        return performanceRepository.save(performance);
    }

    private Performance persistPerformance(Event event, String performanceName) {
        Performance performance = new Performance();
        performance.setHall(persistHall(performanceName + " Hall"));
        performance.setPerformanceName(performanceName);
        performance.setStartTime(event.getStartTime().plusHours(1));
        performance.setEndTime(event.getStartTime().plusHours(2));
        performance.setStartPrice(new BigDecimal("29.90"));
        performance.setEvent(event);
        return performanceRepository.save(performance);
    }
}
