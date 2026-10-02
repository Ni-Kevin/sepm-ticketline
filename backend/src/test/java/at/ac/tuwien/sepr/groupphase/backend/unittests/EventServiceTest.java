package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventPageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.EventMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.EventServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PerformanceRepository performanceRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event eventOne;
    private Event eventTwo;

    @BeforeEach
    public void beforeEach() {
        eventOne = event(1L, "Event One", LocalDateTime.of(2026, 5, 10, 18, 0));
        eventTwo = event(2L, "Event Two", LocalDateTime.of(2026, 5, 1, 18, 0));
    }

    @Test
    public void findAllReturnsPagedEvents() {

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(eventRepository.searchEventIds(
            eq(""),
            eq(""),
            isNull(),
            isNull(),
            eq(""),
            eq(""),
            eq(""),
            eq(""),
            isNull(),
            isNull(),
            any(LocalDateTime.class),
            eq(pageRequest)
        ))
            .thenReturn(new PageImpl<>(List.of(eventOne.getId(), eventTwo.getId()), pageRequest, 2));
        when(eventRepository.findEventsByIds(List.of(eventOne.getId(), eventTwo.getId())))
            .thenReturn(List.of(eventOne, eventTwo));

        when(eventMapper.eventToEventDetailDto(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            EventDetailDto dto = new EventDetailDto();
            dto.setId(event.getId());
            dto.setTitle(event.getTitle());
            dto.setStartTime(event.getStartTime());
            return dto;
        });

        EventPageDto result = eventService.findAll("", "", null, null, "", null, null, null, null, null, 0, 10);

        assertAll(
            () -> assertEquals(2, result.getContent().size()),
            () -> assertEquals(1L, result.getContent().get(0).getId()),
            () -> assertEquals(2L, result.getContent().get(1).getId()),
            () -> assertEquals(1, result.getTotalPages()),
            () -> assertEquals(2, result.getTotalElements())
        );
    }

    @Test
    public void findAllNormalizesAdvancedSearchTextFilters() {
        when(eventRepository.searchEventIds(
            eq("Summer Festival"),
            eq("Concert"),
            isNull(),
            isNull(),
            eq("Main Hall"),
            eq("Street 1"),
            eq("Vienna"),
            eq("1010"),
            isNull(),
            isNull(),
            any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        EventPageDto result = eventService.findAll(
            "  Summer   Festival  ",
            " Concert ",
            null,
            null,
            " Main Hall ",
            " Street 1 ",
            " Vienna ",
            " 1010 ",
            null,
            null,
            0,
            10
        );

        assertEquals(0, result.getContent().size());
        verify(eventRepository).searchEventIds(
            eq("Summer Festival"),
            eq("Concert"),
            isNull(),
            isNull(),
            eq("Main Hall"),
            eq("Street 1"),
            eq("Vienna"),
            eq("1010"),
            isNull(),
            isNull(),
            any(LocalDateTime.class),
            any(Pageable.class)
        );
    }

    @Test
    public void findByIdReturnsMappedEventWhenEventExists() {
        EventDetailDto mappedEvent = new EventDetailDto();
        mappedEvent.setId(eventOne.getId());
        mappedEvent.setTitle(eventOne.getTitle());

        when(eventRepository.findByIdWithPerformances(eventOne.getId())).thenReturn(Optional.of(eventOne));
        when(eventMapper.eventToEventDetailDto(eventOne)).thenReturn(mappedEvent);

        EventDetailDto result = eventService.findById(eventOne.getId());

        assertSame(mappedEvent, result);
    }

    @Test
    public void findByIdThrowsNotFoundWhenEventDoesNotExist() {
        when(eventRepository.findByIdWithPerformances(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> eventService.findById(999L)
        );

        assertAll(
            () -> assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode()),
            () -> assertEquals("Event not found", exception.getReason())
        );
    }

    @Test
    public void createEventLinksProvidedPerformancesToPersistedEvent() {
        Event eventToCreate = event("New Event", "Concert", LocalDateTime.of(2026, 5, 1, 19, 0), LocalDateTime.of(2026, 5, 1, 22, 0));
        Event persistedEvent = event(10L, "New Event", LocalDateTime.of(2026, 5, 1, 19, 0));
        Performance performanceOne = performance(21L, "39.99", LocalDateTime.of(2026, 5, 1, 19, 30), LocalDateTime.of(2026, 5, 1, 20, 30));
        Performance performanceTwo = performance(22L, "49.99", LocalDateTime.of(2026, 5, 1, 21, 0), LocalDateTime.of(2026, 5, 1, 21, 30));

        when(eventRepository.save(eventToCreate)).thenReturn(persistedEvent);
        when(performanceRepository.findAllById(List.of(21L, 22L))).thenReturn(List.of(performanceOne, performanceTwo));

        Event result = eventService.createEvent(eventToCreate, List.of(21L, 22L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Performance>> savedPerformancesCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(performanceRepository).saveAll(savedPerformancesCaptor.capture());

        assertAll(
            () -> assertSame(persistedEvent, result),
            () -> assertSame(persistedEvent, performanceOne.getEvent()),
            () -> assertSame(persistedEvent, performanceTwo.getEvent()),
            () -> assertEquals(2, savedPerformancesCaptor.getValue().size())
        );
    }

    @Test
    public void createEventThrowsWhenEventEndTimeIsBeforePerformanceEndTime() {
        Event eventToCreate = event("Too Short Event", "Concert", LocalDateTime.of(2026, 5, 1, 19, 0), LocalDateTime.of(2026, 5, 1, 20, 59));
        Performance performance = performance(21L, "39.99", LocalDateTime.of(2026, 5, 1, 19, 30), LocalDateTime.of(2026, 5, 1, 21, 0));

        when(performanceRepository.findAllById(List.of(21L))).thenReturn(List.of(performance));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> eventService.createEvent(eventToCreate, List.of(21L))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNothingPersisted();
    }

    @Test
    public void createEventChecksTitleBeforeStartTime() {
        Event eventToCreate = new Event();
        eventToCreate.setTitle(" ");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> eventService.createEvent(eventToCreate, null)
        );

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode()),
            () -> assertEquals("Title is required", exception.getReason())
        );
        assertNothingPersisted();
    }

    @Test
    public void createEventChecksGenreBeforeStartTime() {
        Event eventToCreate = new Event();
        eventToCreate.setTitle("New Event");
        eventToCreate.setGenre(" ");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> eventService.createEvent(eventToCreate, null)
        );

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode()),
            () -> assertEquals("Genre is required", exception.getReason())
        );
        assertNothingPersisted();
    }

    @Test
    public void createEventRejectsInvalidGenre() {
        Event eventToCreate = new Event();
        eventToCreate.setTitle("New Event");
        eventToCreate.setGenre("Rock");

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> eventService.createEvent(eventToCreate, null)
        );

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode()),
            () -> assertEquals("Invalid genre", exception.getReason())
        );
        assertNothingPersisted();
    }

    @Test
    public void createEventThrowsWhenPerformanceIdsAreMissing() {
        Event eventToCreate = event("Event Without Performance", "Concert", LocalDateTime.of(2026, 5, 1, 19, 0), LocalDateTime.of(2026, 5, 1, 22, 0));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> eventService.createEvent(eventToCreate, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertNothingPersisted();
    }

    private Event event(Long id, String title, LocalDateTime startTime) {
        Event event = event(title, null, startTime, startTime.plusHours(5));
        event.setId(id);
        return event;
    }

    private Event event(String title, String genre, LocalDateTime startTime, LocalDateTime endTime) {
        Event event = new Event();
        event.setTitle(title);
        event.setGenre(genre);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        return event;
    }

    private Performance performance(Long id, String startPrice, LocalDateTime startTime, LocalDateTime endTime) {
        Performance performance = new Performance();
        performance.setId(id);
        performance.setStartPrice(new BigDecimal(startPrice));
        performance.setStartTime(startTime);
        performance.setEndTime(endTime);
        return performance;
    }

    private void assertNothingPersisted() {
        verify(eventRepository, never()).save(any(Event.class));
        verify(performanceRepository, never()).saveAll(any());
    }
}
