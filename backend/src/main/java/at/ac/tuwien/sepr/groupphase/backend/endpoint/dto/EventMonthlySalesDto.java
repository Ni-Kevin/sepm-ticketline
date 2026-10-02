package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.util.List;
import java.util.Map;

public class EventMonthlySalesDto {

    private int year;
    private int month;
    private List<EventSalesStatDto> topOverall;
    private Map<String, List<EventSalesStatDto>> topByGenre;

    public EventMonthlySalesDto() {
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public List<EventSalesStatDto> getTopOverall() {
        return topOverall;
    }

    public void setTopOverall(List<EventSalesStatDto> topOverall) {
        this.topOverall = topOverall;
    }

    public Map<String, List<EventSalesStatDto>> getTopByGenre() {
        return topByGenre;
    }

    public void setTopByGenre(Map<String, List<EventSalesStatDto>> topByGenre) {
        this.topByGenre = topByGenre;
    }
}
