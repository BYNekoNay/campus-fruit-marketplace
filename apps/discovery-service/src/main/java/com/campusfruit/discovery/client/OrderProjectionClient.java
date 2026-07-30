package com.campusfruit.discovery.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.Map;

/**
 * Order Service 内部 API 客户端，用于获取销售统计数据。
 */
@Service
public class OrderProjectionClient {

    private static final Logger log = LoggerFactory.getLogger(OrderProjectionClient.class);

    private final RestClient restClient;
    private final String internalApiKey;

    public OrderProjectionClient(@Value("${discovery.order-service.url:http://order-service}") String baseUrl,
                                  @Value("${discovery.internal-api-key:}") String internalApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.internalApiKey = internalApiKey;
    }

    /**
     * 获取近30天各门店 COMPLETED 订单数量。
     *
     * @return storeId -> salesCount 映射
     */
    public Map<Long, Long> getSalesStatsPerStore() {
        try {
            Map<Long, Long> response = restClient.get()
                    .uri("/api/internal/order/sales-stats/per-store")
                    .header("X-Internal-API-Key", internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<Long, Long>>() {});
            return response != null ? response : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to fetch sales stats per store: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 获取近30天指定门店按 offer 分组的销量统计。
     *
     * @param storeId 门店ID
     * @return offerId -> salesQuantity 映射
     */
    public Map<Long, Long> getSalesStatsByStoreOffers(Long storeId) {
        try {
            Map<Long, Long> response = restClient.get()
                    .uri("/api/internal/order/sales-stats/store/{storeId}", storeId)
                    .header("X-Internal-API-Key", internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<Long, Long>>() {});
            return response != null ? response : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to fetch sales stats for store {}: {}", storeId, e.getMessage());
            return Collections.emptyMap();
        }
    }
}
