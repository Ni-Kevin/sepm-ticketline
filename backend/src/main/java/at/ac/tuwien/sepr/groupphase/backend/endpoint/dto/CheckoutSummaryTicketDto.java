package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CheckoutSummaryTicketDto {
    private List<TicketItemDto> items;
    private BigDecimal totalAmount;
    private LocalDateTime earliestExpiration;
    private List<String> artists;
    private String eventName;
    private String performanceName;
    private LocalDateTime performanceStartTime;
    private String hallName;
    private String venueName;

    public CheckoutSummaryTicketDto() {
    }

    public CheckoutSummaryTicketDto(List<TicketItemDto> items, BigDecimal totalAmount, LocalDateTime earliestExpiration,
                                   List<String> artists, String eventName, String performanceName, LocalDateTime performanceStartTime,
                                   String hallName, String venueName) {
        this.items = items;
        this.totalAmount = totalAmount;
        this.earliestExpiration = earliestExpiration;
        this.artists = artists;
        this.eventName = eventName;
        this.performanceName = performanceName;
        this.performanceStartTime = performanceStartTime;
        this.hallName = hallName;
        this.venueName = venueName;
    }

    public List<TicketItemDto> getItems() {
        return items;
    }

    public void setItems(List<TicketItemDto> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getEarliestExpiration() {
        return earliestExpiration;
    }

    public void setEarliestExpiration(LocalDateTime earliestExpiration) {
        this.earliestExpiration = earliestExpiration;
    }

    public List<String> getArtists() {
        return artists;
    }

    public void setArtists(List<String> artists) {
        this.artists = artists;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getPerformanceName() {
        return performanceName;
    }

    public void setPerformanceName(String performanceName) {
        this.performanceName = performanceName;
    }

    public LocalDateTime getPerformanceStartTime() {
        return performanceStartTime;
    }

    public void setPerformanceStartTime(LocalDateTime performanceStartTime) {
        this.performanceStartTime = performanceStartTime;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }
}
