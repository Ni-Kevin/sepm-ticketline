package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallAreaType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.HallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HallServiceImpl implements HallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final HallRepository hallRepository;
    private final VenueRepository venueRepository;
    private final SectorRepository sectorRepository;
    private final PerformanceRepository performanceRepository;

    public HallServiceImpl(HallRepository hallRepository, VenueRepository venueRepository, SectorRepository sectorRepository,
                           PerformanceRepository performanceRepository) {
        this.hallRepository = hallRepository;
        this.venueRepository = venueRepository;
        this.sectorRepository = sectorRepository;
        this.performanceRepository = performanceRepository;
    }

    @Override
    public List<Hall> findAll(Long venueId) {
        LOGGER.debug("Find halls for venueId {}", venueId);
        if (venueId == null) {
            return hallRepository.findAllByOrderByNameAsc();
        }
        return hallRepository.findByVenueIdOrderByNameAsc(venueId);
    }

    @Override
    public Hall findOne(Long id) {
        LOGGER.debug("Find hall with id {}", id);
        return hallRepository.findWithVenueById(id)
            .orElseThrow(() -> new NotFoundException(String.format("Could not find hall with id %s", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Hall findLayout(Long id) {
        Hall hall = findOne(id);
        hall.getSectors().size();
        hall.getAreas().size();
        hall.getSectors().forEach(sector -> sector.getSeats().size());
        return hall;
    }

    @Override
    public boolean isLayoutLocked(Long id) {
        return performanceRepository.existsByHallId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDimensionsLocked(Long id) {
        Hall hall = findOne(id);
        return isDimensionsLocked(hall);
    }

    private boolean isDimensionsLocked(Hall hall) {
        return isLayoutLocked(hall.getId()) || !hall.getAreas().isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public String getDimensionsLockReason(Long id) {
        Hall hall = findOne(id);
        return getDimensionsLockReason(hall);
    }

    private String getDimensionsLockReason(Hall hall) {
        if (isLayoutLocked(hall.getId())) {
            return "PERFORMANCE";
        }
        if (!hall.getAreas().isEmpty()) {
            return "AREAS";
        }
        return null;
    }

    @Override
    @Transactional
    public Hall create(Hall hall, Long venueId) throws ValidationException {
        LOGGER.debug("Create hall {} for venue {}", hall, venueId);
        Venue venue = findVenueOrThrow(venueId);
        validateUniqueHallName(venueId, hall.getName(), null);
        hall.setVenue(venue);
        Hall savedHall = hallRepository.save(hall);
        return hallRepository.findWithVenueById(savedHall.getId())
            .orElseThrow(() -> new NotFoundException(String.format("Could not find hall with id %s", savedHall.getId())));
    }

    @Override
    @Transactional
    public Hall update(Long id, Hall hall, Long venueId) throws ValidationException {
        LOGGER.debug("Update hall with id {}", id);
        Hall existingHall = findOne(id);
        validateDimensionsUnlocked(existingHall, hall);
        validateUniqueHallName(venueId, hall.getName(), id);
        Venue venue = findVenueOrThrow(venueId);
        existingHall.setName(hall.getName());
        existingHall.setWidth(hall.getWidth());
        existingHall.setLength(hall.getLength());
        existingHall.setVenue(venue);
        Hall savedHall = hallRepository.save(existingHall);
        return hallRepository.findWithVenueById(savedHall.getId())
            .orElseThrow(() -> new NotFoundException(String.format("Could not find hall with id %s", savedHall.getId())));
    }

    @Override
    @Transactional
    public Hall updateLayout(Long id, List<Sector> sectors, List<HallArea> areas) throws ValidationException {
        LOGGER.debug("Update hall layout for hall {}", id);
        Hall hall = findOne(id);
        validateLayoutUnlocked(hall.getId());
        validateLayout(hall, sectors, areas);

        Map<Long, Sector> existingSectorsById = hall.getSectors().stream()
            .filter(sector -> sector.getId() != null)
            .collect(Collectors.toMap(Sector::getId, Function.identity()));

        List<Sector> updatedSectors = new ArrayList<>();
        Map<String, List<Seat>> requestedSeatsBySectorName = new java.util.HashMap<>();
        for (Sector sector : sectors) {
            Sector targetSector = sector.getId() != null && existingSectorsById.containsKey(sector.getId())
                ? existingSectorsById.get(sector.getId())
                : new Sector();

            targetSector.setName(sector.getName());
            targetSector.setType(sector.getType());
            targetSector.setColor(sector.getColor());
            targetSector.setCapacity(sector.getCapacity());
            targetSector.setHall(hall);
            List<Seat> requestedSeats = sector.getSeats().stream()
                .map(seat -> {
                    Seat requestedSeat = new Seat();
                    requestedSeat.setRowNumber(seat.getRowNumber());
                    requestedSeat.setSeatNumber(seat.getSeatNumber());
                    requestedSeat.setPositionX(seat.getPositionX());
                    requestedSeat.setPositionY(seat.getPositionY());
                    return requestedSeat;
                })
                .toList();
            requestedSeatsBySectorName.put(sector.getName().toLowerCase(), requestedSeats);
            targetSector.getSeats().clear();
            updatedSectors.add(targetSector);
        }

        var updatedSectorIds = updatedSectors.stream()
            .map(Sector::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (HallArea existingArea : hall.getAreas()) {
            if (existingArea.getSector() != null
                && existingArea.getSector().getId() != null
                && !updatedSectorIds.contains(existingArea.getSector().getId())) {
                existingArea.setSector(null);
            }
        }

        hallRepository.saveAndFlush(hall);

        hall.getSectors().clear();
        hall.getSectors().addAll(updatedSectors);

        hall = hallRepository.saveAndFlush(hall);

        for (Sector sector : hall.getSectors()) {
            List<Seat> requestedSeats = requestedSeatsBySectorName.getOrDefault(sector.getName().toLowerCase(), List.of());
            for (Seat seat : requestedSeats) {
                Seat targetSeat = new Seat();
                targetSeat.setRowNumber(seat.getRowNumber());
                targetSeat.setSeatNumber(seat.getSeatNumber());
                targetSeat.setPositionX(seat.getPositionX());
                targetSeat.setPositionY(seat.getPositionY());
                targetSeat.setSector(sector);
                sector.getSeats().add(targetSeat);
            }
        }

        Map<Long, Sector> persistedSectorsById = hallRepository.save(hall).getSectors().stream()
            .filter(sector -> sector.getId() != null)
            .collect(Collectors.toMap(Sector::getId, Function.identity()));
        Map<String, Sector> persistedSectorsByName = hall.getSectors().stream()
            .filter(sector -> sector.getName() != null)
            .collect(Collectors.toMap(sector -> sector.getName().toLowerCase(), java.util.function.Function.identity(), (left, right) -> left));

        Map<Long, HallArea> existingAreasById = hall.getAreas().stream()
            .filter(area -> area.getId() != null)
            .collect(Collectors.toMap(HallArea::getId, Function.identity()));

        List<HallArea> updatedAreas = new ArrayList<>();
        for (HallArea area : areas) {
            HallArea targetArea = area.getId() != null && existingAreasById.containsKey(area.getId())
                ? existingAreasById.get(area.getId())
                : new HallArea();
            targetArea.setPositionX(area.getPositionX());
            targetArea.setPositionY(area.getPositionY());
            targetArea.setWidth(area.getWidth());
            targetArea.setLength(area.getLength());
            targetArea.setType(area.getType());
            targetArea.setHall(hall);
            if (area.getSector() != null && area.getSector().getId() != null) {
                targetArea.setSector(persistedSectorsById.get(area.getSector().getId()));
            } else if (area.getSector() != null && area.getSector().getName() != null) {
                targetArea.setSector(persistedSectorsByName.get(area.getSector().getName().toLowerCase()));
            } else {
                targetArea.setSector(null);
            }
            updatedAreas.add(targetArea);
        }

        hall.getAreas().removeIf(existingArea -> !updatedAreas.contains(existingArea));
        for (HallArea updatedArea : updatedAreas) {
            if (!hall.getAreas().contains(updatedArea)) {
                hall.getAreas().add(updatedArea);
            }
        }
        return hallRepository.save(hall);
    }

    private Venue findVenueOrThrow(Long venueId) {
        return venueRepository.findById(venueId)
            .orElseThrow(() -> new NotFoundException(String.format("Could not find venue with id %s", venueId)));
    }

    private void validateLayoutUnlocked(Long hallId) throws ValidationException {
        if (isLayoutLocked(hallId)) {
            throw new ValidationException(
                "Hall layout is locked",
                List.of("Hall layout can no longer be changed because this hall is already used in a performance")
            );
        }
    }

    private void validateDimensionsUnlocked(Hall existingHall, Hall updatedHall) throws ValidationException {
        boolean dimensionsChanged = !Objects.equals(existingHall.getWidth(), updatedHall.getWidth())
            || !Objects.equals(existingHall.getLength(), updatedHall.getLength());

        if (dimensionsChanged && isDimensionsLocked(existingHall)) {
            String reason = getDimensionsLockReason(existingHall);
            throw new ValidationException(
                "Hall dimensions are locked",
                List.of(
                    "PERFORMANCE".equals(reason)
                        ? "Hall width and length can no longer be changed because this hall is already used in a performance"
                        : "Hall width and length can no longer be changed because this hall already has configured areas"
                )
            );
        }
    }

    private void validateUniqueHallName(Long venueId, String name, Long currentHallId) throws ValidationException {
        List<Hall> halls = hallRepository.findByVenueIdOrderByNameAsc(venueId);
        boolean duplicateExists = halls.stream()
            .anyMatch(hall -> hall.getName().equalsIgnoreCase(name)
                && (currentHallId == null || !hall.getId().equals(currentHallId)));

        if (duplicateExists) {
            throw new ValidationException(
                "Hall validation failed",
                List.of(String.format("A hall with the name '%s' already exists in the selected venue", name))
            );
        }
    }

    private void validateLayout(Hall hall, List<Sector> sectors, List<HallArea> areas) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (areas.stream().noneMatch(area -> area.getType() == HallAreaType.STAGE)) {
            errors.add("Each hall layout must contain at least one stage area");
        }

        long distinctNames = sectors.stream()
            .map(Sector::getName)
            .filter(name -> name != null)
            .map(String::toLowerCase)
            .distinct()
            .count();
        if (distinctNames != sectors.size()) {
            errors.add("Sector names must be unique within the hall layout");
        }

        for (Sector sector : sectors) {
            if (sector.getType() == SectorType.STANDING && sector.getCapacity() == null) {
                errors.add(String.format("Standing sector '%s' must define a capacity", sector.getName()));
            }
        }

        for (HallArea area : areas) {
            if (area.getType() == HallAreaType.STAGE && area.getSector() != null) {
                errors.add("Stage areas must not be linked to a sector");
            }
            if (area.getType() == HallAreaType.STANDING && area.getSector() == null) {
                errors.add("Standing areas must be linked to a standing sector");
            }
            if (area.getType() == HallAreaType.STANDING && area.getSector() != null
                && area.getSector().getType() != SectorType.STANDING) {
                errors.add("Standing areas must be linked to a standing sector");
            }
            if (area.getType() == HallAreaType.SEATING && area.getSector() == null) {
                errors.add("Seating areas must be linked to a seated sector");
            }
            if (area.getType() == HallAreaType.SEATING && area.getSector() != null
                && area.getSector().getType() != SectorType.SEATING) {
                errors.add("Seating areas must be linked to a seated sector");
            }
            if (area.getPositionX() < 0 || area.getPositionY() < 0) {
                errors.add(String.format("Area position (%d,%d) must not be negative",
                    area.getPositionX(), area.getPositionY()));
            }
            if (area.getPositionX() + area.getWidth() > hall.getWidth()) {
                errors.add(String.format("Area at x=%d with width=%d exceeds hall width %d",
                    area.getPositionX(), area.getWidth(), hall.getWidth()));
            }
            if (area.getPositionY() + area.getLength() > hall.getLength()) {
                errors.add(String.format("Area at y=%d with length=%d exceeds hall length %d",
                    area.getPositionY(), area.getLength(), hall.getLength()));
            }
        }

        for (int i = 0; i < areas.size(); i++) {
            for (int j = i + 1; j < areas.size(); j++) {
                HallArea left = areas.get(i);
                HallArea right = areas.get(j);
                if (overlaps(left, right)) {
                    errors.add(String.format("Areas at (%d,%d) and (%d,%d) must not overlap",
                        left.getPositionX(), left.getPositionY(), right.getPositionX(), right.getPositionY()));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Hall layout validation failed", errors);
        }
    }

    private boolean overlaps(HallArea left, HallArea right) {
        return left.getPositionX() < right.getPositionX() + right.getWidth()
            && left.getPositionX() + left.getWidth() > right.getPositionX()
            && left.getPositionY() < right.getPositionY() + right.getLength()
            && left.getPositionY() + left.getLength() > right.getPositionY();
    }
}
