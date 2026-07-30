package com.campusfruit.discovery.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue discoveryMerchantQueue() {
        return new Queue("discovery.merchant", true);
    }

    @Bean
    public Queue discoveryOfferQueue() {
        return new Queue("discovery.offer", true);
    }
}
