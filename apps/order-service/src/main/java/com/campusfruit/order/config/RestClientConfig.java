package com.campusfruit.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP 客户端配置。
 * 用于调用 Offer Service 等内部微服务。
 */
@Configuration
public class RestClientConfig {

    @Value("${app.order.offer-service.url:http://localhost:8083}")
    private String offerServiceUrl;

    @Bean
    public RestClient offerServiceRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(offerServiceUrl)
                .build();
    }
}
