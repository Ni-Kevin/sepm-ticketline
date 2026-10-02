package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.VenueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    private VenueServiceImpl venueService;

    @BeforeEach
    void setUp() {
        venueService = new VenueServiceImpl(venueRepository);
    }

    @Test
    void givenVenue_whenCreate_thenSavesVenue() {
        Venue venue = venue("Venue");
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

        Venue created = venueService.create(venue);

        assertEquals("Venue", created.getName());
        assertEquals("Austria", created.getCountry());
        verify(venueRepository).save(venue);
    }

    @Test
    void givenVenueWithDifferentCountry_whenUpdate_thenCountryIsForcedToAustria() {
        Venue existing = venue("Existing Venue");
        existing.setId(1L);
        Venue update = venue("Updated Venue");
        update.setCountry("Germany");

        when(venueRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

        Venue result = venueService.update(1L, update);

        assertEquals("Updated Venue", result.getName());
        assertEquals("Austria", result.getCountry());
    }

    @Test
    void givenMissingVenue_whenFindOne_thenNotFound() {
        when(venueRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> venueService.findOne(1L));
    }

    @Test
    void givenMissingVenue_whenUpdate_thenNotFound() {
        when(venueRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> venueService.update(1L, venue("Venue")));
    }

    private Venue venue(String name) {
        Venue venue = new Venue();
        venue.setName(name);
        venue.setStreet("Street");
        venue.setCity("City");
        venue.setCountry("Germany");
        venue.setZipCode("1000");
        return venue;
    }
}
