package com.shop.paymentservice.client;


import com.shop.paymentservice.client.fallback.ExternalClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "random-number",
        url = "${external.random-number-api.url}",
        fallback = ExternalClientFallback.class
)
public interface ExternalClient {

    @GetMapping("/random")
    List<Long> getRandomNumber();

}
