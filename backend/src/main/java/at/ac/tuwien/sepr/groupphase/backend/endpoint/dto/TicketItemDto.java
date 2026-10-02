package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.math.BigDecimal;

public class TicketItemDto {
    private Long ticketId;
    private String sectorName;
    private String seatName;
    private Integer quantity;
    private BigDecimal price;

    public TicketItemDto() {
    }

    public TicketItemDto(Long ticketId, String sectorName, String seatName, Integer quantity, BigDecimal price) {
        this.ticketId = ticketId;
        this.sectorName = sectorName;
        this.seatName = seatName;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public String getSeatName() {
        return seatName;
    }

    public void setSeatName(String seatName) {
        this.seatName = seatName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
