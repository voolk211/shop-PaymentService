package com.shop.paymentservice.exception;

import java.io.Serial;

public class ExternalServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8213983657180288139L;

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalServiceException() {
    }

    public ExternalServiceException(Throwable cause) {
        super(cause);
    }

}
