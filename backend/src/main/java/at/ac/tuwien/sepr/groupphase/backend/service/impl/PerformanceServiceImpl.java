package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceHallLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceSectorLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceSectorPriceDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatStatusDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeatHold;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatHoldRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.PerformanceService;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementation for performance service.
 */
@Service
public class PerformanceServiceImpl implements PerformanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final PerformanceRepository performanceRepository;
    private final ArtistRepository artistRepository;
    private final HallRepository hallRepository;
    private final TicketRepository ticketRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final UserRepository userRepository;


    public PerformanceServiceImpl(PerformanceRepository performanceRepository, ArtistRepository artistRepository,
                                  TicketRepository ticketRepository,
                                  SeatHoldRepository seatHoldRepository, UserRepository userRepository, HallRepository hallRepository) {
        this.performanceRepository = performanceRepository;
        this.artistRepository = artistRepository;
        this.hallRepository = hallRepository;
        this.ticketRepository = ticketRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a performance and link all referenced artists.
     *
     * @param performance performance entity to persist
     * @param artistIds ids of artists to link
     * @return persisted performance entity
     */
    @Override
    public Performance createPerformance(Performance performance, List<Long> artistIds, List<PerformanceSectorPrice> sectorPrices,
                                         Integer durationHours, Integer durationMinutes) {
        LOGGER.debug("Create performance {}", performance);
        if (performance.getStartTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time is required");
        }
        if (durationHours == null || durationHours < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration hours must be greater than or equal to 0");
        }
        if (durationMinutes == null || durationMinutes < 0 || durationMinutes > 59) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration minutes must be between 0 and 59");
        }
        if (durationHours == 0 && durationMinutes == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be greater than 0 minutes");
        }
        performance.setEndTime(performance.getStartTime().plusHours(durationHours).plusMinutes(durationMinutes));
        Long hallId = performance.getHall() != null ? performance.getHall().getId() : null;
        if (hallId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall is required");
        }
        Hall hall = hallRepository.findById(hallId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not exist"));
        performance.setHall(hall);
        if (performanceRepository.existsOverlappingPerformanceForHall(hallId, performance.getStartTime(), performance.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall already has a performance in the selected time range");
        }
        List<Artist> artists = artistRepository.findAllById(artistIds);
        if (artists.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one artist is required");
        }
        if (performanceRepository.existsOverlappingPerformanceForAnyArtist(artistIds, performance.getStartTime(), performance.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one artist already has a performance in the selected time range");
        }
        if (sectorPrices != null) {
            for (PerformanceSectorPrice sectorPrice : sectorPrices) {
                sectorPrice.setPerformance(performance);
            }
            performance.setSectorPrices(sectorPrices);
        }
        performance.setArtists(new HashSet<>(artists));
        return performanceRepository.save(performance);
    }

    @Override
    public List<Performance> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return performanceRepository.findAllByIdInWithRelations(ids).stream()
            .sorted(Comparator.comparing(Performance::getStartTime).thenComparing(Performance::getId))
            .toList();
    }

    @Override
    public List<Performance> findAll() {
        return performanceRepository.findAllWithRelationsOrderByStartTimeAsc();
    }

    /**
     * Load all performances based on artists.
     *
     * @param artistId the artist of which the performances are sought after
     * @return performances
     */
    @Override
    public List<Performance> findByArtistId(Long artistId) {
        LOGGER.debug("Find performances for artist {}", artistId);
        if (!artistRepository.existsById(artistId)) {
            throw new NotFoundException(String.format("Could not find artist with id %s", artistId));
        }
        return performanceRepository.findAllByArtistIdWithRelations(artistId);
    }

    /**
     * Delete a performance by id.
     *
     * @param id id of the performance to delete
     */
    @Override
    public void deletePerformance(Long id) {
        LOGGER.debug("Delete performance {}", id);
        if (!performanceRepository.existsById(id)) {
            throw new NotFoundException(String.format("Could not find performance with id %s", id));
        }
        performanceRepository.deleteById(id);
    }

    @Override
    @Transactional
    public PerformanceHallLayoutDto getHallLayout(Long performanceId) {
        LOGGER.debug("Get hall layout for performance {}", performanceId);
        Performance performance = performanceRepository.findById(performanceId)
            .orElseThrow(() -> new NotFoundException("Performance not found"));
        List<PerformanceSectorPrice> prices = performance.getSectorPrices();

        Hall hall = performance.getHall();
        if (hall == null) {
            throw new NotFoundException("Performance is not assigned to a hall");
        }

        List<PerformanceSectorLayoutDto> sectorDtos = hall.getAreas().stream()
            .map(area -> {
                Sector sector = area.getSector();

                if (sector != null) {
                    BigDecimal priceForSector = prices.stream()
                        .filter(p -> Objects.equals(p.getSectorId(), sector.getId()))
                        .map(PerformanceSectorPrice::getPrice)
                        .findFirst().orElse(BigDecimal.ZERO);

                    int capacity = 0;
                    if (sector.getType().name().equals("SEATING")) {
                        capacity = sector.getSeats() != null ? sector.getSeats().size() : 0;
                    } else if (sector.getType().name().equals("STANDING")) {
                        capacity = sector.getCapacity();
                    }

                    int soldTickets = ticketRepository.countActiveTicketsForSector(performanceId, sector.getId());

                    return new PerformanceSectorLayoutDto(
                        sector.getId(),
                        sector.getName(),
                        sector.getType().name(),
                        sector.getColor(),
                        area.getPositionX(),
                        area.getPositionY(),
                        area.getWidth(),
                        area.getLength(),
                        priceForSector,
                        capacity,
                        soldTickets
                    );
                } else {
                    return new PerformanceSectorLayoutDto(
                        null,
                        "Stage",
                        "STAGE",
                        "#7f8c8d",
                        area.getPositionX(),
                        area.getPositionY(),
                        area.getWidth(),
                        area.getLength(),
                        BigDecimal.ZERO,
                        0,
                        0
                    );
                }
            })
            .collect(Collectors.toList());

        return new PerformanceHallLayoutDto(
            hall.getName(),
            hall.getWidth(),
            hall.getLength(),
            hall.getVenue().getName(),
            hall.getVenue().getStreet(),
            hall.getVenue().getCity(),
            hall.getVenue().getZipCode(),
            hall.getVenue().getCountry(),
            sectorDtos
        );
    }

    @Override
    @Transactional
    public List<SeatStatusDto> getSeatsForSector(Long performanceId, Long sectorId, String email) {
        LOGGER.debug("Get seats for sector {} in performance {}", sectorId, performanceId);

        Performance performance = performanceRepository.findById(performanceId)
            .orElseThrow(() -> new NotFoundException("Performance not found"));

        Long userId = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found")).getId();

        Sector sector = performance.getHall().getSectors().stream()
            .filter(s -> s.getId().equals(sectorId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Sector not found in the performance's hall"));

        LocalDateTime now = LocalDateTime.now();

        Set<Long> soldSeatIds = new HashSet<>(
            ticketRepository.findActiveTicketSeatIdsByPerformanceId(performanceId));
        Set<Long> heldSeatIds = new HashSet<>(
            seatHoldRepository.findActiveHoldSeatIdsByPerformanceId(performanceId, now, userId));

        return sector.getSeats().stream()
            .sorted(Comparator.comparingInt(Seat::getRowNumber)
                .thenComparingInt(Seat::getSeatNumber))
            .map(seat -> {
                String status = "AVAILABLE";

                if (soldSeatIds.contains(seat.getId())) {
                    status = "SOLD";
                } else if (heldSeatIds.contains(seat.getId())) {
                    status = "HELD";
                }

                return new SeatStatusDto(
                    seat.getId(),
                    seat.getRowNumber(),
                    seat.getSeatNumber(),
                    status,
                    seat.getPositionX(),
                    seat.getPositionY()
                );
            })
            .collect(Collectors.toList());
    }
}
