package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import at.ac.tuwien.sepr.groupphase.backend.entity.HallAreaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class HallAreaLayoutDto {

    private Long id;

    @NotNull(message = "Area x position is required")
    @Min(value = 0, message = "Area x position must not be negative")
    private Integer positionX;

    @NotNull(message = "Area y position is required")
    @Min(value = 0, message = "Area y position must not be negative")
    private Integer positionY;

    @NotNull(message = "Area width is required")
    @Min(value = 1, message = "Area width must be at least 1")
    private Integer width;

    @NotNull(message = "Area length is required")
    @Min(value = 1, message = "Area length must be at least 1")
    private Integer length;

    @NotNull(message = "Area type is required")
    private HallAreaType type;

    private Long sectorId;
    private String sectorName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public void setPositionX(Integer positionX) {
        this.positionX = positionX;
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

    public HallAreaType getType() {
        return type;
    }

    public void setType(HallAreaType type) {
        this.type = type;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }
}
