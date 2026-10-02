package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventPageDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

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
     * @return matching events
     */
    EventPageDto findAll(
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
    );

    /**
     * Find one event by id.
     *
     * @param id event id
     * @return event detail
     */
    EventDetailDto findById(Long id);

    /**
     * Create a new event and assign existing performances.
     *
     * @param event event data
     * @param performanceIds ids of performances that must be linked to the event
     * @return persisted event
     */
    Event createEvent(Event event, List<Long> performanceIds);
}
