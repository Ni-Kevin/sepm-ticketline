package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public class PerformanceRepositoryTest {

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void findAllByIdInWithRelationsReturnsSortedAndFetchedRelations() {
        Hall hall = persistHall("Main Hall");
        Artist batman = persistArtist("Bruce", "Wayne", "Batman Live");
        Artist superman = persistArtist("Clark", "Kent", "Superman Live");

        Performance laterPerformance = new Performance();
        laterPerformance.setPerformanceName("Later Performance");
        laterPerformance.setStartTime(LocalDateTime.of(2026, 12, 10, 21, 0));
        laterPerformance.setEndTime(LocalDateTime.of(2026, 12, 10, 23, 0));
        laterPerformance.setStartPrice(new BigDecimal("59.90"));
        laterPerformance.setHall(hall);
        laterPerformance.setArtists(new HashSet<>(List.of(superman)));
        laterPerformance = performanceRepository.saveAndFlush(laterPerformance);

        Performance earlierPerformance = new Performance();
        earlierPerformance.setPerformanceName("Earlier Performance");
        earlierPerformance.setStartTime(LocalDateTime.of(2026, 12, 10, 19, 0));
        earlierPerformance.setEndTime(LocalDateTime.of(2026, 12, 10, 21, 0));
        earlierPerformance.setStartPrice(new BigDecimal("39.90"));
        earlierPerformance.setHall(hall);
        earlierPerformance.setArtists(new HashSet<>(List.of(batman, superman)));

        PerformanceSectorPrice standingPrice = new PerformanceSectorPrice();
        standingPrice.setSectorId(77L);
        standingPrice.setSectorName("Standing A");
        standingPrice.setSectorType("STANDING");
        standingPrice.setPrice(new BigDecimal("39.90"));
        standingPrice.setPerformance(earlierPerformance);
        earlierPerformance.setSectorPrices(List.of(standingPrice));
        earlierPerformance = performanceRepository.saveAndFlush(earlierPerformance);
        Long earlierPerformanceId = earlierPerformance.getId();
        Long laterPerformanceId = laterPerformance.getId();

        entityManager.clear();

        List<Performance> result = performanceRepository.findAllByIdInWithRelations(List.of(laterPerformanceId, earlierPerformanceId));

        assertAll(
            () -> assertEquals(2, result.size()),
            () -> assertEquals(earlierPerformanceId, result.get(0).getId()),
            () -> assertEquals(laterPerformanceId, result.get(1).getId()),
            () -> assertEquals("Main Hall", result.get(0).getHall().getName()),
            () -> assertEquals(2, result.get(0).getArtists().size()),
            () -> assertFalse(result.get(0).getSectorPrices().isEmpty()),
            () -> assertTrue(result.get(0).getSectorPrices().stream().anyMatch(price -> "Standing A".equals(price.getSectorName())))
        );
    }

    @Test
    public void findAllWithRelationsOrderByStartTimeAscReturnsAllSortedPerformances() {
        Hall hall = persistHall("All Performances Hall");

        Performance laterPerformance = new Performance();
        laterPerformance.setPerformanceName("Later Performance");
        laterPerformance.setStartTime(LocalDateTime.of(2026, 12, 10, 21, 0));
        laterPerformance.setEndTime(LocalDateTime.of(2026, 12, 10, 23, 0));
        laterPerformance.setStartPrice(new BigDecimal("59.90"));
        laterPerformance.setHall(hall);
        laterPerformance = performanceRepository.saveAndFlush(laterPerformance);

        Performance earlierPerformance = new Performance();
        earlierPerformance.setPerformanceName("Earlier Performance");
        earlierPerformance.setStartTime(LocalDateTime.of(2026, 12, 10, 19, 0));
        earlierPerformance.setEndTime(LocalDateTime.of(2026, 12, 10, 21, 0));
        earlierPerformance.setStartPrice(new BigDecimal("39.90"));
        earlierPerformance.setHall(hall);
        earlierPerformance = performanceRepository.saveAndFlush(earlierPerformance);

        Long earlierPerformanceId = earlierPerformance.getId();
        Long laterPerformanceId = laterPerformance.getId();

        entityManager.clear();

        List<Performance> result = performanceRepository.findAllWithRelationsOrderByStartTimeAsc();

        assertAll(
            () -> assertEquals(2, result.size()),
            () -> assertEquals(earlierPerformanceId, result.get(0).getId()),
            () -> assertEquals(laterPerformanceId, result.get(1).getId()),
            () -> assertEquals("All Performances Hall", result.get(0).getHall().getName())
        );
    }

    @Test
    public void findAllByIdInWithRelationsReturnsOnlyExistingIds() {
        Hall hall = persistHall("Filtered Hall");

        Performance performance = new Performance();
        performance.setPerformanceName("Existing Performance");
        performance.setStartTime(LocalDateTime.of(2026, 12, 12, 20, 0));
        performance.setEndTime(LocalDateTime.of(2026, 12, 12, 22, 0));
        performance.setStartPrice(new BigDecimal("49.90"));
        performance.setHall(hall);
        performance = performanceRepository.saveAndFlush(performance);
        Long performanceId = performance.getId();

        entityManager.clear();

        List<Performance> result = performanceRepository.findAllByIdInWithRelations(List.of(performanceId, 999999L));

        assertAll(
            () -> assertEquals(1, result.size()),
            () -> assertEquals(performanceId, result.get(0).getId()),
            () -> assertEquals("Existing Performance", result.get(0).getPerformanceName())
        );
    }

    @Test
    public void findAllByIdInWithRelationsSortsByIdWhenStartTimeIsEqual() {
        Hall hall = persistHall("Tie Break Hall");

        LocalDateTime sameStartTime = LocalDateTime.of(2026, 12, 20, 19, 30);

        Performance first = new Performance();
        first.setPerformanceName("Tie A");
        first.setStartTime(sameStartTime);
        first.setEndTime(sameStartTime.plusHours(2));
        first.setStartPrice(new BigDecimal("19.90"));
        first.setHall(hall);
        first = performanceRepository.saveAndFlush(first);

        Performance second = new Performance();
        second.setPerformanceName("Tie B");
        second.setStartTime(sameStartTime);
        second.setEndTime(sameStartTime.plusHours(2));
        second.setStartPrice(new BigDecimal("29.90"));
        second.setHall(hall);
        second = performanceRepository.saveAndFlush(second);

        entityManager.clear();

        List<Performance> result = performanceRepository.findAllByIdInWithRelations(List.of(second.getId(), first.getId()));

        assertAll(
            () -> assertEquals(2, result.size()),
            () -> assertTrue(result.get(0).getId() < result.get(1).getId())
        );
    }

    @Test
    public void findAllByIdInWithRelationsReturnsEmptyWhenNoIdsExist() {
        Hall hall = persistHall("No Match Hall");

        Performance performance = new Performance();
        performance.setPerformanceName("Stored Performance");
        performance.setStartTime(LocalDateTime.of(2026, 12, 21, 18, 0));
        performance.setEndTime(LocalDateTime.of(2026, 12, 21, 20, 0));
        performance.setStartPrice(new BigDecimal("31.90"));
        performance.setHall(hall);
        performanceRepository.saveAndFlush(performance);

        entityManager.clear();

        List<Performance> result = performanceRepository.findAllByIdInWithRelations(List.of(999998L, 999999L));

        assertTrue(result.isEmpty());
    }

    @Test
    public void existsOverlappingPerformanceForHallReturnsTrueOnlyForSameHallAndOverlappingTime() {
        Hall occupiedHall = persistHall("Occupied Hall");
        Hall otherHall = persistHall("Other Hall");

        Performance performance = new Performance();
        performance.setPerformanceName("Stored Performance");
        performance.setStartTime(LocalDateTime.of(2026, 12, 21, 18, 0));
        performance.setEndTime(LocalDateTime.of(2026, 12, 21, 20, 0));
        performance.setStartPrice(new BigDecimal("31.90"));
        performance.setHall(occupiedHall);
        performanceRepository.saveAndFlush(performance);

        entityManager.clear();

        assertAll(
            () -> assertTrue(performanceRepository.existsOverlappingPerformanceForHall(
                occupiedHall.getId(),
                LocalDateTime.of(2026, 12, 21, 19, 0),
                LocalDateTime.of(2026, 12, 21, 21, 0)
            )),
            () -> assertFalse(performanceRepository.existsOverlappingPerformanceForHall(
                occupiedHall.getId(),
                LocalDateTime.of(2026, 12, 21, 20, 0),
                LocalDateTime.of(2026, 12, 21, 22, 0)
            )),
            () -> assertFalse(performanceRepository.existsOverlappingPerformanceForHall(
                otherHall.getId(),
                LocalDateTime.of(2026, 12, 21, 19, 0),
                LocalDateTime.of(2026, 12, 21, 21, 0)
            ))
        );
    }

    private Hall persistHall(String hallName) {
        Venue venue = new Venue();
        venue.setName("Venue " + hallName);
        venue.setStreet("Street 1");
        venue.setCity("Vienna");
        venue.setCountry("AT");
        venue.setZipCode("1010");
        Venue savedVenue = venueRepository.saveAndFlush(venue);

        Hall hall = new Hall();
        hall.setName(hallName);
        hall.setWidth(30);
        hall.setLength(20);
        hall.setVenue(savedVenue);
        return hallRepository.saveAndFlush(hall);
    }

    private Artist persistArtist(String firstName, String lastName, String artistName) {
        Artist artist = new Artist();
        artist.setFirstName(firstName);
        artist.setLastName(lastName);
        artist.setArtistName(artistName);
        return artistRepository.saveAndFlush(artist);
    }
}
