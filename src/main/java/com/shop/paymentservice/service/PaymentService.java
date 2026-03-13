package com.shop.paymentservice.service;

import com.shop.paymentservice.model.dto.PaymentDto;
import com.shop.paymentservice.model.entities.Payment;
import com.shop.paymentservice.model.entities.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentService {

    Payment createPayment(Payment payment);

    Page<Payment> getPayments(Long orderId, Long userId, PaymentStatus paymentStatus, Pageable pageable);

    BigDecimal getTotalSumOfPaymentsForUser(Long userId, LocalDateTime from, LocalDateTime to);

    BigDecimal getTotalSumOfPaymentsForAllUsers(LocalDateTime from, LocalDateTime to);

}
