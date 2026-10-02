package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class SectorLayoutDto {

    private Long id;

    @NotBlank(message = "Sector name is required")
    @Size(max = 100, message = "Sector name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Sector type is required")
    private SectorType type;

    @NotBlank(message = "Sector color is required")
    @Size(max = 20, message = "Sector color must not exceed 20 characters")
    private String color;

    private Integer capacity;

    @Valid
    private List<SeatLayoutDto> seats = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SectorType getType() {
        return type;
    }

    public void setType(SectorType type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public List<SeatLayoutDto> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatLayoutDto> seats) {
        this.seats = seats;
    }
}
