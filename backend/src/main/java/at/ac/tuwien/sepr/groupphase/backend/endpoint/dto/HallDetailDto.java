package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

public class HallDetailDto {

    private Long id;
    private String name;
    private Integer width;
    private Integer length;
    private Long venueId;
    private String venueName;
    private boolean layoutLocked;
    private boolean dimensionsLocked;
    private String dimensionsLockReason;

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

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public boolean isLayoutLocked() {
        return layoutLocked;
    }

    public void setLayoutLocked(boolean layoutLocked) {
        this.layoutLocked = layoutLocked;
    }

    public boolean isDimensionsLocked() {
        return dimensionsLocked;
    }

    public void setDimensionsLocked(boolean dimensionsLocked) {
        this.dimensionsLocked = dimensionsLocked;
    }

    public String getDimensionsLockReason() {
        return dimensionsLockReason;
    }

    public void setDimensionsLockReason(String dimensionsLockReason) {
        this.dimensionsLockReason = dimensionsLockReason;
    }
}
