package com.shop.paymentservice.model.dto;

import com.shop.paymentservice.model.entities.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class PaymentResponseDto {

    private String id;
    private Long orderId;
    private Long userId;
    private PaymentStatus status;
    private LocalDateTime timestamp;
    private BigDecimal paymentAmount;

}
