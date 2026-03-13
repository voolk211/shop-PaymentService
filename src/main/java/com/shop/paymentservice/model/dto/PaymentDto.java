package com.shop.paymentservice.model.dto;

import com.shop.paymentservice.model.entities.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class PaymentDto {

    private String id;

    @NotNull(message = "Order id must not be null")
    private Long orderId;

    @NotNull(message = "User id must not be null")
    private Long userId;

    private PaymentStatus status;

    @NotNull(message = "Timestamp must not be null")
    private LocalDateTime timestamp;

    @NotNull(message = "Payment amount must not be null")
    @PositiveOrZero(message = "Payment amount must not be negative")
    private BigDecimal paymentAmount;

}
