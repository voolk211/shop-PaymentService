package com.shop.paymentservice.service.impl;

import com.shop.paymentservice.client.ExternalClient;
import com.shop.paymentservice.exception.ExternalServiceException;
import com.shop.paymentservice.model.entities.Payment;
import com.shop.paymentservice.model.entities.PaymentStatus;
import com.shop.paymentservice.model.entities.TotalSumOfPayments;
import com.shop.paymentservice.repository.PaymentRepository;
import com.shop.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final MongoTemplate mongoTemplate;

    private final PaymentRepository paymentRepository;

    private final ExternalClient externalClient;

    @Override
    public Payment createPayment(Payment payment) {

        List<Long> numbers = externalClient.getRandomNumber();

        if (numbers == null ||numbers.isEmpty()) {
            throw new ExternalServiceException("Random number service returned empty exception");
        }

        Long number = numbers.getFirst();

        if (number % 2 == 0) {
            payment.setStatus(PaymentStatus.SUCCESS);
        }
        else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        return paymentRepository.save(payment);
    }

    @Override
    public Page<Payment> getPayments(Long orderId, Long userId, PaymentStatus paymentStatus, Pageable pageable) {
        Query query = new Query();

        if (orderId != null) {
            query.addCriteria(Criteria.where("order_id").is(orderId));
        }
        if (userId != null) {
            query.addCriteria(Criteria.where("user_id").is(userId));
        }
        if (paymentStatus != null) {
            query.addCriteria(Criteria.where("status").is(paymentStatus));
        }

        long total = mongoTemplate.count(query, Payment.class);
        query.with(pageable);
        List<Payment> payments = mongoTemplate.find(query, Payment.class);

        return new PageImpl<>(payments, pageable, total);
    }

    @Override
    public BigDecimal getTotalSumOfPaymentsForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        List<Criteria> criteriaList = criteriaListDateFromTo(from, to);
        criteriaList.add(Criteria.where("user_id").is(userId));

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(new Criteria().andOperator(criteriaList)),
                Aggregation.group().sum("payment_amount").as("totalSumOfPayments")
        );
        AggregationResults<TotalSumOfPayments> results = mongoTemplate.aggregate(aggregation, "payments", TotalSumOfPayments.class);
        return Optional.ofNullable(results.getUniqueMappedResult())
                .map(TotalSumOfPayments::getTotalSumOfPayments)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getTotalSumOfPaymentsForAllUsers(LocalDateTime from, LocalDateTime to) {
        List<Criteria> criteriaList = criteriaListDateFromTo(from, to);

        List<AggregationOperation> operations = new ArrayList<>();

        if (!criteriaList.isEmpty()) {
            operations.add(Aggregation.match(new Criteria().andOperator(criteriaList)));
        }
        operations.add(Aggregation.group().sum("payment_amount").as("totalSumOfPayments"));

        Aggregation aggregation = Aggregation.newAggregation(operations);

        AggregationResults<TotalSumOfPayments> results = mongoTemplate.aggregate(aggregation, "payments", TotalSumOfPayments.class);
        return Optional.ofNullable(results.getUniqueMappedResult())
                .map(TotalSumOfPayments::getTotalSumOfPayments)
                .orElse(BigDecimal.ZERO);
    }

    private List<Criteria> criteriaListDateFromTo(LocalDateTime from, LocalDateTime to){
        List<Criteria> criteriaList = new ArrayList<>();
        if (from != null && to != null) {
            criteriaList.add(Criteria.where("timestamp").gte(from).lte(to));
        } else if (from != null) {
            criteriaList.add(Criteria.where("timestamp").gte(from));
        } else if (to != null) {
            criteriaList.add(Criteria.where("timestamp").lte(to));
        }
        return criteriaList;
    }

}
