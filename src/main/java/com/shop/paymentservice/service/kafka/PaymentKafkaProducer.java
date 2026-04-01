package com.shop.paymentservice.service.kafka;

import com.shop.paymentservice.exception.KafkaDeliveryException;
import com.shop.paymentservice.model.events.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${spring.kafka.topics.payment-events}")
    private String topicName;

    public void sendPaymentEvent(PaymentEvent paymentEvent) {
        try {
            kafkaTemplate.send(
                    topicName,
                    paymentEvent.getOrderId().toString(),
                    paymentEvent
            ).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Timeout sending payment event for orderId: {}", paymentEvent.getOrderId(), e);
            throw new KafkaDeliveryException("Kafka send timed out for orderId: " + paymentEvent.getOrderId());
        } catch (ExecutionException e) {
            log.error("Failed to send payment event for orderId: {}", paymentEvent.getOrderId(), e);
            throw new KafkaDeliveryException("Kafka send failed for orderId: " + paymentEvent.getOrderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaDeliveryException("Kafka send interrupted for orderId: " + paymentEvent.getOrderId());
        }
    }

}
