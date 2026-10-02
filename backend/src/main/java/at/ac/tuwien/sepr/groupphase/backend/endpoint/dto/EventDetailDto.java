package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response body containing detailed event information.
 */
public class EventDetailDto {

    private Long id;
    private String title;
    private String genre;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private byte[] image;
    private List<Long> performanceIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public List<Long> getPerformanceIds() {
        return performanceIds;
    }

    public void setPerformanceIds(List<Long> performanceIds) {
        this.performanceIds = performanceIds;
    }
}
