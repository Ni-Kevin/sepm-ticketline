package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceHallLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatStatusDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.PerformanceMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST endpoint for performance operations.
 */
@RestController
@RequestMapping(value = "/api/v1/performances")
public class PerformanceEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final PerformanceService performanceService;
    private final PerformanceMapper performanceMapper;

    public PerformanceEndpoint(PerformanceService performanceService, PerformanceMapper performanceMapper) {
        this.performanceService = performanceService;
        this.performanceMapper = performanceMapper;
    }

    /**
     * Create a new performance.
     *
     * @param performanceCreateDto performance payload
     * @return persisted performance
     */
    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(summary = "Create a new performance", security = @SecurityRequirement(name = "apiKey"))
    public PerformanceDetailDto create(@Valid @RequestBody PerformanceCreateDto performanceCreateDto) {
        LOGGER.info("POST /api/v1/performances body: {}", performanceCreateDto);
        Performance performance = performanceService.createPerformance(
            performanceMapper.performanceCreateDtoToPerformance(performanceCreateDto),
            performanceCreateDto.getArtistIds(),
            performanceMapper.performanceSectorPriceDtosToPerformanceSectorPrices(performanceCreateDto.getSectorPrices()),
            performanceCreateDto.getDurationHours(),
            performanceCreateDto.getDurationMinutes());
        return performanceMapper.performanceToPerformanceDetailDto(performance);
    }

    /**
     * Load performances by ids.
     *
     * @param ids performance ids
     * @return matching performances
     */
    @GetMapping
    @PermitAll
    @Operation(summary = "Get performances")
    public List<PerformanceDetailDto> findByIds(@RequestParam(name = "ids", required = false) List<Long> ids) {
        LOGGER.info("GET /api/v1/performances ids={}", ids);
        List<Performance> performances = ids == null ? performanceService.findAll() : performanceService.findByIds(ids);
        return performances.stream()
            .map(performanceMapper::performanceToPerformanceDetailDto)
            .toList();
    }

    /**
     * Delete a performance by id.
     *
     * @param id performance id
     */
    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Delete a performance", security = @SecurityRequirement(name = "apiKey"))
    public void delete(@PathVariable(name = "id") Long id) {
        LOGGER.info("DELETE /api/v1/performances/{}", id);
        performanceService.deletePerformance(id);
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping(value = "/{id}/layout")
    @Operation(summary = "Get hall layout for a performance", security = @SecurityRequirement(name = "apiKey"))
    public PerformanceHallLayoutDto getLayout(@PathVariable(name = "id") Long id) {
        LOGGER.info("GET /api/v1/performances/{} /layout", id);
        return performanceService.getHallLayout(id);
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping(value = "/{id}/sectors/{sectorId}/seats")
    @Operation(summary = "Get seat statuses for a sector in a performance", security = @SecurityRequirement(name = "apiKey"))
    public List<SeatStatusDto> getSeats(@PathVariable(name = "id") Long id, @PathVariable(name = "sectorId") Long sectorId, Authentication auth) {
        LOGGER.info("GET /api/v1/performances/{}/sectors/{}/seats", id, sectorId);
        return performanceService.getSeatsForSector(id, sectorId, auth.getName());
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping(value = "/by-artist/{artistId}")
    @Operation(summary = "Get performances for a specific artist")
    public List<PerformanceDetailDto> findByArtistId(@PathVariable(name = "artistId") Long artistId) {
        LOGGER.info("GET /api/v1/performances/by-artist/{}", artistId);
        return performanceService.findByArtistId(artistId).stream()
            .map(performanceMapper::performanceToPerformanceDetailDto)
            .toList();
    }
}
