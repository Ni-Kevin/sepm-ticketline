package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.math.BigDecimal;

public class PerformanceSectorLayoutDto {
    private Long sectorId;
    private String name;
    private String type;
    private String color;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer length;
    private BigDecimal price;
    private Integer capacity;
    private Integer soldTickets;

    public PerformanceSectorLayoutDto() {
    }

    public PerformanceSectorLayoutDto(Long sectorId, String name, String type, String color,
                                      Integer x, Integer positionY, Integer width, Integer length,  BigDecimal price,
                                      Integer capacity, Integer soldTickets) {
        this.sectorId = sectorId;
        this.name = name;
        this.type = type;
        this.color = color;
        this.positionX = x;
        this.positionY = positionY;
        this.width = width;
        this.length = length;
        this.price = price;
        this.capacity = capacity;
        this.soldTickets = soldTickets;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getSoldTickets() {
        return soldTickets;
    }

    public void setSoldTickets(int soldTickets) {
        this.soldTickets = soldTickets;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public void setPositionX(Integer x) {
        this.positionX = x;
    }

    public Integer getPositionY() {
        return positionY;
    }

    public void setPositionY(Integer positionY) {
        this.positionY = positionY;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }
}
