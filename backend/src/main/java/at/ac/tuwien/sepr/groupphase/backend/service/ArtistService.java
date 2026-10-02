package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import java.util.List;

public interface ArtistService {

    /**
     * Create a new artist.
     *
     * @param artist artist data
     * @return persisted artist
     */
    Artist createArtist(Artist artist);

    /**
     * Search artists by stage name, first name, or last name.
     *
     * @param query search term
     * @return matching artists
     */
    List<Artist> searchArtists(String query);

    /**
     * Get by artist id.
     *
     * @param id id of artist
     * @return artist entity
     */
    Artist getArtistById(Long id);
}
