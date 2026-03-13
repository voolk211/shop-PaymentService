package com.shop.paymentservice.repository;

import com.shop.paymentservice.model.entities.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> getPaymentById(String id);

}
