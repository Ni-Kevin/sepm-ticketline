package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class HallLayoutDto {

    private Long hallId;
    private String name;
    private Integer width;
    private Integer length;
    private Long venueId;
    private boolean layoutLocked;

    @Valid
    private List<SectorLayoutDto> sectors = new ArrayList<>();

    @Valid
    private List<HallAreaLayoutDto> areas = new ArrayList<>();

    public Long getHallId() {
        return hallId;
    }

    public void setHallId(Long hallId) {
        this.hallId = hallId;
    }

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

    public boolean isLayoutLocked() {
        return layoutLocked;
    }

    public void setLayoutLocked(boolean layoutLocked) {
        this.layoutLocked = layoutLocked;
    }

    public List<SectorLayoutDto> getSectors() {
        return sectors;
    }

    public void setSectors(List<SectorLayoutDto> sectors) {
        this.sectors = sectors;
    }

    public List<HallAreaLayoutDto> getAreas() {
        return areas;
    }

    public void setAreas(List<HallAreaLayoutDto> areas) {
        this.areas = areas;
    }

}
