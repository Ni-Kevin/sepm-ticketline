package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.ArtistServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private ArtistServiceImpl artistService;

    @Test
    public void createArtistThrowsConflictWhenArtistNameAlreadyExists() {
        Artist artist = new Artist();
        artist.setArtistName("already-used");

        when(artistRepository.existsByArtistNameIgnoreCase("already-used")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> artistService.createArtist(artist));

        assertAll(
            () -> assertEquals(HttpStatus.CONFLICT, exception.getStatusCode()),
            () -> assertEquals("Artist name already exists", exception.getReason())
        );
        verify(artistRepository, never()).save(artist);
    }

    @Test
    public void createArtistPersistsArtistWhenArtistNameDoesNotExist() {
        Artist artist = new Artist();
        artist.setArtistName("new-name");

        Artist savedArtist = new Artist();
        savedArtist.setId(12L);
        savedArtist.setArtistName("new-name");

        when(artistRepository.existsByArtistNameIgnoreCase("new-name")).thenReturn(false);
        when(artistRepository.save(artist)).thenReturn(savedArtist);

        Artist result = artistService.createArtist(artist);

        assertSame(savedArtist, result);
        verify(artistRepository).save(artist);
    }

    @Test
    public void searchArtistsReturnsEmptyListWhenQueryIsBlank() {
        Artist artist = new Artist();
        artist.setArtistName("Alpha");
        when(artistRepository.findAll()).thenReturn(List.of(artist));

        List<Artist> result = artistService.searchArtists("   ");

        assertEquals(1, result.size());
    }

    @Test
    public void searchArtistsSortsResultsByArtistNameCaseInsensitive() {
        Artist zulu = new Artist();
        zulu.setArtistName("Zulu");
        zulu.setFirstName("Zed");
        zulu.setLastName("Person");

        Artist alpha = new Artist();
        alpha.setArtistName("alpha");
        alpha.setFirstName("Alice");
        alpha.setLastName("Artist");

        when(artistRepository.findAll()).thenReturn(List.of(zulu, alpha));

        List<Artist> result = artistService.searchArtists("l");

        assertAll(
            () -> assertEquals(2, result.size()),
            () -> assertEquals("alpha", result.get(0).getArtistName()),
            () -> assertEquals("Zulu", result.get(1).getArtistName())
        );
    }

    @Test
    public void searchArtistsRequiresAllTermsToMatchAcrossFields() {
        Artist match = new Artist();
        match.setArtistName("StageOne");
        match.setFirstName("Till");
        match.setLastName("Lindemann");

        Artist partial = new Artist();
        partial.setArtistName("StageTwo");
        partial.setFirstName("Till");
        partial.setLastName("Smith");

        when(artistRepository.findAll()).thenReturn(List.of(match, partial));

        List<Artist> result = artistService.searchArtists("Till Lindemann");

        assertEquals(1, result.size());
        assertEquals("StageOne", result.get(0).getArtistName());
    }
}
