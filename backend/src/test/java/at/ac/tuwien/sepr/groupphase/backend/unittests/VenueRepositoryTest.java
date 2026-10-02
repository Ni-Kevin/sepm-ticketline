package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public class VenueRepositoryTest {

    @Autowired
    private VenueRepository venueRepository;

    @BeforeEach
    void setUp() {
        venueRepository.deleteAll();
    }

    @Test
    void givenVenues_whenFindAll_thenOrderedByName() {
        venueRepository.save(venue("B Venue"));
        venueRepository.save(venue("A Venue"));

        List<Venue> venues = venueRepository.findAllByOrderByNameAsc();

        assertEquals(List.of("A Venue", "B Venue"), venues.stream().map(Venue::getName).toList());
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
