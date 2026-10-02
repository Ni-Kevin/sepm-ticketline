package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request body used for creating a new performance.
 */
public class PerformanceCreateDto {

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    @Min(0)
    private Integer durationHours;

    @NotNull
    @Min(0)
    @Max(59)
    private Integer durationMinutes;

    @NotBlank(message = "Performance name is required")
    private String performanceName;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal startPrice;

    @NotNull(message = "Hall id is required")
    private Long hallId;

    @NotEmpty(message = "must not be empty")
    private List<Long> artistIds;

    @Valid
    private List<PerformanceSectorPriceDto> sectorPrices;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getPerformanceName() {
        return performanceName;
    }

    public void setPerformanceName(String performanceName) {
        this.performanceName = performanceName;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public Long getHallId() {
        return hallId;
    }

    public void setHallId(Long hallId) {
        this.hallId = hallId;
    }

    public List<Long> getArtistIds() {
        return artistIds;
    }

    public void setArtistIds(List<Long> artistIds) {
        this.artistIds = artistIds;
    }

    public List<PerformanceSectorPriceDto> getSectorPrices() {
        return sectorPrices;
    }

    public void setSectorPrices(List<PerformanceSectorPriceDto> sectorPrices) {
        this.sectorPrices = sectorPrices;
    }
}
