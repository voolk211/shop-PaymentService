package com.shop.paymentservice.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentCreateDto {

    @NotNull(message = "Order id must not be null")
    private Long orderId;

    @NotNull(message = "User id must not be null")
    private Long userId;

    @NotNull(message = "Timestamp must not be null")
    private LocalDateTime timestamp;

    @NotNull(message = "Payment amount must not be null")
    @PositiveOrZero(message = "Payment amount must not be negative")
    private BigDecimal paymentAmount;

}
