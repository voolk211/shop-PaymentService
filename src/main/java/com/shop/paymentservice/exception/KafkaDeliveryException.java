package com.shop.paymentservice.exception;

import java.io.Serial;

public class KafkaDeliveryException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -5606361737373854398L;

    public KafkaDeliveryException(String message) {
        super(message);
    }

    public KafkaDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public KafkaDeliveryException() {
    }

    public KafkaDeliveryException(Throwable cause) {
        super(cause);
    }

}
