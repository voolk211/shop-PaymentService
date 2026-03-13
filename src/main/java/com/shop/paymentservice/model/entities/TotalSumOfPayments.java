package com.shop.paymentservice.model.entities;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TotalSumOfPayments {
    private BigDecimal totalSumOfPayments;
}
