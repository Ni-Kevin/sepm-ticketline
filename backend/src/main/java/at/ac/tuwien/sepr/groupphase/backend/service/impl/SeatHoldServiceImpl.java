package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummarySeatholdDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HoldItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatHoldCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeatHold;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatHoldRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.SeatHoldService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SeatHoldServiceImpl implements SeatHoldService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final SectorRepository sectorRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final TicketRepository ticketRepository;
    private final UserService userService;

    public SeatHoldServiceImpl(PerformanceRepository performanceRepository,
                               UserRepository userRepository,
                               SeatRepository seatRepository,
                               SectorRepository sectorRepository,
                               SeatHoldRepository seatHoldRepository,
                               TicketRepository ticketRepository,
                               UserService userService) {
        this.performanceRepository = performanceRepository;
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
        this.sectorRepository = sectorRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.ticketRepository = ticketRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public List<SeatHold> createHold(Long performanceId, String email, SeatHoldCreateDto seatHoldCreateDto) throws ValidationException {
        LOGGER.trace("createHold(perf:{}, email:{}, dto:{})", performanceId, email, seatHoldCreateDto);

        ApplicationUser user = userService.findApplicationUserByEmail(email);
        Performance performance = performanceRepository.findById(performanceId)
            .orElseThrow(() -> new NotFoundException("Performance not found"));

        LocalDateTime now = LocalDateTime.now();
        List<String> validationErrors = new ArrayList<>();

        for (SeatHoldCreateDto.HoldItemRequest item : seatHoldCreateDto.getItems()) {
            Sector sector = sectorRepository.findById(item.getSectorId())
                .orElseThrow(() -> new NotFoundException("Sector not found with ID " + item.getSectorId()));

            if (sector.getType() == SectorType.SEATING) {
                Seat lockedSeat = seatRepository.findAndLockSeatById(item.getSeatId())
                    .orElseThrow(() -> new NotFoundException("Seat not found"));

                validateSeatedHold(performance, user, sector, lockedSeat, now, validationErrors);
            } else {
                Sector lockedSector = sectorRepository.findAndLockSectorById(item.getSectorId()).get();
                validateStandingHold(performance, lockedSector, item.getQuantity(), now, validationErrors);
            }
        }

        if (validationErrors.isEmpty()) {
            int existingTickets = ticketRepository.countTicketsForUserAndPerformance(user.getId(), performanceId);
            int activeHoldQty = seatHoldRepository.countActiveHoldsQuantityForUserAndPerformance(user.getId(), performanceId, now);
            int requestedQty = seatHoldCreateDto.getItems().stream()
                .mapToInt(SeatHoldCreateDto.HoldItemRequest::getQuantity)
                .sum();

            if (existingTickets + activeHoldQty + requestedQty > 10) {
                validationErrors.add(
                    "Maximum of 10 tickets per user per performance exceeded");
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Seat hold validation failed", validationErrors);
        }

        List<SeatHold> createdHolds = new ArrayList<>();
        for (SeatHoldCreateDto.HoldItemRequest item : seatHoldCreateDto.getItems()) {
            Sector sector = sectorRepository.findById(item.getSectorId()).get();

            if (sector.getType() == SectorType.SEATING) {
                Seat seat = seatRepository.findById(item.getSeatId()).get();

                seatHoldRepository.findActiveHoldBySeat(seat.getId(), performance.getId(), now)
                    .ifPresent(hold -> {
                        if (hold.getUser().getId().equals(user.getId())) {
                            seatHoldRepository.delete(hold);
                        }
                    });
                createdHolds.add(saveNewHold(performance, user, seat, sector, 1, now));
            } else {
                List<SeatHold> existingUserHolds = seatHoldRepository.findActiveHoldsByUserSectorAndPerformance(
                    user.getId(), sector.getId(), performance.getId(), now);

                if (!existingUserHolds.isEmpty()) {
                    seatHoldRepository.deleteAll(existingUserHolds);
                }

                createdHolds.add(saveNewHold(performance, user, null, sector, item.getQuantity(), now));
            }
        }

        seatHoldRepository.flush();
        return createdHolds;
    }

    private void validateSeatedHold(Performance performance, ApplicationUser user, Sector sector, Seat seat, LocalDateTime now, List<String> validationErrors) {
        if (seat == null) {
            validationErrors.add("Seat must be provided for seated sectors in sector " + sector.getName());
            return;
        }


        if (!seat.getSector().getId().equals(sector.getId())) {
            validationErrors.add("The selected seat " + seat + " does not belong to sector " + sector.getName());
        }

        if (ticketRepository.existsActiveTicketForSeat(performance.getId(), seat.getId())) {
            validationErrors.add("Seat " + seat + " is already sold");
        }

        seatHoldRepository.findActiveHoldBySeat(seat.getId(), performance.getId(), now)
            .ifPresent(hold -> {
                if (!hold.getUser().getId().equals(user.getId())) {
                    validationErrors.add("Seat " + seat + " is currently held by another user");
                }
            });
    }

    private void validateStandingHold(Performance performance, Sector sector, Integer quantity, LocalDateTime now, List<String> validationErrors) {
        if (quantity == null || quantity <= 0) {
            validationErrors.add("A valid quantity must be provided for standing sector " + sector.getName());
            return;
        }

        int soldTickets = ticketRepository.countActiveTicketsForSector(performance.getId(), sector.getId());
        int activeHolds = seatHoldRepository.countActiveHoldsForSector(sector.getId(), performance.getId(), now);

        if (soldTickets + activeHolds + quantity > sector.getCapacity()) {
            validationErrors.add("Not enough capacity left in standing sector " + sector.getName());
        }
    }

    private SeatHold saveNewHold(Performance perf, ApplicationUser user, Seat seat, Sector sector, int qty, LocalDateTime now) {
        SeatHold hold = new SeatHold();
        hold.setPerformance(perf);
        hold.setUser(user);
        hold.setSeat(seat);
        hold.setSector(sector);
        hold.setActive(true);
        hold.setQuantity(qty);
        hold.setExpiresAt(now.plusMinutes(15));
        return seatHoldRepository.save(hold);
    }

    @Override
    public boolean isHoldValid(Long id, Long userId) {
        LOGGER.trace("isHoldValid(holdId:{}, userId:{})", id, userId);

        return seatHoldRepository.findById(id)
            .map(hold -> {
                boolean belongsToUser = hold.getUser().getId().equals(userId);
                boolean isNotExpired = hold.getExpiresAt().isAfter(LocalDateTime.now());
                if (!belongsToUser) {
                    LOGGER.warn("Security Alert: User {} tried to access Hold {} belonging to User {}",
                        userId, id, hold.getUser().getId());
                }
                return belongsToUser && isNotExpired;
            })
            .orElse(false);
    }

    @Override
    @Transactional
    public void releaseHold(Long id) {
        if (!seatHoldRepository.existsById(id)) {
            throw new NotFoundException("Hold not found");
        }
        seatHoldRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void releaseAllHoldsForPerformanceAndUser(Long performanceId, Long userId) {
        LOGGER.debug("Release all holds for user {} and performance {}", userId, performanceId);
        seatHoldRepository.deleteActiveHoldsByPerformanceAndUser(performanceId, userId, LocalDateTime.now());
    }


    @Override
    @Transactional(readOnly = true)
    public CheckoutSummarySeatholdDto getCheckoutSummary(Long performanceId, Long userId) {
        LOGGER.trace("getCheckoutSummary(perf:{}, user:{})", performanceId, userId);

        LocalDateTime now = LocalDateTime.now();
        List<SeatHold> activeHolds = seatHoldRepository.findActiveHoldsByUserAndPerformance(userId, performanceId, now);

        if (activeHolds.isEmpty()) {
            throw new NotFoundException("NO holds not found");
        }

        Performance performance = performanceRepository.findById(performanceId)
            .orElseThrow(() -> new NotFoundException("Performance not found"));

        List<HoldItemDto> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        LocalDateTime earliestExpiry = null;

        for (SeatHold hold : activeHolds) {
            BigDecimal itemPrice = calculateHoldPrice(hold, performance.getStartPrice());
            BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(hold.getQuantity()));

            String seatName = null;
            if (hold.getSeat() != null) {
                seatName = String.format("Row %d, Seat %d",
                    hold.getSeat().getRowNumber(),
                    hold.getSeat().getSeatNumber());
            }

            items.add(new HoldItemDto(
                hold.getId(),
                hold.getSector().getName(),
                seatName,
                hold.getQuantity(),
                lineTotal
            ));

            total = total.add(lineTotal);

            if (earliestExpiry == null || hold.getExpiresAt().isBefore(earliestExpiry)) {
                earliestExpiry = hold.getExpiresAt();
            }
        }

        List<String> artists = performance.getArtists().stream()
            .map(Artist::getArtistName)
            .collect(Collectors.toList());

        String venueName = activeHolds.get(0).getSector().getHall().getVenue().getName();

        return new CheckoutSummarySeatholdDto(
            items,
            total,
            earliestExpiry,
            artists,
            performance.getEvent().getTitle(),
            performance.getPerformanceName(),
            performance.getStartTime(),
            performance.getHall().getName(),
            venueName
        );
    }

    private BigDecimal calculateHoldPrice(SeatHold hold, BigDecimal fallbackPrice) {
        if (hold.getSector() != null && hold.getPerformance() != null) {
            BigDecimal categoryPrice = hold.getPerformance().getSectorPrices().stream()
                .filter(p -> Objects.equals(hold.getSector().getId(), p.getSectorId()))
                .map(p -> p.getPrice())
                .findFirst()
                .orElse(fallbackPrice);

            if (categoryPrice != null) {
                return categoryPrice;
            }
        }

        return fallbackPrice;
    }
}