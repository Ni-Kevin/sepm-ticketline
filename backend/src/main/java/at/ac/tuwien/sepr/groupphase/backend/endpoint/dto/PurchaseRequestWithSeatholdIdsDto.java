package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import at.ac.tuwien.sepr.groupphase.backend.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class PurchaseRequestWithSeatholdIdsDto {
    @NotNull
    private Long performanceId;
    @NotNull
    @Size(min = 1)
    private List<Long> holdIds;
    @NotNull
    private PaymentMethod paymentMethod;
    private Map<String, String> paymentDetails;

    public PurchaseRequestWithSeatholdIdsDto() {}

    public PurchaseRequestWithSeatholdIdsDto(Long performanceId, List<Long> holdIds, PaymentMethod paymentMethod, Map<String, String> paymentDetails) {
        this.performanceId = performanceId;
        this.holdIds = holdIds;
        this.paymentMethod = paymentMethod;
        this.paymentDetails = paymentDetails;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Map<String, String> getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(Map<String, String> paymentDetails) {
        this.paymentDetails = paymentDetails;
    }
}
