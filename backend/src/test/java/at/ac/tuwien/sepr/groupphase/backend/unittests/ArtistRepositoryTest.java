package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public class ArtistRepositoryTest {

    @Autowired
    private ArtistRepository artistRepository;

    @Test
    public void existsByArtistNameIgnoreCaseReturnsTrueForCaseInsensitiveMatch() {
        Artist artist = new Artist();
        artist.setFirstName("John");
        artist.setLastName("Doe");
        artist.setArtistName("TheName");
        artistRepository.saveAndFlush(artist);

        boolean result = artistRepository.existsByArtistNameIgnoreCase("thename");

        assertTrue(result);
    }

    @Test
    public void saveDuplicateArtistNameThrowsDataIntegrityViolationException() {
        Artist first = new Artist();
        first.setFirstName("Max");
        first.setLastName("Power");
        first.setArtistName("UniqueStageName");
        artistRepository.saveAndFlush(first);

        Artist duplicate = new Artist();
        duplicate.setFirstName("Other");
        duplicate.setLastName("Person");
        duplicate.setArtistName("UniqueStageName");

        assertThrows(DataIntegrityViolationException.class, () -> artistRepository.saveAndFlush(duplicate));
    }
}
