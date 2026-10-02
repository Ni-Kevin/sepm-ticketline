package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HallAreaRepository extends JpaRepository<HallArea, Long> {

    /**
     * Find all hall areas for a given hall ordered by id ascending.
     *
     * @param hallId the hall id
     * @return list of hall areas
     */
    List<HallArea> findByHallIdOrderByIdAsc(Long hallId);
}
