package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceHallLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatStatusDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import java.util.List;

public interface PerformanceService {

    /**
     * Create a performance and link it to existing artists.
     *
     * @param performance performance data
     * @param artistIds ids of artists that perform in this performance
     * @param sectorPrices sector prices for seating and standing sectors
     * @return persisted performance
     */
    Performance createPerformance(Performance performance, List<Long> artistIds, List<PerformanceSectorPrice> sectorPrices,
                                  Integer durationHours, Integer durationMinutes);

    /**
     * Load performances by ids.
     *
     * @param ids performance ids
     * @return performances including hall and artists
     */
    List<Performance> findByIds(List<Long> ids);

    /**
     * Load all performances ordered by start time.
     *
     * @return performances including hall and artists
     */
    List<Performance> findAll();

    /**
     * Load all performances based on artists.
     *
     * @param artistId the artist of which the performances are sought after
     * @return performances
     */
    List<Performance> findByArtistId(Long artistId);

    /**
     * Delete a performance by id.
     *
     * @param id id of the performance to delete
     */
    void deletePerformance(Long id);

    /**
     * Get the hall layout for a specific performance.
     *
     * @param performanceId id of the performance
     * @return hall layout with sectors and dimensions
     */
    PerformanceHallLayoutDto getHallLayout(Long performanceId);

    /**
     * Get the status of all seats in a specific sector for a performance.
     *
     * @param performanceId id of the performance
     * @param sectorId id of the sector
     * @param email of user
     * @return list of seat statuses
     */
    List<SeatStatusDto> getSeatsForSector(Long performanceId, Long sectorId, String email);
}
