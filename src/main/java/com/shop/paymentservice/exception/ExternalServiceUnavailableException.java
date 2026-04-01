package com.shop.paymentservice.exception;

import java.io.Serial;

public class ExternalServiceUnavailableException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 5473939561279830305L;

    public ExternalServiceUnavailableException() {
    }

    public ExternalServiceUnavailableException(Throwable cause) {
        super(cause);
    }

    public ExternalServiceUnavailableException(String message) {
        super(message);
    }

    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
