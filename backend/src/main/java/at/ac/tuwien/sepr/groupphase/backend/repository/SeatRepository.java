package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Find all seats belonging to a sector ordered by row and seat number ascending.
     *
     * @param sectorId the sector id
     * @return ordered list of seats
     */
    List<Seat> findBySectorIdOrderByRowNumberAscSeatNumberAsc(Long sectorId);

    /**
     * Find all seats belonging to a hall through the sector relation.
     *
     * @param hallId the hall id
     * @return list of seats in the hall
     */
    List<Seat> findBySectorHallId(Long hallId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findAndLockSeatById(@Param("id") Long id);
}
