package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventPageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.EventMapper;
import at.ac.tuwien.sepr.groupphase.backend.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST endpoint for event operations.
 */
@RestController
@RequestMapping(value = "/api/v1/events")
public class EventEndpoint {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_PNG_VALUE,
        "image/webp"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final EventService eventService;
    private final EventMapper eventMapper;

    public EventEndpoint(EventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    /**
     * Load events matching the given optional search filters.
     *
     * @param query text query matched against event title
     * @param genre genre filter
     * @param dateFrom earliest event start date to include
     * @param dateTo latest event start date to include
     * @param hallName hall name filter
     * @param venueStreet venue street filter
     * @param venueCity venue city filter
     * @param venueZipCode venue ZIP code filter
     * @param startPriceMin minimum performance start price
     * @param startPriceMax maximum performance start price
     * @return matching events with detail information
     */
    @PermitAll
    @GetMapping
    @Operation(summary = "Get all events")
    public EventPageDto findAll(
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "genre", required = false) String genre,
        @RequestParam(name = "dateFrom", required = false) LocalDate dateFrom,
        @RequestParam(name = "dateTo", required = false) LocalDate dateTo,
        @RequestParam(name = "hallName", required = false) String hallName,
        @RequestParam(name = "venueStreet", required = false) String venueStreet,
        @RequestParam(name = "venueCity", required = false) String venueCity,
        @RequestParam(name = "venueZipCode", required = false) String venueZipCode,
        @RequestParam(name = "startPriceMin", required = false) BigDecimal startPriceMin,
        @RequestParam(name = "startPriceMax", required = false) BigDecimal startPriceMax,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        LOGGER.info("GET /api/v1/events q={} genre={} dateFrom={} dateTo={} hallName={} venueStreet={} venueCity={} venueZipCode={} startPriceMin={} startPriceMax={}",
            query, genre, dateFrom, dateTo, hallName, venueStreet, venueCity, venueZipCode, startPriceMin, startPriceMax);
        if (startPriceMin != null && startPriceMax != null && startPriceMin.compareTo(startPriceMax) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startPriceMin must be less than or equal to startPriceMax");
        }
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(23, 59, 59) : null;
        return eventService.findAll(query, genre, from, to, hallName, venueStreet, venueCity, venueZipCode, startPriceMin, startPriceMax, page, size);
    }

    /**
     * Load one event by id.
     *
     * @param id event id
     * @return event detail information
     */
    @PermitAll
    @GetMapping("/{id}")
    @Operation(summary = "Get event by id")
    public EventDetailDto findById(@PathVariable("id") Long id) {
        LOGGER.info("GET /api/v1/events/{}", id);
        return eventService.findById(id);
    }

    /**
     * Create a new event from multipart payload.
     *
     * @param eventCreateDto event payload
     * @param image optional event image
     * @return persisted event
     */
    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new event", security = @SecurityRequirement(name = "apiKey"))
    public EventDetailDto create(
        @Valid @RequestPart("event") EventCreateDto eventCreateDto,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        LOGGER.info("POST /api/v1/events body: {}", eventCreateDto);
        if (image != null && !image.isEmpty()) {
            try {
                if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PNG, JPEG and WebP images are allowed");
                }
                eventCreateDto.setImage(image.getBytes());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image file");
            }
        }
        return eventMapper.eventToEventDetailDto(
            eventService.createEvent(eventMapper.eventCreateDtoToEvent(eventCreateDto), eventCreateDto.getPerformanceIds()));
    }
}
