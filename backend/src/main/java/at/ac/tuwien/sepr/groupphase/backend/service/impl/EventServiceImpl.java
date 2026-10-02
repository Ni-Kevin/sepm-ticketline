package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventPageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.EventMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.EventService;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementation for event service.
 */
@Service
public class EventServiceImpl implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Set<String> ALLOWED_GENRES = Set.of(
        "Cinema",
        "Comedy",
        "Concert",
        "Dance",
        "Exhibition",
        "Festival",
        "Musical",
        "Opera",
        "Sport",
        "Theater",
        "Other"
    );
    private final EventRepository eventRepository;
    private final PerformanceRepository performanceRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository, PerformanceRepository performanceRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.performanceRepository = performanceRepository;
        this.eventMapper = eventMapper;
    }

    /**
     * Load events matching the given optional search filters.
     *
     * @param query text query matched against event title
     * @param genre genre filter
     * @param dateFrom earliest event start time to include
     * @param dateTo latest event start time to include
     * @param hallName hall name filter
     * @param venueStreet venue street filter
     * @param venueCity venue city filter
     * @param venueZipCode venue ZIP code filter
     * @param startPriceMin minimum performance start price
     * @param startPriceMax maximum performance start price
     * @return matching event detail DTOs sorted by event start time
     */
    @Override
    @Transactional(readOnly = true)
    public EventPageDto findAll(
        String query,
        String genre,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        String hallName,
        String venueStreet,
        String venueCity,
        String venueZipCode,
        BigDecimal startPriceMin,
        BigDecimal startPriceMax,
        int page,
        int size
    ) {
        String normalizedQuery = query == null ? "" : query.trim().replaceAll("\\s+", " ");
        String normalizedGenre = genre == null ? "" : genre.trim();
        String normalizedHallName = hallName == null ? "" : hallName.trim();
        String normalizedVenueStreet = venueStreet == null ? "" : venueStreet.trim();
        String normalizedVenueCity = venueCity == null ? "" : venueCity.trim();
        String normalizedVenueZipCode = venueZipCode == null ? "" : venueZipCode.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        LocalDateTime activeAt = dateFrom == null && dateTo == null ? LocalDateTime.now() : null;

        Page<Long> eventIds = eventRepository.searchEventIds(
            normalizedQuery,
            normalizedGenre,
            dateFrom,
            dateTo,
            normalizedHallName,
            normalizedVenueStreet,
            normalizedVenueCity,
            normalizedVenueZipCode,
            startPriceMin,
            startPriceMax,
            activeAt,
            PageRequest.of(safePage, safeSize)
        );

        List<Long> ids = eventIds.getContent();
        if (ids.isEmpty()) {
            return new EventPageDto(List.of(), eventIds.getTotalPages(), eventIds.getTotalElements());
        }

        Map<Long, Event> eventsById = eventRepository.findEventsByIds(ids).stream()
            .collect(Collectors.toMap(Event::getId, Function.identity()));

        List<EventDetailDto> content = ids.stream()
            .map(eventsById::get)
            .filter(Objects::nonNull)
            .map(eventMapper::eventToEventDetailDto)
            .toList();

        return new EventPageDto(content, eventIds.getTotalPages(), eventIds.getTotalElements());
    }

    /**
     * Find one event by id with its performances loaded.
     *
     * @param id event id
     * @return mapped event detail DTO
     */
    @Override
    @Transactional(readOnly = true)
    public EventDetailDto findById(Long id) {
        return eventRepository.findByIdWithPerformances(id)
            .map(eventMapper::eventToEventDetailDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    /**
     * Create a new event and link performances to it.
     *
     * @param event event entity to persist
     * @param performanceIds ids of performances to link
     * @return persisted event entity
     */
    @Override
    public Event createEvent(Event event, List<Long> performanceIds) {
        LOGGER.debug("Create event {}", event);
        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (event.getGenre() == null || event.getGenre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Genre is required");
        }
        event.setGenre(event.getGenre().trim());
        if (!ALLOWED_GENRES.contains(event.getGenre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid genre");
        }
        if (event.getStartTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time is required");
        }
        if (event.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time is required");
        }
        if (event.getEndTime().isBefore(event.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must not be before start time");
        }
        if (performanceIds == null || performanceIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one performance is required");
        }

        List<Performance> performances = performanceRepository.findAllById(performanceIds);
        if (performances.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one performance is required");
        }

        LocalDateTime earliestStartTime = performances.stream()
            .map(Performance::getStartTime)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Performance start time is required"));
        LocalDateTime latestEndTime = performances.stream()
            .map(Performance::getEndTime)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Performance end time is required"));

        if (earliestStartTime.isBefore(event.getStartTime())
            || event.getEndTime().isBefore(latestEndTime)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Performance times must be within event start and end time"
            );
        }

        Event persistedEvent = eventRepository.save(event);
        for (Performance performance : performances) {
            performance.setEvent(persistedEvent);
            performance.setGenre(persistedEvent.getGenre());
        }
        performanceRepository.saveAll(performances);
        return persistedEvent;
    }
}
