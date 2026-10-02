package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {

    /**
     * Find all sectors belonging to a hall ordered by name ascending.
     *
     * @param hallId the hall id
     * @return ordered list of sectors
     */
    List<Sector> findByHallIdOrderByNameAsc(Long hallId);

    /**
     * Check whether a sector name already exists within a hall.
     *
     * @param hallId the hall id
     * @param name the sector name
     * @return true if a sector with that name exists in the hall
     */
    boolean existsByHallIdAndName(Long hallId, String name);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sector s WHERE s.id = :id")
    Optional<Sector> findAndLockSectorById(@Param("id") Long id);

    @Query("SELECT s FROM Sector s "
        + "LEFT JOIN FETCH s.hall h "
        + "LEFT JOIN FETCH h.venue "
        + "WHERE s.id = :id")
    Optional<Sector> findByIdWithVenue(@Param("id") Long id);
}
