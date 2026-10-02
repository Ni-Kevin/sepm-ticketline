package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {


    @Query("SELECT COUNT(t) FROM Ticket t "
        + "LEFT JOIN t.order o "
        + "LEFT JOIN t.reservation r "
        + "WHERE t.performance.id = :performanceId "
        + "AND t.status <> 'CANCELLED' "
        + "AND (o.user.id = :userId OR r.user.id = :userId)")
    int countTicketsForUserAndPerformance(@Param("userId") Long userId,
                                          @Param("performanceId") Long performanceId);

    @Query("SELECT COUNT(t) > 0 FROM Ticket t " + "WHERE t.performance.id = :performanceId " + "AND t.seat.id = :seatId " + "AND t.status <> 'CANCELLED'")
    boolean existsActiveTicketForSeat(@Param("performanceId") Long performanceId, @Param("seatId") Long seatId);


    @Query("SELECT COUNT(t) FROM Ticket t " + "WHERE t.performance.id = :performanceId " + "AND t.sector.id = :sectorId " + "AND t.status <> 'CANCELLED'")
    int countActiveTicketsForSector(@Param("performanceId") Long performanceId, @Param("sectorId") Long sectorId);

    @Query("SELECT t.seat.id FROM Ticket t "
        + "WHERE t.performance.id = :performanceId "
        + "AND t.status <> 'CANCELLED' ")
    Set<Long> findActiveTicketSeatIdsByPerformanceId(@Param("performanceId") Long performanceId);

    @Query("SELECT t FROM Ticket t "
        + "LEFT JOIN FETCH t.performance p "
        + "LEFT JOIN FETCH p.event "
        + "LEFT JOIN FETCH p.artists "
        + "LEFT JOIN FETCH t.sector s "
        + "LEFT JOIN FETCH s.hall h "
        + "LEFT JOIN FETCH h.venue "
        + "LEFT JOIN FETCH t.seat "
        + "WHERE t.id = :id")
    Optional<Ticket> findByIdWithAllRelations(@Param("id") Long id);

    @Query("SELECT t FROM Ticket t "
        + "LEFT JOIN FETCH t.performance p "
        + "LEFT JOIN FETCH p.event "
        + "LEFT JOIN FETCH p.artists "
        + "LEFT JOIN FETCH t.sector s "
        + "LEFT JOIN FETCH s.hall h "
        + "LEFT JOIN FETCH h.venue "
        + "LEFT JOIN FETCH t.seat "
        + "LEFT JOIN t.order o "
        + "LEFT JOIN t.reservation r "
        + "WHERE t.id IN :ticketIds "
        + "AND t.status = 'RESERVED'"
        + "AND (o.user.id = :userId OR r.user.id = :userId)")
    List<Ticket> findReservedTicketsByIdsAndUserId(@Param("ticketIds") List<Long> ticketIds, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t.id FROM Ticket t "
        + "LEFT JOIN t.order o "
        + "LEFT JOIN t.reservation r "
        + "WHERE t.id IN :ticketIds "
        + "AND t.status = 'RESERVED'"
        + "AND (o.user.id = :userId OR r.user.id = :userId)")
    List<Long> findReservedTicketIdsWithLock(@Param("ticketIds") List<Long> ticketIds, @Param("userId") Long userId);

    @Query("SELECT t FROM Ticket t "
        + "LEFT JOIN FETCH t.performance p "
        + "LEFT JOIN FETCH p.event "
        + "LEFT JOIN FETCH p.artists "
        + "LEFT JOIN FETCH t.sector s "
        + "LEFT JOIN FETCH s.hall h "
        + "LEFT JOIN FETCH h.venue "
        + "LEFT JOIN FETCH t.seat "
        + "LEFT JOIN t.order o "
        + "LEFT JOIN t.reservation r "
        + "WHERE t.id IN :ticketIds "
        + "AND t.status = 'RESERVED'"
        + "AND (o.user.id = :userId OR r.user.id = :userId)")
    List<Ticket> findReservedTicketsWithAllRelations(@Param("ticketIds") List<Long> ticketIds, @Param("userId") Long userId);

    @Query("SELECT t FROM Ticket t "
        + "WHERE t.id IN :ticketIds "
        + "AND t.status = 'PURCHASED' "
        + "AND t.order.user.id = :userId")
    List<Ticket> findPurchasedTicketsByIdsAndUserId(@Param("ticketIds") List<Long> ticketIds, @Param("userId") Long userId);

    @Query("""
        select e.id, e.title, e.genre, count(t.id)
        from Ticket t
        join t.performance p
        join p.event e
        join t.order o
        where t.status in ('PURCHASED', 'USED')
          and o.purchaseDate >= :fromInclusive
          and o.purchaseDate < :toExclusive
          and (:genre is null or lower(e.genre) = lower(:genre))
        group by e.id, e.title, e.genre
        order by count(t.id) desc, e.title asc
        """)
    List<Object[]> findTopEventSalesByMonth(@Param("fromInclusive") java.time.LocalDateTime fromInclusive,
                                            @Param("toExclusive") java.time.LocalDateTime toExclusive,
                                            @Param("genre") String genre,
                                            Pageable pageable);


    @Query("SELECT COUNT(t) FROM Ticket t "
        + "LEFT JOIN t.reservation r "
        + "WHERE t.performance.id = :performanceId "
        + "AND t.status = 'RESERVED' "
        + "AND r.user.id = :userId")
    int countReservedTicketsForUserAndPerformance(@Param("userId") Long userId,
                                                  @Param("performanceId") Long performanceId);

    @Query("SELECT t FROM Ticket t "
        + "LEFT JOIN FETCH t.performance p "
        + "LEFT JOIN FETCH p.hall h "
        + "LEFT JOIN FETCH h.venue v "
        + "LEFT JOIN FETCH t.sector s "
        + "LEFT JOIN FETCH t.seat st "
        + "LEFT JOIN t.order o "
        + "LEFT JOIN t.reservation r "
        + "WHERE (o.user.id = :userId) OR (r.user.id = :userId) "
        + "ORDER BY p.startTime DESC")
    List<Ticket> findAllByUserIdWithRelations(@Param("userId") Long userId);


    boolean existsByReservationId(Long reservationId);
}
