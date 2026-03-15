package com.shop.paymentservice.service.kafka;

import com.shop.paymentservice.model.events.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${spring.kafka.topics.payment-events}")
    private String topicName;

    public void sendPaymentEvent(PaymentEvent paymentEvent) {
        kafkaTemplate.send(
                topicName,
                paymentEvent.getOrderId().toString(),
                paymentEvent
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send payment event for orderId: {}",
                        paymentEvent.getOrderId(), ex);
            }
            else {
                log.info("Payment event sent for orderId: {}, offset: {}",
                        paymentEvent.getOrderId(),
                        result.getRecordMetadata().offset());
            }
        });
    }

}
