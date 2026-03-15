package com.shop.paymentservice.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    @Value("${spring.kafka.topics.payment-events}")
    private String topicName;

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder
                .name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

}
