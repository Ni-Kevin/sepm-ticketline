package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.math.BigDecimal;

public class HoldItemDto {
    private Long holdId;
    private String sectorName;
    private String seatName;
    private Integer quantity;
    private BigDecimal price;

    public HoldItemDto() {
    }

    public HoldItemDto(Long holdId, String sectorName, String seatName, Integer quantity, BigDecimal price) {
        this.holdId = holdId;
        this.sectorName = sectorName;
        this.seatName = seatName;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getHoldId() {
        return holdId;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
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