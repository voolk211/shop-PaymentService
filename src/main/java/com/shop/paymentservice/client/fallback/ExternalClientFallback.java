package com.shop.paymentservice.client.fallback;

import com.shop.paymentservice.client.ExternalClient;
import com.shop.paymentservice.exception.ExternalServiceUnavailableException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExternalClientFallback implements ExternalClient {

    @Override
    public List<Long> getRandomNumber() {
        throw new ExternalServiceUnavailableException("Random number service is unavailable");
    }
}
