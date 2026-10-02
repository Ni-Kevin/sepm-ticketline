package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeatHold;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for seat hold persistence operations.
 */
@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    @Query("SELECT s FROM SeatHold s WHERE s.seat.id = :seatId "
        + "AND s.performance.id = :performanceId AND s.expiresAt > :now AND s.active = true")
    Optional<SeatHold> findActiveHoldBySeat(
        @Param("seatId") Long seatId,
        @Param("performanceId") Long performanceId,
        @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM SeatHold s WHERE s.sector.id = :sectorId "
        + "AND s.performance.id = :performanceId AND s.expiresAt > :now AND s.active = true")
    int countActiveHoldsForSector(
        @Param("sectorId") Long sectorId,
        @Param("performanceId") Long performanceId,
        @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SeatHold s WHERE s.performance.id = :performanceId "
        + "AND s.user.id = :userId  AND s.active = true")
    void deleteActiveHoldsByPerformanceAndUser(
        @Param("performanceId") Long performanceId,
        @Param("userId") Long userId,
        @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SeatHold s WHERE s.expiresAt <= :now")
    void deleteExpiredHolds(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM SeatHold s WHERE s.user.id = :userId "
        + "AND s.performance.id = :performanceId "
        + "AND s.sector.id = :sectorId AND s.expiresAt > :now AND s.active = true")
    List<SeatHold> findActiveHoldsByUserSectorAndPerformance(
        @Param("userId") Long userId,
        @Param("sectorId") Long sectorId,
        @Param("performanceId") Long performanceId,
        @Param("now") LocalDateTime now);

    @Query("SELECT s FROM SeatHold s WHERE s.user.id = :userId "
        + "AND s.performance.id = :performanceId "
        + "AND s.expiresAt > :now AND s.active = true")
    List<SeatHold> findActiveHoldsByUserAndPerformance(
        @Param("userId") Long userId,
        @Param("performanceId") Long performanceId,
        @Param("now") LocalDateTime now);

    @Query("SELECT s.seat.id FROM SeatHold s "
        + "WHERE s.performance.id = :performanceId "
        + "AND s.expiresAt > :now AND s.active = true "
        + "AND s.user.id <> :userId")
    Set<Long> findActiveHoldSeatIdsByPerformanceId(
        @Param("performanceId") Long performanceId,
        @Param("now") LocalDateTime now,
        @Param("userId") Long userId);


    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM SeatHold s "
        + "WHERE s.user.id = :userId "
        + "AND s.performance.id = :performanceId "
        + "AND s.expiresAt > :now AND s.active = true")
    int countActiveHoldsQuantityForUserAndPerformance(
        @Param("userId") Long userId,
        @Param("performanceId") Long performanceId,
        @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatHold s WHERE s.id = :id")
    Optional<SeatHold> findAndLockSeatById(@Param("id") Long id);
}
