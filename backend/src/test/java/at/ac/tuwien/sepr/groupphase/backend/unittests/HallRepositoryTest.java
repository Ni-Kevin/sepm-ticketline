package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public class HallRepositoryTest {

    @Autowired
    private HallRepository hallRepository;
    @Autowired
    private VenueRepository venueRepository;

    private Venue venue;

    @BeforeEach
    void setUp() {
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        venue = venueRepository.save(venue("Venue"));
    }

    @Test
    void givenHalls_whenFindAll_thenOrderedByName() {
        hallRepository.save(hall("B Hall", venue));
        hallRepository.save(hall("A Hall", venue));

        List<Hall> halls = hallRepository.findAllByOrderByNameAsc();

        assertEquals(List.of("A Hall", "B Hall"), halls.stream().map(Hall::getName).toList());
    }

    @Test
    void givenTwoVenues_whenFindByVenue_thenFilteredAndOrdered() {
        Venue other = venueRepository.save(venue("Other"));
        hallRepository.save(hall("B Hall", venue));
        hallRepository.save(hall("A Hall", venue));
        hallRepository.save(hall("C Hall", other));

        List<Hall> halls = hallRepository.findByVenueIdOrderByNameAsc(venue.getId());

        assertEquals(List.of("A Hall", "B Hall"), halls.stream().map(Hall::getName).toList());
    }

    @Test
    void givenHall_whenFindWithVenueById_thenVenueLoaded() {
        Hall saved = hallRepository.save(hall("Hall", venue));

        Hall found = hallRepository.findWithVenueById(saved.getId()).orElseThrow();

        assertEquals(venue.getName(), found.getVenue().getName());
    }

    private Hall hall(String name, Venue venue) {
        Hall hall = new Hall();
        hall.setName(name);
        hall.setWidth(10);
        hall.setLength(10);
        hall.setVenue(venue);
        return hall;
    }

    private Venue venue(String name) {
        Venue venue = new Venue();
        venue.setName(name);
        venue.setStreet("Street");
        venue.setCity("City");
        venue.setCountry("AT");
        venue.setZipCode("1000");
        return venue;
    }
}
