package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HallRepository extends JpaRepository<Hall, Long> {

    @EntityGraph(attributePaths = "venue")
    List<Hall> findAllByOrderByNameAsc();

    /**
     * Find all halls belonging to a venue ordered by name ascending.
     *
     * @param venueId the venue id
     * @return ordered list of halls
     */
    @EntityGraph(attributePaths = "venue")
    List<Hall> findByVenueIdOrderByNameAsc(Long venueId);

    @EntityGraph(attributePaths = "venue")
    Optional<Hall> findWithVenueById(Long id);

    /**
     * Check whether a hall name already exists within a venue.
     *
     * @param venueId the venue id
     * @param name the hall name
     * @return true if a hall with that name exists in the venue
     */
    boolean existsByVenueIdAndName(Long venueId, String name);
}
