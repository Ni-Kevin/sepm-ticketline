package at.ac.tuwien.sepr.groupphase.backend.unittests;

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
import at.ac.tuwien.sepr.groupphase.backend.service.impl.HallServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HallServiceImplTest {

    @Mock
    private HallRepository hallRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private PerformanceRepository performanceRepository;

    private HallServiceImpl hallService;

    @BeforeEach
    void setUp() {
        hallService = new HallServiceImpl(hallRepository, venueRepository, sectorRepository, performanceRepository);
    }

    @Test
    void givenValidHall_whenCreate_thenReturnsSavedHall() throws Exception {
        Venue venue = venue(1L, "Venue");
        Hall hall = hall("Hall A", 10, 20, venue);
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
        when(hallRepository.findByVenueIdOrderByNameAsc(1L)).thenReturn(List.of());
        when(hallRepository.save(any(Hall.class))).thenAnswer(inv -> {
            Hall saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(hallRepository.findWithVenueById(1L)).thenReturn(Optional.of(invocationHall(1L, hall, venue)));

        Hall created = hallService.create(hall, 1L);

        assertEquals("Hall A", created.getName());
        verify(hallRepository).save(hall);
    }

    @Test
    void givenHallUsedInPerformance_whenUpdateDimensions_thenValidationException() {
        Venue venue = venue(1L, "Venue");
        Hall existing = hall("Hall A", 10, 20, venue);
        existing.setId(7L);
        Hall updated = hall("Hall A", 11, 20, venue);

        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(existing));
        when(performanceRepository.existsByHallId(7L)).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.update(7L, updated, 1L));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("width and length can no longer be changed")));
    }

    @Test
    void givenHallWithAreas_whenUpdateDimensions_thenValidationException() {
        Venue venue = venue(1L, "Venue");
        Hall existing = hall("Hall A", 10, 20, venue);
        existing.setId(7L);
        existing.getAreas().add(area(0, 0, 2, 2, HallAreaType.STAGE));
        Hall updated = hall("Hall A", 11, 20, venue);

        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(existing));

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.update(7L, updated, 1L));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("already has configured areas")));
    }

    @Test
    void givenHallUsedInPerformance_whenUpdateNameOnly_thenUpdateSucceeds() throws Exception {
        Venue venue = venue(1L, "Venue");
        Hall existing = hall("Hall A", 10, 20, venue);
        existing.setId(7L);
        Hall updated = hall("Renamed Hall", 10, 20, venue);

        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(existing));
        when(hallRepository.findByVenueIdOrderByNameAsc(1L)).thenReturn(List.of(existing));
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
        when(hallRepository.save(any(Hall.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(existing));

        Hall result = hallService.update(7L, updated, 1L);

        assertEquals("Renamed Hall", result.getName());
    }

    @Test
    void givenDuplicateName_whenCreate_thenValidationException() {
        Venue venue = venue(1L, "Venue");
        Hall existing = hall("Hall A", 10, 20, venue);
        existing.setId(2L);
        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
        when(hallRepository.findByVenueIdOrderByNameAsc(1L)).thenReturn(List.of(existing));

        assertThrows(ValidationException.class, () -> hallService.create(hall("hall a", 10, 20, venue), 1L));
    }

    @Test
    void givenHallAndInvalidLayout_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));

        HallArea area = new HallArea();
        area.setPositionX(9);
        area.setPositionY(0);
        area.setWidth(2);
        area.setLength(2);
        area.setType(HallAreaType.STAGE);

        assertThrows(ValidationException.class, () -> hallService.updateLayout(7L, List.of(), List.of(area)));
    }

    @Test
    void givenHallAndLayoutWithoutStage_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));

        HallArea area = new HallArea();
        area.setPositionX(0);
        area.setPositionY(0);
        area.setWidth(2);
        area.setLength(2);
        area.setType(HallAreaType.SEATING);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.updateLayout(7L, List.of(), List.of(area)));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("at least one stage area")));
    }

    @Test
    void givenStandingAreaWithoutSector_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));

        HallArea stage = area(0, 0, 2, 2, HallAreaType.STAGE);
        HallArea standing = area(3, 3, 2, 2, HallAreaType.STANDING);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.updateLayout(7L, List.of(), List.of(stage, standing)));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("Standing areas must be linked")));
    }

    @Test
    void givenSeatingAreaWithoutSector_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));

        HallArea stage = area(0, 0, 2, 2, HallAreaType.STAGE);
        HallArea seating = area(3, 3, 2, 2, HallAreaType.SEATING);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.updateLayout(7L, List.of(), List.of(stage, seating)));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("Seating areas must be linked")));
    }

    @Test
    void givenStandingAreaWithWrongSectorType_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));

        Sector seatingSector = sector("A", SectorType.SEATING);
        HallArea stage = area(0, 0, 2, 2, HallAreaType.STAGE);
        HallArea standing = area(3, 3, 2, 2, HallAreaType.STANDING);
        standing.setSector(seatingSector);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.updateLayout(7L, List.of(seatingSector), List.of(stage, standing)));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("Standing areas must be linked")));
    }

    @Test
    void givenStageAreaWithSector_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));

        Sector seatingSector = sector("A", SectorType.SEATING);
        HallArea stage = area(0, 0, 2, 2, HallAreaType.STAGE);
        stage.setSector(seatingSector);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.updateLayout(7L, List.of(seatingSector), List.of(stage)));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("Stage areas must not be linked to a sector")));
    }

    @Test
    void givenValidLayout_whenUpdateLayout_thenPersistsSectorsAreasAndSeats() throws Exception {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));
        when(performanceRepository.existsByHallId(7L)).thenReturn(false);
        when(hallRepository.saveAndFlush(any(Hall.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hallRepository.save(any(Hall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sector seatingSector = sector("A", SectorType.SEATING);
        seatingSector.getSeats().add(seat(1, 1, 3, 3));
        seatingSector.getSeats().add(seat(1, 2, 4, 3));

        HallArea stage = area(0, 0, 2, 2, HallAreaType.STAGE);
        HallArea seating = area(3, 3, 3, 3, HallAreaType.SEATING);
        seating.setSector(seatingSector);

        Hall updated = hallService.updateLayout(7L, List.of(seatingSector), List.of(stage, seating));

        assertAll(
            () -> assertEquals(1, updated.getSectors().size()),
            () -> assertEquals("A", updated.getSectors().get(0).getName()),
            () -> assertEquals(2, updated.getSectors().get(0).getSeats().size()),
            () -> assertEquals(2, updated.getAreas().size()),
            () -> assertTrue(updated.getAreas().stream().anyMatch(area -> area.getType() == HallAreaType.STAGE && area.getSector() == null)),
            () -> assertTrue(updated.getAreas().stream().anyMatch(area -> area.getType() == HallAreaType.SEATING
                && area.getSector() != null
                && "A".equals(area.getSector().getName())))
        );
    }

    @Test
    void givenHallUsedInPerformance_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));
        when(performanceRepository.existsByHallId(7L)).thenReturn(true);

        HallArea stage = area(0, 0, 2, 2, HallAreaType.STAGE);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.updateLayout(7L, List.of(), List.of(stage)));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("already used in a performance")));
    }

    @Test
    void givenHallAndOverlappingAreas_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));

        HallArea stage = new HallArea();
        stage.setPositionX(0);
        stage.setPositionY(0);
        stage.setWidth(4);
        stage.setLength(4);
        stage.setType(HallAreaType.STAGE);

        HallArea standing = new HallArea();
        standing.setPositionX(3);
        standing.setPositionY(3);
        standing.setWidth(4);
        standing.setLength(4);
        standing.setType(HallAreaType.STANDING);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.updateLayout(7L, List.of(), List.of(stage, standing)));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("must not overlap")));
    }

    @Test
    void givenHallAndNegativeAreaPosition_whenUpdateLayout_thenValidationException() {
        Hall hall = hall("Hall A", 10, 20, venue(1L, "Venue"));
        hall.setId(7L);
        when(hallRepository.findWithVenueById(7L)).thenReturn(Optional.of(hall));

        HallArea stage = new HallArea();
        stage.setPositionX(-1);
        stage.setPositionY(0);
        stage.setWidth(2);
        stage.setLength(2);
        stage.setType(HallAreaType.STAGE);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> hallService.updateLayout(7L, List.of(), List.of(stage)));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("must not be negative")));
    }

    @Test
    void givenMissingHall_whenFindOne_thenNotFound() {
        when(hallRepository.findWithVenueById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> hallService.findOne(99L));
    }

    @Test
    void givenMissingHall_whenUpdate_thenNotFound() {
        when(hallRepository.findWithVenueById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> hallService.update(99L, hall("X", 1, 1, venue(1L, "V")), 1L));
    }

    private Hall hall(String name, int width, int length, Venue venue) {
        Hall hall = new Hall();
        hall.setName(name);
        hall.setWidth(width);
        hall.setLength(length);
        hall.setVenue(venue);
        return hall;
    }

    private HallArea area(int x, int y, int width, int length, HallAreaType type) {
        HallArea area = new HallArea();
        area.setPositionX(x);
        area.setPositionY(y);
        area.setWidth(width);
        area.setLength(length);
        area.setType(type);
        return area;
    }

    private Sector sector(String name, SectorType type) {
        Sector sector = new Sector();
        sector.setName(name);
        sector.setType(type);
        sector.setColor("#123456");
        return sector;
    }

    private Seat seat(int rowNumber, int seatNumber, int x, int y) {
        Seat seat = new Seat();
        seat.setRowNumber(rowNumber);
        seat.setSeatNumber(seatNumber);
        seat.setPositionX(x);
        seat.setPositionY(y);
        return seat;
    }

    private Venue venue(Long id, String name) {
        Venue venue = new Venue();
        venue.setId(id);
        venue.setName(name);
        return venue;
    }

    private Hall invocationHall(Long id, Hall hall, Venue venue) {
        Hall saved = new Hall();
        saved.setId(id);
        saved.setName(hall.getName());
        saved.setWidth(hall.getWidth());
        saved.setLength(hall.getLength());
        saved.setVenue(venue);
        return saved;
    }
}
