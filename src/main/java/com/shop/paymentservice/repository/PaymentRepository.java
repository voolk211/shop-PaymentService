package com.shop.paymentservice.repository;

import com.shop.paymentservice.model.entities.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {

}
