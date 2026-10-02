package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventSalesStatDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventMonthlySalesDto;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.StatisticsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final int IDX_EVENT_ID = 0;
    private static final int IDX_EVENT_TITLE = 1;
    private static final int IDX_GENRE = 2;
    private static final int IDX_TICKETS_SOLD = 3;

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public StatisticsServiceImpl(TicketRepository ticketRepository, EventRepository eventRepository) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public EventMonthlySalesDto getMonthlyTopEventSales(Integer year, Integer month, List<String> genres) {
        YearMonth selectedMonth = resolveMonth(year, month);
        LocalDateTime from = selectedMonth.atDay(1).atStartOfDay();
        LocalDateTime to = selectedMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<String> availableGenres = eventRepository.findGenres();
        List<String> selectedGenres = filterValidGenres(genres, availableGenres);

        final List<EventSalesStatDto> topOverall = mapSales(ticketRepository.findTopEventSalesByMonth(from, to, null, PageRequest.of(0, 10)));

        Map<String, List<EventSalesStatDto>> topByGenre = new LinkedHashMap<>();
        for (String genre : selectedGenres) {
            topByGenre.put(genre,
                mapSales(ticketRepository.findTopEventSalesByMonth(from, to, genre, PageRequest.of(0, 10))));
        }

        EventMonthlySalesDto result = new EventMonthlySalesDto();
        result.setYear(selectedMonth.getYear());
        result.setMonth(selectedMonth.getMonthValue());
        result.setTopOverall(topOverall);
        result.setTopByGenre(topByGenre);
        return result;
    }

    private YearMonth resolveMonth(Integer year, Integer month) {
        YearMonth now = YearMonth.from(LocalDate.now());
        if (year == null || month == null) {
            return now;
        }
        if (month < 1 || month > 12) {
            return now;
        }
        return YearMonth.of(year, month);
    }

    private List<String> filterValidGenres(List<String> genres, List<String> availableGenres) {
        if (genres == null || genres.isEmpty()) {
            return availableGenres;
        }

        List<String> filtered = new ArrayList<>();
        for (String genre : genres) {
            if (genre != null && !genre.isBlank()) {
                for (String availableGenre : availableGenres) {
                    if (availableGenre.equalsIgnoreCase(genre.trim())) {
                        filtered.add(availableGenre);
                    }
                }
            }
        }

        return filtered;
    }

    private List<EventSalesStatDto> mapSales(List<Object[]> rows) {
        List<EventSalesStatDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            EventSalesStatDto dto = new EventSalesStatDto();
            dto.setEventId((Long) row[IDX_EVENT_ID]);
            dto.setEventTitle((String) row[IDX_EVENT_TITLE]);
            dto.setGenre((String) row[IDX_GENRE]);
            dto.setTicketsSold((Long) row[IDX_TICKETS_SOLD]);
            result.add(dto);
        }
        return result;
    }

}
