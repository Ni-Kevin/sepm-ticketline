package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.ArtistService;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Implementation for artist service.
 */
@Service
public class ArtistServiceImpl implements ArtistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ArtistRepository artistRepository;

    public ArtistServiceImpl(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    /**
     * Create a new artist if the artist name is unique.
     *
     * @param artist artist entity to persist
     * @return persisted artist entity
     */
    @Override
    public Artist createArtist(Artist artist) {
        LOGGER.debug("Create artist {}", artist);
        if (artistRepository.existsByArtistNameIgnoreCase(artist.getArtistName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Artist name already exists");
        }
        return artistRepository.save(artist);
    }

    /**
     * Search artists by query term across stage name, first name, and last name.
     *
     * @param query query term
     * @return matching artists sorted by stage name
     */
    @Override
    public List<Artist> searchArtists(String query) {
        LOGGER.debug("Search artists with query {}", query);
        List<Artist> sortedArtists = artistRepository.findAll().stream()
            .sorted(Comparator.comparing(Artist::getArtistName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        if (query == null || query.isBlank()) {
            return sortedArtists;
        }

        List<String> terms = List.of(query.trim().toLowerCase(Locale.ROOT).split("\\s+"));
        return sortedArtists.stream()
            .filter(artist -> matchesAllTerms(artist, terms))
            .limit(10)
            .toList();
    }

    private boolean matchesAllTerms(Artist artist, List<String> terms) {
        String searchableText = String.join(" ",
            safeLower(artist.getArtistName()),
            safeLower(artist.getFirstName()),
            safeLower(artist.getLastName()),
            safeLower(artist.getFirstName()) + " " + safeLower(artist.getLastName()),
            safeLower(artist.getLastName()) + " " + safeLower(artist.getFirstName())
        );

        return terms.stream().allMatch(searchableText::contains);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Get by artist id.
     *
     * @param id id of artist
     * @return artist entity
     */
    @Override
    public Artist getArtistById(Long id) {
        return artistRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(String.format("Could not find artist with id %s", id)));
    }
}
