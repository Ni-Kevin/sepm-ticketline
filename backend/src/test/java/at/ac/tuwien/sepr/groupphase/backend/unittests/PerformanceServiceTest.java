package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceHallLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceSectorLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatStatusDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatHoldRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.PerformanceServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PerformanceServiceTest {

    @Mock
    private PerformanceRepository performanceRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private HallRepository hallRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SeatHoldRepository seatHoldRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PerformanceServiceImpl performanceService;

    @Test
    public void createPerformanceAssignsArtistsAndPersistsPerformance() {
        Performance performance = new Performance();
        Hall hall = new Hall();
        hall.setId(10L);
        performance.setHall(hall);
        performance.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 0));

        Artist artistOne = new Artist();
        artistOne.setId(1L);
        Artist artistTwo = new Artist();
        artistTwo.setId(2L);

        Performance savedPerformance = new Performance();
        savedPerformance.setId(40L);

        when(artistRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(artistOne, artistTwo));
        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(performanceRepository.existsOverlappingPerformanceForHall(
            10L,
            LocalDateTime.of(2026, 10, 1, 20, 0),
            LocalDateTime.of(2026, 10, 1, 21, 30)
        )).thenReturn(false);
        when(performanceRepository.existsOverlappingPerformanceForAnyArtist(
            List.of(1L, 2L),
            LocalDateTime.of(2026, 10, 1, 20, 0),
            LocalDateTime.of(2026, 10, 1, 21, 30)
        )).thenReturn(false);
        when(performanceRepository.save(performance)).thenReturn(savedPerformance);

        Performance result = performanceService.createPerformance(performance, List.of(1L, 2L), List.of(), 1, 30);

        assertAll(
            () -> assertSame(savedPerformance, result),
            () -> assertEquals(2, performance.getArtists().size())
        );
        verify(performanceRepository).save(performance);
    }

    @Test
    public void deletePerformanceDeletesPerformanceWhenIdExists() {
        when(performanceRepository.existsById(77L)).thenReturn(true);

        performanceService.deletePerformance(77L);

        verify(performanceRepository).deleteById(77L);
    }

    @Test
    public void createPerformanceThrowsBadRequestWhenHallDoesNotExist() {
        Performance performance = new Performance();
        Hall hall = new Hall();
        hall.setId(1234L);
        performance.setHall(hall);
        performance.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 0));

        when(hallRepository.findById(1234L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> performanceService.createPerformance(performance, List.of(1L), List.of(new PerformanceSectorPrice()), 1, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Hall does not exist", exception.getReason());
        verify(performanceRepository, never()).save(performance);
    }

    @Test
    public void createPerformanceThrowsBadRequestWhenHallHasOverlappingPerformance() {
        Performance performance = new Performance();
        Hall hall = new Hall();
        hall.setId(10L);
        performance.setHall(hall);
        performance.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 0));

        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(performanceRepository.existsOverlappingPerformanceForHall(
            10L,
            LocalDateTime.of(2026, 10, 1, 20, 0),
            LocalDateTime.of(2026, 10, 1, 22, 0)
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> performanceService.createPerformance(performance, List.of(1L), List.of(), 2, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Hall already has a performance in the selected time range", exception.getReason());
        verify(artistRepository, never()).findAllById(anyList());
        verify(performanceRepository, never()).save(performance);
    }

    @Test
    public void createPerformanceThrowsBadRequestWhenArtistHasOverlappingPerformance() {
        Performance performance = new Performance();
        Hall hall = new Hall();
        hall.setId(10L);
        performance.setHall(hall);
        performance.setStartTime(LocalDateTime.of(2026, 10, 1, 20, 0));

        Artist artist = new Artist();
        artist.setId(1L);

        when(hallRepository.findById(10L)).thenReturn(Optional.of(hall));
        when(performanceRepository.existsOverlappingPerformanceForHall(
            10L,
            LocalDateTime.of(2026, 10, 1, 20, 0),
            LocalDateTime.of(2026, 10, 1, 22, 0)
        )).thenReturn(false);
        when(artistRepository.findAllById(List.of(1L))).thenReturn(List.of(artist));
        when(performanceRepository.existsOverlappingPerformanceForAnyArtist(
            List.of(1L),
            LocalDateTime.of(2026, 10, 1, 20, 0),
            LocalDateTime.of(2026, 10, 1, 22, 0)
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> performanceService.createPerformance(performance, List.of(1L), List.of(), 2, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("At least one artist already has a performance in the selected time range", exception.getReason());
        verify(performanceRepository, never()).save(performance);
    }

    @Test
    public void deletePerformanceThrowsNotFoundWhenIdDoesNotExist() {
        when(performanceRepository.existsById(99L)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> performanceService.deletePerformance(99L));

        assertEquals("Could not find performance with id 99", exception.getMessage());
        verify(performanceRepository, never()).deleteById(99L);
    }

    @Test
    public void findByIdsReturnsEmptyListWhenIdsAreNull() {
        List<Performance> result = performanceService.findByIds(null);

        assertTrue(result.isEmpty());
        verify(performanceRepository, never()).findAllByIdInWithRelations(anyList());
    }

    @Test
    public void findByIdsReturnsSortedByStartTimeAndId() {
        Performance later = new Performance();
        later.setId(3L);
        later.setStartTime(LocalDateTime.of(2026, 10, 1, 21, 0));

        Performance sameTimeHigherId = new Performance();
        sameTimeHigherId.setId(2L);
        sameTimeHigherId.setStartTime(LocalDateTime.of(2026, 10, 1, 19, 0));

        Performance sameTimeLowerId = new Performance();
        sameTimeLowerId.setId(1L);
        sameTimeLowerId.setStartTime(LocalDateTime.of(2026, 10, 1, 19, 0));

        when(performanceRepository.findAllByIdInWithRelations(List.of(1L, 2L, 3L)))
            .thenReturn(List.of(later, sameTimeHigherId, sameTimeLowerId));

        List<Performance> result = performanceService.findByIds(List.of(1L, 2L, 3L));

        assertAll(
            () -> assertEquals(3, result.size()),
            () -> assertEquals(1L, result.get(0).getId()),
            () -> assertEquals(2L, result.get(1).getId()),
            () -> assertEquals(3L, result.get(2).getId())
        );
    }

    @Test
    public void findAllReturnsRepositoryResult() {
        List<Performance> performances = List.of(new Performance(), new Performance());
        when(performanceRepository.findAllWithRelationsOrderByStartTimeAsc()).thenReturn(performances);

        List<Performance> result = performanceService.findAll();

        assertSame(performances, result);
        verify(performanceRepository).findAllWithRelationsOrderByStartTimeAsc();
    }

    @Test
    public void getHallLayoutReturnsCorrectDtoWhenPerformanceAndHallExist() {
        Performance performance = new Performance();
        performance.setId(1L);
        performance.setSectorPrices(new ArrayList<>());

        Hall hall = new Hall();
        hall.setId(2L);
        hall.setName("Main Hall");

        Venue venue = new Venue();
        venue.setName("Venue A");
        venue.setStreet("Street 1");
        venue.setZipCode("1010");
        venue.setCity("Vienna");
        venue.setCountry("Austria");
        hall.setVenue(venue);

        hall.setWidth(10);
        hall.setLength(20);
        performance.setHall(hall);

        Sector sector = new Sector();
        sector.setId(3L);
        sector.setName("Sektor A");
        sector.setType(SectorType.SEATING);
        sector.setColor("#FFFFFF");
        hall.setSectors(List.of(sector));

        PerformanceSectorPrice sectorPrice = new PerformanceSectorPrice();
        sectorPrice.setPrice(BigDecimal.valueOf(45.0));
        sectorPrice.setSectorId(3L);
        performance.getSectorPrices().add(sectorPrice);

        HallArea area = new HallArea();
        area.setSector(sector);
        area.setPositionX(10);
        area.setPositionY(20);
        area.setWidth(5);
        area.setLength(10);
        hall.setAreas(List.of(area));

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(ticketRepository.countActiveTicketsForSector(1L, 3L)).thenReturn(5);

        PerformanceHallLayoutDto result = performanceService.getHallLayout(1L);

        PerformanceSectorLayoutDto sectorDto = result.getSectors().get(0);
        assertAll("Hall Layout Properties",
            () -> assertEquals("Main Hall", result.getHallName()),
            () -> assertEquals(1, result.getSectors().size()),
            () -> assertEquals("Sektor A", sectorDto.getName()),
            () -> assertEquals(BigDecimal.valueOf(45.0), sectorDto.getPrice()),
            () -> assertEquals(5, sectorDto.getSoldTickets())
        );
    }

    @Test
    public void getHallLayoutThrowsNotFoundWhenPerformanceDoesNotExist() {
        when(performanceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> performanceService.getHallLayout(1L));
    }

    @Test
    public void getHallLayoutThrowsNotFoundWhenPerformanceHasNoHall() {
        Performance performance = new Performance();
        performance.setId(1L);
        performance.setHall(null);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));

        assertThrows(NotFoundException.class, () -> performanceService.getHallLayout(1L));
    }

    @Test
    public void getSeatsForSectorReturnsCorrectStatusWhenSeatsExist() {
        Performance performance = new Performance();
        performance.setId(1L);
        Hall hall = new Hall();
        Sector sector = new Sector();
        sector.setId(2L);
        hall.setSectors(List.of(sector));
        performance.setHall(hall);

        Seat seat = new Seat();
        seat.setId(3L);
        seat.setRowNumber(1);
        seat.setSeatNumber(1);
        sector.setSeats(List.of(seat));

        ApplicationUser user = new ApplicationUser();
        user.setId(99L);
        user.setEmail("test@user.at");

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(userRepository.findByEmail("test@user.at")).thenReturn(Optional.of(user));
        when(ticketRepository.findActiveTicketSeatIdsByPerformanceId(1L)).thenReturn(Set.of());
        when(seatHoldRepository.findActiveHoldSeatIdsByPerformanceId(eq(1L), any(LocalDateTime.class), eq(99L)))
            .thenReturn(Set.of());

        List<SeatStatusDto> result = performanceService.getSeatsForSector(1L, 2L, "test@user.at");

        assertAll("Seat Status Properties",
            () -> assertEquals(1, result.size()),
            () -> assertEquals("AVAILABLE", result.get(0).getStatus())
        );
    }

    @Test
    public void getSeatsForSectorThrowsNotFoundWhenPerformanceDoesNotExist() {
        when(performanceRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> performanceService.getSeatsForSector(1L, 2L, "test@user.at"));
    }

    @Test
    public void getSeatsForSectorThrowsNotFoundWhenSectorNotBelongsToHall() {
        Performance performance = new Performance();
        performance.setId(1L);
        Hall hall = new Hall();
        hall.setSectors(List.of());
        performance.setHall(hall);

        ApplicationUser user = new ApplicationUser();
        user.setId(99L);

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(userRepository.findByEmail("test@user.at")).thenReturn(Optional.of(user));

        assertThrows(NotFoundException.class, () -> performanceService.getSeatsForSector(1L, 2L, "test@user.at"));
    }
}
