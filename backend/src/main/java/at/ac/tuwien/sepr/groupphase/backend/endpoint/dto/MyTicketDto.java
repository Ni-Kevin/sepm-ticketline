package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import at.ac.tuwien.sepr.groupphase.backend.enums.TicketStatus;

import java.time.LocalDateTime;

public class MyTicketDto {

    private Long orderId;
    private Long ticketId;
    private TicketStatus ticketStatus;
    private Long performanceId;
    private Double finalTicketPrice;
    private String performanceTitle;
    private LocalDateTime performanceDate;
    private String venueName;
    private String hallName;
    private String sector;
    private String seat;
    private Long invoiceId;

    public MyTicketDto() {
    }

    public MyTicketDto(Long orderId, Long ticketId, TicketStatus ticketStatus, Long performanceId, Double finalTicketPrice,
                       String performanceTitle, LocalDateTime performanceDate, String venueName,
                       String hallName, String sector, String seat, Long invoiceId) {
        this.orderId = orderId;
        this.ticketId = ticketId;
        this.ticketStatus = ticketStatus;
        this.performanceId = performanceId;
        this.finalTicketPrice = finalTicketPrice;
        this.performanceTitle = performanceTitle;
        this.performanceDate = performanceDate;
        this.venueName = venueName;
        this.hallName = hallName;
        this.sector = sector;
        this.seat = seat;
        this.invoiceId = invoiceId;
    }

    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public Double getFinalTicketPrice() {
        return finalTicketPrice;
    }

    public void setFinalTicketPrice(Double finalTicketPrice) {
        this.finalTicketPrice = finalTicketPrice;
    }

    public String getPerformanceTitle() {
        return performanceTitle;
    }

    public void setPerformanceTitle(String performanceTitle) {
        this.performanceTitle = performanceTitle;
    }

    public LocalDateTime getPerformanceDate() {
        return performanceDate;
    }

    public void setPerformanceDate(LocalDateTime performanceDate) {
        this.performanceDate = performanceDate;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getSeat() {
        return seat;
    }

    public void setSeat(String seat) {
        this.seat = seat;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }


}
