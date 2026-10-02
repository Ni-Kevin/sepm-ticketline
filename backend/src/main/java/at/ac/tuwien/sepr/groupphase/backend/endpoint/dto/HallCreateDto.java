package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class HallCreateDto {

    @NotBlank(message = "Hall name is required")
    @Size(max = 200, message = "Hall name must not exceed 200 characters")
    private String name;

    @NotNull(message = "Width is required")
    @Min(value = 1, message = "Width must be at least 1")
    private Integer width;

    @NotNull(message = "Length is required")
    @Min(value = 1, message = "Length must be at least 1")
    private Integer length;

    @NotNull(message = "Venue id is required")
    private Long venueId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Long getVenueId() {
        return venueId;
    }

    public void setVenueId(Long venueId) {
        this.venueId = venueId;
    }
}
