package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventMonthlySalesDto;
import at.ac.tuwien.sepr.groupphase.backend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.PermitAll;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final StatisticsService statisticsService;

    public StatisticsEndpoint(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @PermitAll
    @GetMapping("/event-sales")
    @Operation(summary = "Get top ten event sales for a month")
    public EventMonthlySalesDto getMonthlyEventSales(
        @RequestParam(name = "year", required = false) Integer year,
        @RequestParam(name = "month", required = false) Integer month,
        @RequestParam(name = "genres", required = false) List<String> genres
    ) {
        LOGGER.info("GET /api/v1/statistics/event-sales year={} month={} genres={}", year, month, genres);
        return statisticsService.getMonthlyTopEventSales(year, month, genres);
    }
}
