package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for event persistence operations.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("select distinct e.genre from Event e where trim(e.genre) <> '' order by e.genre asc")
    List<String> findGenres();

    /**
     * Find one event with its performances loaded.
     *
     * @param id event id
     * @return matching event if present
     */
    @Query("""
        select distinct e
        from Event e
        left join fetch e.performances
        where e.id = :id
        """)
    Optional<Event> findByIdWithPerformances(@Param("id") Long id);

    /**
     * Search events using optional filters and return a page of matching event ids.
     * Empty string filters are ignored, and null date or price filters are ignored.
     *
     * @param query text query matched against event title
     * @param genre genre filter
     * @param dateFrom earliest event start time to include
     * @param dateTo latest event start time to include
     * @param hallName hall name filter
     * @param venueStreet venue street filter
     * @param venueCity venue city filter
     * @param venueZipCode venue ZIP code filter
     * @param startPriceMin minimum performance start price
     * @param startPriceMax maximum performance start price
     * @param activeAt only include events that have not ended at this time; ignored when null
     * @param pageable page request
     * @return matching event ids sorted by start time and id
     */
    @Query(value = """
        select e.id
        from Event e
        where (:query = '' or lower(e.title) like lower(concat('%', :query, '%')))
          and (:genre = '' or lower(e.genre) like lower(concat('%', :genre, '%')))
          and (:dateFrom is null or e.startTime >= :dateFrom)
          and (:dateTo is null or e.startTime <= :dateTo)
          and (:activeAt is null or e.endTime >= :activeAt)
          and exists (
              select 1
              from Performance p
              join p.hall h
              join h.venue v
              where p.event = e
                and (:hallName = '' or lower(h.name) like lower(concat('%', :hallName, '%')))
                and (:venueStreet = '' or lower(v.street) like lower(concat('%', :venueStreet, '%')))
                and (:venueCity = '' or lower(v.city) like lower(concat('%', :venueCity, '%')))
                and (:venueZipCode = '' or lower(v.zipCode) like lower(concat('%', :venueZipCode, '%')))
                and (:startPriceMin is null or p.startPrice >= :startPriceMin)
                and (:startPriceMax is null or p.startPrice <= :startPriceMax)
          )
        order by e.startTime asc, e.id asc
        """,
        countQuery = """
        select count(e.id)
        from Event e
        where (:query = '' or lower(e.title) like lower(concat('%', :query, '%')))
          and (:genre = '' or lower(e.genre) like lower(concat('%', :genre, '%')))
          and (:dateFrom is null or e.startTime >= :dateFrom)
          and (:dateTo is null or e.startTime <= :dateTo)
          and (:activeAt is null or e.endTime >= :activeAt)
          and exists (
              select 1
              from Performance p
              join p.hall h
              join h.venue v
              where p.event = e
                and (:hallName = '' or lower(h.name) like lower(concat('%', :hallName, '%')))
                and (:venueStreet = '' or lower(v.street) like lower(concat('%', :venueStreet, '%')))
                and (:venueCity = '' or lower(v.city) like lower(concat('%', :venueCity, '%')))
                and (:venueZipCode = '' or lower(v.zipCode) like lower(concat('%', :venueZipCode, '%')))
                and (:startPriceMin is null or p.startPrice >= :startPriceMin)
                and (:startPriceMax is null or p.startPrice <= :startPriceMax)
          )
        """)
    Page<Long> searchEventIds(
        @Param("query") String query,
        @Param("genre") String genre,
        @Param("dateFrom") LocalDateTime dateFrom,
        @Param("dateTo") LocalDateTime dateTo,
        @Param("hallName") String hallName,
        @Param("venueStreet") String venueStreet,
        @Param("venueCity") String venueCity,
        @Param("venueZipCode") String venueZipCode,
        @Param("startPriceMin") BigDecimal startPriceMin,
        @Param("startPriceMax") BigDecimal startPriceMax,
        @Param("activeAt") LocalDateTime activeAt,
        Pageable pageable
    );

    /**
     * Load events for a previously paged list of ids.
     *
     * @param ids event ids to load
     * @return matching events
     */
    @Query("""
        select e
        from Event e
        where e.id in :ids
        """)
    List<Event> findEventsByIds(@Param("ids") List<Long> ids);
}
