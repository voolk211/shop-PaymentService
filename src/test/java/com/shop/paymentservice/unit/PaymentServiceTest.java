package com.shop.paymentservice.unit;

import com.shop.paymentservice.client.ExternalClient;
import com.shop.paymentservice.exception.ExternalServiceException;
import com.shop.paymentservice.model.entities.Payment;
import com.shop.paymentservice.model.entities.PaymentStatus;
import com.shop.paymentservice.model.entities.TotalSumOfPayments;
import com.shop.paymentservice.model.events.PaymentEvent;
import com.shop.paymentservice.repository.PaymentRepository;
import com.shop.paymentservice.service.impl.PaymentServiceImpl;
import com.shop.paymentservice.service.kafka.PaymentKafkaProducer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;


@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ExternalClient externalClient;

    @Mock
    private PaymentKafkaProducer kafkaProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createPayment_WhenEvenNumber_ShouldSetStatusSuccess() {

        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setUserId(1L);

        Payment savedPayment = new Payment();
        savedPayment.setId("abc123");
        savedPayment.setOrderId(1L);
        savedPayment.setStatus(PaymentStatus.SUCCESS);

        when(externalClient.getRandomNumber()).thenReturn(List.of(2L));
        when(paymentRepository.save(payment)).thenReturn(savedPayment);

        Payment result = paymentService.createPayment(payment);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(payment);
        verify(kafkaProducer).sendPaymentEvent(
                new PaymentEvent(savedPayment.getOrderId(), PaymentStatus.SUCCESS)
        );
    }

    @Test
    void createPayment_WhenOddNumber_ShouldSetStatusFailed() {
        Payment payment = new Payment();
        payment.setOrderId(1L);

        Payment savedPayment = new Payment();
        savedPayment.setId("abc123");
        savedPayment.setOrderId(1L);
        savedPayment.setStatus(PaymentStatus.FAILED);

        when(externalClient.getRandomNumber()).thenReturn(List.of(3L));
        when(paymentRepository.save(payment)).thenReturn(savedPayment);

        Payment result = paymentService.createPayment(payment);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(kafkaProducer).sendPaymentEvent(
                new PaymentEvent(savedPayment.getOrderId(), PaymentStatus.FAILED)
        );
    }

    @Test
    void createPayment_WhenExternalServiceReturnsEmptyList_ShouldThrowExternalServiceException() {
        Payment payment = new Payment();
        when(externalClient.getRandomNumber()).thenReturn(List.of());

        assertThatThrownBy(() -> paymentService.createPayment(payment))
                .isInstanceOf(ExternalServiceException.class);

        verify(paymentRepository, never()).save(any());
        verify(kafkaProducer, never()).sendPaymentEvent(any());
    }

    @Test
    void createPayment_WhenExternalServiceReturnsNull_ShouldThrowExternalServiceException() {
        Payment payment = new Payment();
        when(externalClient.getRandomNumber()).thenReturn(null);

        assertThatThrownBy(() -> paymentService.createPayment(payment))
                .isInstanceOf(ExternalServiceException.class);

        verify(paymentRepository, never()).save(any());
        verify(kafkaProducer, never()).sendPaymentEvent(any());
    }

    @Test
    void getPayments_WhenAllParamsProvided_ShouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Payment> payments = List.of(new Payment(), new Payment());

        when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(2L);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);

        Page<Payment> result = paymentService.getPayments(1L, 1L, PaymentStatus.SUCCESS, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getPayments_WhenNoParamsProvided_ShouldReturnAllPayments() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Payment> payments = List.of(new Payment(), new Payment(), new Payment());

        when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(3L);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(payments);

        Page<Payment> result = paymentService.getPayments(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void getPayments_WhenNoResults_ShouldReturnEmptyPage() {

        Pageable pageable = PageRequest.of(0, 10);

        when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(List.of());

        Page<Payment> result = paymentService.getPayments(1L, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }


    @Test
    void getTotalSumOfPaymentsForUser_WhenPaymentsExist_ShouldReturnSum() {
        TotalSumOfPayments totalSumOfPayments = new TotalSumOfPayments();
        totalSumOfPayments.setTotalSumOfPayments(new BigDecimal("150.00"));

        AggregationResults<TotalSumOfPayments> aggregationResults =
                mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(totalSumOfPayments);
        when(mongoTemplate.aggregate(
                any(Aggregation.class),
                anyString(),
                eq(TotalSumOfPayments.class))
        ).thenReturn(aggregationResults);

        BigDecimal result = paymentService.getTotalSumOfPaymentsForUser(
                1L,
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59)
        );

        assertThat(result).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void getTotalSumOfPaymentsForUser_WhenNoPayments_ShouldReturnZero() {
        AggregationResults<TotalSumOfPayments> aggregationResults =
                mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(
                any(Aggregation.class),
                anyString(),
                eq(TotalSumOfPayments.class))
        ).thenReturn(aggregationResults);

        BigDecimal result = paymentService.getTotalSumOfPaymentsForUser(
                1L, null, null
        );

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalSumOfPaymentsForUser_WhenOnlyFromProvided_ShouldReturnSum() {
        TotalSumOfPayments totalSumOfPayments = new TotalSumOfPayments();
        totalSumOfPayments.setTotalSumOfPayments(new BigDecimal("75.00"));

        AggregationResults<TotalSumOfPayments> aggregationResults =
                mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(totalSumOfPayments);
        when(mongoTemplate.aggregate(
                any(Aggregation.class),
                anyString(),
                eq(TotalSumOfPayments.class))
        ).thenReturn(aggregationResults);

        BigDecimal result = paymentService.getTotalSumOfPaymentsForUser(
                1L,
                LocalDateTime.of(2024, 1, 1, 0, 0),
                null
        );

        assertThat(result).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void getTotalSumOfPaymentsForAllUsers_WhenPaymentsExist_ShouldReturnSum() {
        TotalSumOfPayments totalSumOfPayments = new TotalSumOfPayments();
        totalSumOfPayments.setTotalSumOfPayments(new BigDecimal("500.00"));

        AggregationResults<TotalSumOfPayments> aggregationResults =
                mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(totalSumOfPayments);
        when(mongoTemplate.aggregate(
                any(Aggregation.class),
                anyString(),
                eq(TotalSumOfPayments.class))
        ).thenReturn(aggregationResults);

        BigDecimal result = paymentService.getTotalSumOfPaymentsForAllUsers(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59)
        );

        assertThat(result).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void getTotalSumOfPaymentsForAllUsers_WhenNoDateRange_ShouldReturnTotalSum() {
        TotalSumOfPayments totalSumOfPayments = new TotalSumOfPayments();
        totalSumOfPayments.setTotalSumOfPayments(new BigDecimal("1000.00"));

        AggregationResults<TotalSumOfPayments> aggregationResults =
                mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(totalSumOfPayments);
        when(mongoTemplate.aggregate(
                any(Aggregation.class),
                anyString(),
                eq(TotalSumOfPayments.class))
        ).thenReturn(aggregationResults);

        BigDecimal result = paymentService.getTotalSumOfPaymentsForAllUsers(null, null);

        assertThat(result).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void getTotalSumOfPaymentsForAllUsers_WhenNoPayments_ShouldReturnZero() {
        AggregationResults<TotalSumOfPayments> aggregationResults =
                mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(
                any(Aggregation.class),
                anyString(),
                eq(TotalSumOfPayments.class))
        ).thenReturn(aggregationResults);

        BigDecimal result = paymentService.getTotalSumOfPaymentsForAllUsers(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59)
        );

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
