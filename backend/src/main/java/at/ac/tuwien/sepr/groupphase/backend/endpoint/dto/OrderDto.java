package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import at.ac.tuwien.sepr.groupphase.backend.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDto {
    private Long id;
    private LocalDateTime purchaseDate;
    private BigDecimal totalPrice;
    private PaymentMethod paymentMethod;
    private String status;

    public OrderDto() {}

    public OrderDto(Long id, LocalDateTime purchaseDate, BigDecimal totalPrice, PaymentMethod paymentMethod, String status) {
        this.id = id;
        this.purchaseDate = purchaseDate;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}