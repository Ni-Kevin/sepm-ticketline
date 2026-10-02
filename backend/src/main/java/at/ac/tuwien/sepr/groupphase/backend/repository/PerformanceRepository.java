package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for performance persistence operations.
 */
@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    boolean existsByHallId(Long hallId);

    @Query("""
        select distinct p from Performance p
        left join fetch p.artists
        left join fetch p.hall
        left join fetch p.sectorPrices
        where p.id in :ids
        order by p.startTime asc, p.id asc
        """)
    List<Performance> findAllByIdInWithRelations(@Param("ids") List<Long> ids);

    @Query("""
        select distinct p from Performance p
        left join fetch p.artists
        left join fetch p.hall
        left join fetch p.sectorPrices
        order by p.startTime asc, p.id asc
        """)
    List<Performance> findAllWithRelationsOrderByStartTimeAsc();

    @Query("""
        select (count(p) > 0) from Performance p
        join p.artists a
        where a.id in :artistIds
          and p.startTime < :newEndTime
          and p.endTime > :newStartTime
        """)
    boolean existsOverlappingPerformanceForAnyArtist(@Param("artistIds") List<Long> artistIds,
                                                      @Param("newStartTime") LocalDateTime newStartTime,
                                                      @Param("newEndTime") LocalDateTime newEndTime);

    @Query("""
        select (count(p) > 0) from Performance p
        where p.hall.id = :hallId
          and p.startTime < :newEndTime
          and p.endTime > :newStartTime
        """)
    boolean existsOverlappingPerformanceForHall(@Param("hallId") Long hallId,
                                                @Param("newStartTime") LocalDateTime newStartTime,
                                                @Param("newEndTime") LocalDateTime newEndTime);

    @Query("""
       select distinct p from Performance p
       left join fetch p.artists
       left join fetch p.hall
       left join fetch p.sectorPrices
       join p.artists a
       where a.id = :artistId
       order by p.startTime asc, p.id asc
        """)
    List<Performance> findAllByArtistIdWithRelations(@Param("artistId") Long artistId);
}
