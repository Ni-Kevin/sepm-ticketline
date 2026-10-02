package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    /**
     * Find all venues ordered by name ascending.
     *
     * @return ordered list of venues
     */
    List<Venue> findAllByOrderByNameAsc();
}
