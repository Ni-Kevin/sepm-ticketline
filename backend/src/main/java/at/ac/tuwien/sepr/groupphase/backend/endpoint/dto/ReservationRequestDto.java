package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ReservationRequestDto {

    @NotNull
    private Long performanceId;

    @NotEmpty
    private List<Long> holdIds;

    public ReservationRequestDto() {
    }

    public ReservationRequestDto(Long performanceId, List<Long> holdIds) {
        this.performanceId = performanceId;
        this.holdIds = holdIds;
    }

    public Long getPerformanceId() {
        return performanceId;
    }

    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }

    public List<Long> getHoldIds() {
        return holdIds;
    }

    public void setHoldIds(List<Long> holdIds) {
        this.holdIds = holdIds;
    }
}
