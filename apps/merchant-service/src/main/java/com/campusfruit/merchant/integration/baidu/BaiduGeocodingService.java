package com.campusfruit.merchant.integration.baidu;

import com.campusfruit.merchant.integration.baidu.model.BaiduGeoResult;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class BaiduGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(BaiduGeocodingService.class);

    private final RestClient baiduMapRestClient;
    private final BaiduMapConfig config;

    public BaiduGeocodingService(RestClient baiduMapRestClient, BaiduMapConfig config) {
        this.baiduMapRestClient = baiduMapRestClient;
        this.config = config;
    }

    /**
     * 正向地理编码：将文本地址转换为经纬度（BD-09）。
     */
    @CircuitBreaker(name = "baidu-map", fallbackMethod = "geocodeFallback")
    @TimeLimiter(name = "baidu-map")
    public CompletableFuture<Optional<BaiduGeoResult>> geocode(String address) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode response = baiduMapRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/geocoding/v3")
                                .queryParam("address", address)
                                .queryParam("output", "json")
                                .queryParam("ak", config.getServerAk())
                                .queryParam("ret_coordtype", "gcj02ll")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                if (response == null || response.path("status").asInt() != 0) {
                    log.warn("Baidu geocode returned non-zero status: {}",
                            response != null ? response.path("status").asInt() : "null");
                    return Optional.empty();
                }

                JsonNode result = response.path("result");
                JsonNode location = result.path("location");

                BaiduGeoResult geoResult = new BaiduGeoResult();
                geoResult.setStatus(response.path("status").asInt());
                geoResult.setLat(location.path("lat").asDouble());
                geoResult.setLng(location.path("lng").asDouble());
                geoResult.setPrecise(result.path("precise").asInt());
                geoResult.setConfidence(result.path("confidence").asInt());
                geoResult.setLevel(result.path("level").asText());
                geoResult.setFormattedAddress(result.path("formatted_address").asText());

                return Optional.of(geoResult);
            } catch (Exception e) {
                log.warn("Baidu geocode failed for address '{}': {}", address, e.getMessage());
                return Optional.empty();
            }
        });
    }

    /**
     * 反向地理编码：将经纬度转换为文本地址。
     */
    @CircuitBreaker(name = "baidu-map", fallbackMethod = "reverseGeocodeFallback")
    @TimeLimiter(name = "baidu-map")
    public CompletableFuture<Optional<BaiduGeoResult>> reverseGeocode(double lat, double lng) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String location = lat + "," + lng;
                JsonNode response = baiduMapRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/reverse_geocoding/v3")
                                .queryParam("location", location)
                                .queryParam("output", "json")
                                .queryParam("ak", config.getServerAk())
                                .queryParam("coordtype", "bd09ll")
                                .queryParam("ret_coordtype", "bd09ll")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                if (response == null || response.path("status").asInt() != 0) {
                    log.warn("Baidu reverse geocode returned non-zero status: {}",
                            response != null ? response.path("status").asInt() : "null");
                    return Optional.empty();
                }

                JsonNode result = response.path("result");
                JsonNode locationNode = result.path("location");

                BaiduGeoResult geoResult = new BaiduGeoResult();
                geoResult.setStatus(response.path("status").asInt());
                geoResult.setLat(locationNode.path("lat").asDouble());
                geoResult.setLng(locationNode.path("lng").asDouble());
                geoResult.setFormattedAddress(result.path("formatted_address").asText());

                return Optional.of(geoResult);
            } catch (Exception e) {
                log.warn("Baidu reverse geocode failed for ({}, {}): {}", lat, lng, e.getMessage());
                return Optional.empty();
            }
        });
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Optional<BaiduGeoResult>> geocodeFallback(String address, Throwable t) {
        log.warn("Geocode circuit breaker fallback for '{}': {}", address, t.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Optional<BaiduGeoResult>> reverseGeocodeFallback(double lat, double lng, Throwable t) {
        log.warn("Reverse geocode circuit breaker fallback for ({}, {}): {}", lat, lng, t.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
