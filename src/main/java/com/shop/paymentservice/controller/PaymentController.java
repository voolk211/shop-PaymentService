package com.shop.paymentservice.controller;

import com.shop.paymentservice.model.dto.PaymentCreateDto;
import com.shop.paymentservice.model.dto.PaymentResponseDto;
import com.shop.paymentservice.model.entities.Payment;
import com.shop.paymentservice.model.entities.PaymentStatus;
import com.shop.paymentservice.model.mappers.PaymentMapper;
import com.shop.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    private final PaymentMapper paymentMapper;

    @PreAuthorize("hasRole('ADMIN') or (#paymentDto.userId != null and #paymentDto.userId == authentication.principal)")
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentCreateDto paymentDto) {
        Payment payment = paymentService.createPayment(paymentMapper.toEntity(paymentDto));
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMapper.toResponseDto(payment));
    }

    @PreAuthorize("hasRole('ADMIN') or (#userId != null and #userId == authentication.principal)")
    @GetMapping
    public ResponseEntity<Page<PaymentResponseDto>> getPayments(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            Pageable pageable) {
        Page<Payment> payments = paymentService.getPayments(orderId, userId, paymentStatus, pageable);
        return ResponseEntity.ok(paymentMapper.toResponseDto(payments));

    }

    @PreAuthorize("hasRole('ADMIN') or (#userId != null and #userId == authentication.principal)")
    @GetMapping("/summary/{userId}")
    public ResponseEntity<BigDecimal> getTotalSumOfPaymentsForUser(
            @PathVariable Long userId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(required = false) LocalDateTime from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(required = false) LocalDateTime to
    ) {
        return ResponseEntity.ok(paymentService.getTotalSumOfPaymentsForUser(userId, from, to));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<BigDecimal> getTotalSumOfPaymentsForAllUsers(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(required = false) LocalDateTime from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(required = false) LocalDateTime to
    ) {
        return ResponseEntity.ok(paymentService.getTotalSumOfPaymentsForAllUsers(from, to));
    }
}
