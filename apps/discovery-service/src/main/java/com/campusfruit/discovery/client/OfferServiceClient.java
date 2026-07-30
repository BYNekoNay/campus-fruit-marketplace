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
public class OfferServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OfferServiceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public OfferServiceClient(@Value("${discovery.offer-service.url:http://offer-service}") String baseUrl,
                               @Value("${discovery.internal-api-key:}") String internalApiKey,
                               ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    /**
     * 从 offer-service 获取全量投影数据。
     *
     * @return 投影数据 JSON 节点列表
     */
    public List<JsonNode> fetchProjections() {
        log.info("Fetching offer projections from offer-service");
        try {
            String response = restClient.get()
                    .uri("/api/internal/offer/projection/export")
                    .header("X-Internal-API-Key", internalApiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                log.warn("Empty response from offer-service projection export");
                return List.of();
            }

            return objectMapper.readValue(response, new TypeReference<List<JsonNode>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch offer projections", e);
            throw new RuntimeException("Failed to fetch offer projections", e);
        }
    }
}
