package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventMonthlySalesDto;
import java.util.List;

public interface StatisticsService {

    EventMonthlySalesDto getMonthlyTopEventSales(Integer year, Integer month, List<String> genres);
}
