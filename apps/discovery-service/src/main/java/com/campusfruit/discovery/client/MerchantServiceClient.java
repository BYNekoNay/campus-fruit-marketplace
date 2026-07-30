package com.campusfruit.discovery.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class MerchantServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MerchantServiceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public MerchantServiceClient(@Value("${discovery.merchant-service.url:http://merchant-service}") String baseUrl,
                                  @Value("${discovery.internal-api-key:}") String internalApiKey,
                                  ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    /**
     * 从 merchant-service 获取全量投影数据。
     *
     * @return 投影数据 JSON 节点列表
     */
    public List<JsonNode> fetchProjections() {
        log.info("Fetching merchant projections from merchant-service");
        try {
            String response = restClient.get()
                    .uri("/api/internal/merchant/projection/export")
                    .header("X-Internal-API-Key", internalApiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                log.warn("Empty response from merchant-service projection export");
                return List.of();
            }

            return objectMapper.readValue(response, new TypeReference<List<JsonNode>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch merchant projections", e);
            throw new RuntimeException("Failed to fetch merchant projections", e);
        }
    }

    /**
     * 获取单个门店的投影信息（用于 StoreActivated 事件填充）。
     */
    public JsonNode fetchStoreProjection(Long storeId) {
        log.info("Fetching store projection for storeId={}", storeId);
        try {
            String response = restClient.get()
                    .uri("/api/internal/merchant/projection/export/{storeId}", storeId)
                    .header("X-Internal-API-Key", internalApiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                log.warn("Empty response for storeId={}", storeId);
                return null;
            }

            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Failed to fetch store projection for storeId={}", storeId, e);
            return null;
        }
    }
}
