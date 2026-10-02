package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private HallRepository hallRepository;

    @Test
    public void searchEventsReturnsEmptyListWhenNoEventMatchesFilters() {
        Event event = new Event();
        event.setTitle("Summer Event");
        event.setGenre("Festival");
        event.setStartTime(LocalDateTime.of(2026, 7, 10, 18, 0));
        event.setEndTime(LocalDateTime.of(2026, 7, 10, 23, 0));
        Event savedEvent = eventRepository.saveAndFlush(event);

        Performance performance = new Performance();
        Hall hall = persistHall("Hall Search");
        performance.setPerformanceName("Opening Act");
        performance.setStartTime(LocalDateTime.of(2026, 7, 10, 19, 0));
        performance.setEndTime(LocalDateTime.of(2026, 7, 10, 21, 0));
        performance.setStartPrice(new BigDecimal("29.90"));
        performance.setHall(hall);
        performance.setEvent(savedEvent);
        performanceRepository.saveAndFlush(performance);

        entityManager.clear();

        Page<Long> result = eventRepository.searchEventIds(
            "no-match-query",
            "",
            LocalDateTime.of(2026, 8, 1, 0, 0),
            LocalDateTime.of(2026, 8, 31, 23, 59),
            "NoSuchLocation",
            "",
            "",
            "",
            new BigDecimal("999.99"),
            null,
            null,
            PageRequest.of(0, 10)
        );

        assertEquals(0, result.getContent().size());
    }

    @Test
    public void searchEventsFiltersByVenueCityAndHallName() {
        Event viennaEvent = persistEvent("Vienna Event", "Concert", LocalDateTime.of(2026, 7, 10, 18, 0));
        Event grazEvent = persistEvent("Graz Event", "Concert", LocalDateTime.of(2026, 7, 11, 18, 0));

        persistPerformance(viennaEvent, persistHall("Main Hall", "Street 1", "Vienna", "AT", "1010"), "Vienna Performance", "29.90");
        persistPerformance(grazEvent, persistHall("Second Hall", "Street 2", "Graz", "AT", "8010"), "Graz Performance", "39.90");

        entityManager.clear();

        Page<Long> result = eventRepository.searchEventIds("", "", null, null, "Main", "", "Vienna", "", null, null, null, PageRequest.of(0, 10));

        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals(viennaEvent.getId(), result.getContent().getFirst())
        );
    }

    @Test
    public void searchEventsFiltersByStartPriceRange() {
        Event cheapEvent = persistEvent("Cheap Event", "Concert", LocalDateTime.of(2026, 7, 10, 18, 0));
        Event matchingEvent = persistEvent("Matching Event", "Concert", LocalDateTime.of(2026, 7, 11, 18, 0));
        Event expensiveEvent = persistEvent("Expensive Event", "Concert", LocalDateTime.of(2026, 7, 12, 18, 0));

        persistPerformance(cheapEvent, persistHall("Cheap Hall"), "Cheap Performance", "19.90");
        persistPerformance(matchingEvent, persistHall("Matching Hall"), "Matching Performance", "35.00");
        persistPerformance(expensiveEvent, persistHall("Expensive Hall"), "Expensive Performance", "69.90");

        entityManager.clear();

        Page<Long> result = eventRepository.searchEventIds(
            "",
            "",
            null,
            null,
            "",
            "",
            "",
            "",
            new BigDecimal("30.00"),
            new BigDecimal("40.00"),
            null,
            PageRequest.of(0, 10)
        );

        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals(matchingEvent.getId(), result.getContent().getFirst())
        );
    }

    @Test
    public void searchEventsFiltersByEventTitleAndGenre() {
        Event matchingEvent = persistEvent("Summer Festival", "Concert", LocalDateTime.of(2026, 7, 10, 18, 0));
        Event wrongGenreEvent = persistEvent("Summer Festival Late", "Theater", LocalDateTime.of(2026, 7, 11, 18, 0));
        Event wrongTitleEvent = persistEvent("Winter Festival", "Concert", LocalDateTime.of(2026, 7, 12, 18, 0));

        persistPerformance(matchingEvent, persistHall("Matching Hall"), "Matching Performance", "29.90");
        persistPerformance(wrongGenreEvent, persistHall("Wrong Genre Hall"), "Wrong Genre Performance", "29.90");
        persistPerformance(wrongTitleEvent, persistHall("Wrong Title Hall"), "Wrong Title Performance", "29.90");

        entityManager.clear();

        Page<Long> result = eventRepository.searchEventIds("Summer", "Concert", null, null, "", "", "", "", null, null, null, PageRequest.of(0, 10));

        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals(matchingEvent.getId(), result.getContent().getFirst())
        );
    }

    @Test
    public void searchEventsDoesNotMatchPerformanceNameWithQuery() {
        Event event = persistEvent("Event Title", "Concert", LocalDateTime.of(2026, 7, 10, 18, 0));
        persistPerformance(event, persistHall("Main Hall"), "Unique Performance", "29.90");

        entityManager.clear();

        Page<Long> result = eventRepository.searchEventIds("Unique Performance", "", null, null, "", "", "", "", null, null, null, PageRequest.of(0, 10));

        assertEquals(0, result.getContent().size());
    }

    @Test
    public void searchEventsFiltersEndedEventsWhenActiveAtIsGiven() {
        LocalDateTime activeAt = LocalDateTime.of(2026, 7, 10, 12, 0);
        Event endedEvent = persistEvent("Ended Event", "Concert", activeAt.minusDays(2));
        Event activeEvent = persistEvent("Active Event", "Concert", activeAt.plusDays(1));

        persistPerformance(endedEvent, persistHall("Ended Hall"), "Ended Performance", "29.90");
        persistPerformance(activeEvent, persistHall("Active Hall"), "Active Performance", "29.90");

        entityManager.clear();

        Page<Long> result = eventRepository.searchEventIds("", "", null, null, "", "", "", "", null, null, activeAt, PageRequest.of(0, 10));

        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals(activeEvent.getId(), result.getContent().getFirst())
        );
    }

    private Event persistEvent(String title, String genre, LocalDateTime startTime) {
        Event event = new Event();
        event.setTitle(title);
        event.setGenre(genre);
        event.setStartTime(startTime);
        event.setEndTime(startTime.plusHours(3));
        return eventRepository.saveAndFlush(event);
    }

    private Performance persistPerformance(Event event, Hall hall, String performanceName, String startPrice) {
        Performance performance = new Performance();
        performance.setPerformanceName(performanceName);
        performance.setStartTime(event.getStartTime().plusHours(1));
        performance.setEndTime(event.getStartTime().plusHours(2));
        performance.setStartPrice(new BigDecimal(startPrice));
        performance.setHall(hall);
        performance.setEvent(event);
        return performanceRepository.saveAndFlush(performance);
    }

    private Hall persistHall(String hallName) {
        return persistHall(hallName, "Street 1", "Vienna", "AT", "1010");
    }

    private Hall persistHall(String hallName, String street, String city, String country, String zipCode) {
        Venue venue = new Venue();
        venue.setName("Venue " + hallName);
        venue.setStreet(street);
        venue.setCity(city);
        venue.setCountry(country);
        venue.setZipCode(zipCode);
        Venue savedVenue = venueRepository.saveAndFlush(venue);

        Hall hall = new Hall();
        hall.setName(hallName);
        hall.setWidth(20);
        hall.setLength(20);
        hall.setVenue(savedVenue);
        return hallRepository.saveAndFlush(hall);
    }
}
