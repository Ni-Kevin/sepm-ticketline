package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class PerformanceSectorPriceDto {

    @NotNull(message = "Sector id is required")
    private Long sectorId;

    @NotBlank(message = "Sector name is required")
    @Size(max = 100, message = "Sector name must not exceed 100 characters")
    private String sectorName;

    @NotBlank(message = "Sector type is required")
    @Size(max = 20, message = "Sector type must not exceed 20 characters")
    private String sectorType;

    @NotNull(message = "Sector price is required")
    @DecimalMin(value = "0.0", message = "Sector price must be greater than or equal to 0")
    private BigDecimal price;

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public String getSectorType() {
        return sectorType;
    }

    public void setSectorType(String sectorType) {
        this.sectorType = sectorType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
