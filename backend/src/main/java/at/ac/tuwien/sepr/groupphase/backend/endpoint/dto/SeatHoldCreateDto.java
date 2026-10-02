package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SeatHoldCreateDto {
    @NotNull
    @Size(min = 1)
    private List<HoldItemRequest> items;

    public SeatHoldCreateDto() {}

    public SeatHoldCreateDto(List<HoldItemRequest> items) {
        this.items = items;
    }

    public List<HoldItemRequest> getItems() {
        return items;
    }

    public void setItems(List<HoldItemRequest> items) {
        this.items = items;
    }

    public static class HoldItemRequest {
        @NotNull
        private Long sectorId;
        private Long seatId;
        @NotNull
        private Integer quantity;

        public HoldItemRequest() {}

        public HoldItemRequest(Long sectorId, Long seatId, Integer quantity) {
            this.sectorId = sectorId;
            this.seatId = seatId;
            this.quantity = quantity;
        }

        public Long getSectorId() {
            return sectorId;
        }

        public void setSectorId(Long sectorId) {
            this.sectorId = sectorId;
        }

        public Long getSeatId() {
            return seatId;
        }

        public void setSeatId(Long seatId) {
            this.seatId = seatId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
