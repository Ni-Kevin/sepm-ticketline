package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventMonthlySalesDto;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.StatisticsServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Test
    public void getMonthlyTopEventSalesReturnsCorrectStructure() {
        int year = 2026;
        int month = 6;
        List<String> genres = List.of("Rock", "Jazz");

        when(eventRepository.findGenres()).thenReturn(List.of("Rock", "Jazz"));

        List<Object[]> rockData = java.util.Collections.singletonList(
            new Object[]{1L, "Rock Concert", "Rock", 3L}
        );
        List<Object[]> jazzData = java.util.Collections.singletonList(
            new Object[]{2L, "Jazz Night", "Jazz", 1L}
        );
        List<Object[]> overallData = java.util.Arrays.asList(
            new Object[]{1L, "Rock Concert", "Rock", 3L},
            new Object[]{2L, "Jazz Night", "Jazz", 1L}
        );

        when(ticketRepository.findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), isNull(), any(Pageable.class)))
            .thenReturn(overallData);
        when(ticketRepository.findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), eq("Rock"), any(Pageable.class)))
            .thenReturn(rockData);
        when(ticketRepository.findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), eq("Jazz"), any(Pageable.class)))
            .thenReturn(jazzData);

        EventMonthlySalesDto result = statisticsService.getMonthlyTopEventSales(year, month, genres);

        assertAll(
            () -> assertEquals(year, result.getYear()),
            () -> assertEquals(month, result.getMonth()),
            () -> assertNotNull(result.getTopOverall()),
            () -> assertEquals(2, result.getTopOverall().size()),
            () -> assertEquals("Rock Concert", result.getTopOverall().get(0).getEventTitle()),
            () -> assertEquals("Jazz Night", result.getTopOverall().get(1).getEventTitle()),
            () -> assertEquals(3L, result.getTopOverall().get(0).getTicketsSold()),
            () -> assertNotNull(result.getTopByGenre()),
            () -> assertTrue(result.getTopByGenre().containsKey("Rock")),
            () -> assertTrue(result.getTopByGenre().containsKey("Jazz")),
            () -> assertEquals(1, result.getTopByGenre().get("Rock").size()),
            () -> assertEquals(1, result.getTopByGenre().get("Jazz").size())
        );

        verify(ticketRepository, times(3)).findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), any(), any(Pageable.class));
    }

    @Test
    public void getMonthlyTopEventSalesDefaultsToCurrentMonthWhenYearOrMonthIsNullOrInvalid() {
        when(eventRepository.findGenres()).thenReturn(List.of());
        when(ticketRepository.findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), any(), any(Pageable.class)))
            .thenReturn(List.of());

        EventMonthlySalesDto resultNull = statisticsService.getMonthlyTopEventSales(null, null, null);
        YearMonth current = YearMonth.from(LocalDate.now());

        assertAll(
            () -> assertEquals(current.getYear(), resultNull.getYear()),
            () -> assertEquals(current.getMonthValue(), resultNull.getMonth())
        );

        EventMonthlySalesDto resultMonth0 = statisticsService.getMonthlyTopEventSales(2026, 0, null);
        assertEquals(current.getMonthValue(), resultMonth0.getMonth());

        EventMonthlySalesDto resultMonth13 = statisticsService.getMonthlyTopEventSales(2026, 13, null);
        assertEquals(current.getMonthValue(), resultMonth13.getMonth());
    }

    @Test
    public void getMonthlyTopEventSalesQueriesAllGenresWhenGenresAreNullOrEmpty() {
        when(eventRepository.findGenres()).thenReturn(List.of("Rock", "Jazz"));
        when(ticketRepository.findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), any(), any(Pageable.class)))
            .thenReturn(List.of());

        statisticsService.getMonthlyTopEventSales(2026, 6, null);

        verify(ticketRepository, times(3)).findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), any(), any(Pageable.class));
    }

    @Test
    public void getMonthlyTopEventSalesFiltersByValidGenresOnly() {
        when(eventRepository.findGenres()).thenReturn(List.of("Rock", "Jazz"));
        when(ticketRepository.findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), any(), any(Pageable.class)))
            .thenReturn(List.of());

        List<String> mixedGenres = java.util.Arrays.asList("rock", "  ", null, "Unknown", "Jazz");
        EventMonthlySalesDto result = statisticsService.getMonthlyTopEventSales(2026, 6, mixedGenres);

        assertAll(
            () -> assertEquals(2, result.getTopByGenre().size()),
            () -> assertTrue(result.getTopByGenre().containsKey("Rock")),
            () -> assertTrue(result.getTopByGenre().containsKey("Jazz"))
        );

        verify(ticketRepository, times(3)).findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), any(), any(Pageable.class));
    }

    @Test
    public void getMonthlyTopEventSalesReturnsEmptyResultsWhenNoTicketsFound() {
        when(eventRepository.findGenres()).thenReturn(List.of("Rock"));
        when(ticketRepository.findTopEventSalesByMonth(any(LocalDateTime.class), any(LocalDateTime.class), any(), any(Pageable.class)))
            .thenReturn(List.of());

        EventMonthlySalesDto result = statisticsService.getMonthlyTopEventSales(2026, 6, List.of("Rock"));

        assertAll(
            () -> assertNotNull(result.getTopOverall()),
            () -> assertTrue(result.getTopOverall().isEmpty()),
            () -> assertNotNull(result.getTopByGenre()),
            () -> assertTrue(result.getTopByGenre().containsKey("Rock")),
            () -> assertTrue(result.getTopByGenre().get("Rock").isEmpty())
        );
    }
}
