package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request body used for creating a new event.
 */
public class EventCreateDto {

    @NotBlank(message = "must not be blank")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "must not be blank")
    @Size(max = 100)
    private String genre;

    @Size(max = 10000)
    private String description;

    @NotNull(message = "must not be null")
    private LocalDateTime startTime;

    @NotNull(message = "must not be null")
    private LocalDateTime endTime;

    private byte[] image;

    @NotEmpty(message = "must not be empty")
    private List<Long> performanceIds;

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

    public List<Long> getPerformanceIds() {
        return performanceIds;
    }

    public void setPerformanceIds(List<Long> performanceIds) {
        this.performanceIds = performanceIds;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
}
