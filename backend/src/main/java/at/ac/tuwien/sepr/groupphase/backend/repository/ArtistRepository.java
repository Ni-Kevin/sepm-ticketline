package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for artist persistence operations.
 */
@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /**
     * Check whether an artist name already exists, case-insensitive.
     *
     * @param artistName stage name to check
     * @return true if a matching artist exists
     */
    boolean existsByArtistNameIgnoreCase(String artistName);

}
