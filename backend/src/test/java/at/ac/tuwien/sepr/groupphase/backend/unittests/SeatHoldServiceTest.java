package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummarySeatholdDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatHoldCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.*;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.*;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.SeatHoldServiceImpl;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatHoldServiceTest {

    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private SeatHoldServiceImpl seatHoldService;

    private ApplicationUser user;
    private Performance performance;
    private Sector sector;
    private Seat seat;
    private Hall hall;
    private Venue venue;
    private Event event;

    @BeforeEach
    void setUp() {
        user = new ApplicationUser();
        user.setId(1L);
        user.setEmail("test@example.com");

        event = new Event();
        event.setId(1L);
        event.setStartTime(LocalDateTime.now());
        event.setTitle("Test Event");

        performance = new Performance();
        performance.setId(1L);
        performance.setEvent(event);

        sector = new Sector();
        sector.setId(1L);
        sector.setName("Sektor A");
        sector.setType(SectorType.SEATING);
        sector.setCapacity(100);

        seat = new Seat();
        seat.setId(1L);
        seat.setSector(sector);

        venue = new Venue();
        venue.setId(1L);
        venue.setName("Venue");

        hall = new Hall();
        hall.setId(1L);
        hall.setVenue(venue);

        sector.setHall(hall);
        performance.setHall(hall);
    }

    @Test
    public void createHoldSucceedsForSeating() throws ValidationException {
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, 1L, 1);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));
        when(seatRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(seat));
        when(ticketRepository.existsActiveTicketForSeat(eq(1L), eq(1L))).thenReturn(false);
        when(seatHoldRepository.findActiveHoldBySeat(eq(1L), eq(1L), any())).thenReturn(Optional.empty());
        when(seatHoldRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        List<SeatHold> result = seatHoldService.createHold(1L, "test@example.com", dto);

        assertAll("Seating Hold Properties",
            () -> assertNotNull(result),
            () -> assertEquals(1, result.size()),
            () -> assertEquals(user, result.get(0).getUser()),
            () -> assertEquals(performance, result.get(0).getPerformance()),
            () -> assertEquals(sector, result.get(0).getSector()),
            () -> assertEquals(seat, result.get(0).getSeat()),
            () -> assertEquals(1, result.get(0).getQuantity())
        );
        verify(seatHoldRepository).save(any());
    }

    @Test
    public void createHoldThrowsValidationExceptionWhenSeatSold() {
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, 1L, 1);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(seatRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(seat));
        when(ticketRepository.existsActiveTicketForSeat(eq(1L), eq(1L))).thenReturn(true);

        assertThrows(ValidationException.class, () -> seatHoldService.createHold(1L, "test@example.com", dto));
    }

    @Test
    public void createHoldSucceedsForStanding() throws ValidationException {
        sector.setType(SectorType.STANDING);
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, null, 5);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepository.findAndLockSectorById(1L)).thenReturn(Optional.of(sector));
        when(ticketRepository.countActiveTicketsForSector(eq(1L), eq(1L))).thenReturn(10);
        when(seatHoldRepository.countActiveHoldsForSector(eq(1L), eq(1L), any())).thenReturn(20);
        when(seatHoldRepository.findActiveHoldsByUserSectorAndPerformance(anyLong(), anyLong(), anyLong(), any())).thenReturn(List.of());
        when(seatHoldRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        List<SeatHold> result = seatHoldService.createHold(1L, "test@example.com", dto);

        assertAll("Standing Hold Properties",
            () -> assertNotNull(result),
            () -> assertEquals(1, result.size()),
            () -> assertEquals(user, result.get(0).getUser()),
            () -> assertEquals(performance, result.get(0).getPerformance()),
            () -> assertEquals(sector, result.get(0).getSector()),
            () -> assertNull(result.get(0).getSeat()),
            () -> assertEquals(5, result.get(0).getQuantity())
        );
    }

    @Test
    public void createHoldThrowsValidationExceptionWhenStandingCapacityExceeded() {
        sector.setType(SectorType.STANDING);
        sector.setCapacity(20);
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, null, 10);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepository.findAndLockSectorById(1L)).thenReturn(Optional.of(sector));
        when(ticketRepository.countActiveTicketsForSector(eq(1L), eq(1L))).thenReturn(10);
        when(seatHoldRepository.countActiveHoldsForSector(eq(1L), eq(1L), any())).thenReturn(10);

        assertThrows(ValidationException.class, () -> seatHoldService.createHold(1L, "test@example.com", dto));
    }

    @Test
    public void releaseHoldSucceeds() {
        when(seatHoldRepository.existsById(1L)).thenReturn(true);
        seatHoldService.releaseHold(1L);
        verify(seatHoldRepository).deleteById(1L);
    }

    @Test
    public void releaseHoldThrowsNotFound() {
        when(seatHoldRepository.existsById(1L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> seatHoldService.releaseHold(1L));
    }

    @Test
    public void releaseAllHoldsForPerformanceAndUserCallsRepository() {
        seatHoldService.releaseAllHoldsForPerformanceAndUser(1L, 1L);
        verify(seatHoldRepository).deleteActiveHoldsByPerformanceAndUser(eq(1L), eq(1L), any());
    }

    @Test
    public void isHoldValidReturnsTrueWhenValid() {
        SeatHold hold = new SeatHold();
        hold.setUser(user);
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(seatHoldRepository.findById(1L)).thenReturn(Optional.of(hold));

        assertTrue(seatHoldService.isHoldValid(1L, 1L));
    }

    @Test
    public void isHoldValidReturnsFalseWhenExpired() {
        SeatHold hold = new SeatHold();
        hold.setUser(user);
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        when(seatHoldRepository.findById(1L)).thenReturn(Optional.of(hold));

        assertFalse(seatHoldService.isHoldValid(1L, 1L));
    }

    @Test
    public void getCheckoutSummaryReturnsCorrectDto() {
        SeatHold hold = new SeatHold();
        hold.setId(1L);
        hold.setQuantity(1);
        hold.setSector(sector);
        hold.setSeat(seat);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
        hold.setExpiresAt(expiry);

        when(seatHoldRepository.findActiveHoldsByUserAndPerformance(eq(1L), eq(1L), any())).thenReturn(List.of(hold));
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        performance.setStartPrice(java.math.BigDecimal.TEN);

        CheckoutSummarySeatholdDto result = seatHoldService.getCheckoutSummary(1L, 1L);

        assertAll("Checkout Summary Properties",
            () -> assertNotNull(result),
            () -> assertEquals(BigDecimal.TEN, result.getTotalAmount()),
            () -> assertEquals(1, result.getItems().size()),
            () -> assertEquals(1L, result.getItems().get(0).getHoldId()),
            () -> assertEquals("Sektor A", result.getItems().get(0).getSectorName()),
            () -> assertEquals(1, result.getItems().get(0).getQuantity())
        );
    }

    @Test
    public void getCheckoutSummaryThrowsNotFoundWhenNoHolds() {
        when(seatHoldRepository.findActiveHoldsByUserAndPerformance(eq(1L), eq(1L), any())).thenReturn(List.of());
        assertThrows(NotFoundException.class, () -> seatHoldService.getCheckoutSummary(1L, 1L));
    }

    @Test
    public void createHoldSucceedsWhenUnderLimit() throws ValidationException {
        sector.setType(SectorType.STANDING);
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, null, 3);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepository.findAndLockSectorById(1L)).thenReturn(Optional.of(sector));
        when(ticketRepository.countActiveTicketsForSector(anyLong(), anyLong())).thenReturn(0);
        when(seatHoldRepository.countActiveHoldsForSector(anyLong(), anyLong(), any())).thenReturn(0);
        when(ticketRepository.countTicketsForUserAndPerformance(anyLong(), anyLong())).thenReturn(5);
        when(seatHoldRepository.countActiveHoldsQuantityForUserAndPerformance(anyLong(), anyLong(), any())).thenReturn(0);
        when(seatHoldRepository.findActiveHoldsByUserSectorAndPerformance(anyLong(), anyLong(), anyLong(), any())).thenReturn(List.of());
        when(seatHoldRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        List<SeatHold> result = seatHoldService.createHold(1L, "test@example.com", dto);

        assertAll("Standing Hold Limit Properties",
            () -> assertNotNull(result),
            () -> assertEquals(1, result.size()),
            () -> assertEquals(3, result.get(0).getQuantity())
        );
        verify(seatHoldRepository).save(any());
    }

    @Test
    public void createHoldThrowsWhenExceedsLimit() {
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, 1L, 1);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(seatRepository.findAndLockSeatById(1L)).thenReturn(Optional.of(seat));
        when(ticketRepository.existsActiveTicketForSeat(anyLong(), anyLong())).thenReturn(false);
        when(seatHoldRepository.findActiveHoldBySeat(anyLong(), anyLong(), any())).thenReturn(Optional.empty());
        when(ticketRepository.countTicketsForUserAndPerformance(anyLong(), anyLong())).thenReturn(8);
        when(seatHoldRepository.countActiveHoldsQuantityForUserAndPerformance(anyLong(), anyLong(), any())).thenReturn(2);

        ValidationException ex = assertThrows(ValidationException.class,
            () -> seatHoldService.createHold(1L, "test@example.com", dto));
        assertTrue(ex.getMessage().contains("Maximum of 10 tickets"));
    }

    @Test
    public void createHoldSucceedsWhenAtExactLimit() throws ValidationException {
        sector.setType(SectorType.STANDING);
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, null, 3);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepository.findAndLockSectorById(1L)).thenReturn(Optional.of(sector));
        when(ticketRepository.countActiveTicketsForSector(anyLong(), anyLong())).thenReturn(0);
        when(seatHoldRepository.countActiveHoldsForSector(anyLong(), anyLong(), any())).thenReturn(0);
        when(ticketRepository.countTicketsForUserAndPerformance(anyLong(), anyLong())).thenReturn(7);
        when(seatHoldRepository.countActiveHoldsQuantityForUserAndPerformance(anyLong(), anyLong(), any())).thenReturn(0);
        when(seatHoldRepository.findActiveHoldsByUserSectorAndPerformance(anyLong(), anyLong(), anyLong(), any())).thenReturn(List.of());
        when(seatHoldRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        List<SeatHold> result = seatHoldService.createHold(1L, "test@example.com", dto);

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getQuantity());
        verify(seatHoldRepository).save(any());
    }

    @Test
    public void createHoldThrowsWhenRequestedQuantityAloneExceedsLimit() {
        sector.setType(SectorType.STANDING);
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, null, 11);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepository.findAndLockSectorById(1L)).thenReturn(Optional.of(sector));
        when(ticketRepository.countActiveTicketsForSector(anyLong(), anyLong())).thenReturn(0);
        when(seatHoldRepository.countActiveHoldsForSector(anyLong(), anyLong(), any())).thenReturn(0);
        when(ticketRepository.countTicketsForUserAndPerformance(anyLong(), anyLong())).thenReturn(0);
        when(seatHoldRepository.countActiveHoldsQuantityForUserAndPerformance(anyLong(), anyLong(), any())).thenReturn(0);

        ValidationException ex = assertThrows(ValidationException.class,
            () -> seatHoldService.createHold(1L, "test@example.com", dto));
        assertTrue(ex.getMessage().contains("Maximum of 10 tickets"));
        verify(seatHoldRepository, never()).save(any());
    }

    @Test
    public void createHoldThrowsWhenExistingTicketsAndHoldsAlreadyExceedLimit() {
        sector.setType(SectorType.STANDING);
        SeatHoldCreateDto.HoldItemRequest item = new SeatHoldCreateDto.HoldItemRequest(1L, null, 1);
        SeatHoldCreateDto dto = new SeatHoldCreateDto(List.of(item));

        when(userService.findApplicationUserByEmail(anyString())).thenReturn(user);
        when(performanceRepository.findById(1L)).thenReturn(Optional.of(performance));
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(sectorRepository.findAndLockSectorById(1L)).thenReturn(Optional.of(sector));
        when(ticketRepository.countActiveTicketsForSector(anyLong(), anyLong())).thenReturn(0);
        when(seatHoldRepository.countActiveHoldsForSector(anyLong(), anyLong(), any())).thenReturn(0);
        when(ticketRepository.countTicketsForUserAndPerformance(anyLong(), anyLong())).thenReturn(12);
        when(seatHoldRepository.countActiveHoldsQuantityForUserAndPerformance(anyLong(), anyLong(), any())).thenReturn(0);

        ValidationException ex = assertThrows(ValidationException.class,
            () -> seatHoldService.createHold(1L, "test@example.com", dto));
        assertTrue(ex.getMessage().contains("Maximum of 10 tickets"));
        verify(seatHoldRepository, never()).save(any());
    }

}
