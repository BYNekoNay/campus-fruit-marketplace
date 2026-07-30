package com.campusfruit.merchant.integration.baidu;

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
public class BaiduCoordinateService {

    private static final Logger log = LoggerFactory.getLogger(BaiduCoordinateService.class);

    private final RestClient baiduMapRestClient;
    private final BaiduMapConfig config;

    public BaiduCoordinateService(RestClient baiduMapRestClient, BaiduMapConfig config) {
        this.baiduMapRestClient = baiduMapRestClient;
        this.config = config;
    }

    /**
     * 将其他坐标系的坐标转换为 BD-09 格式。
     *
     * @param lat          原始纬度
     * @param lng          原始经度
     * @param fromCoordType 源坐标系类型（如 "gcj02ll", "wgs84ll" 等）
     * @return 转换后的 BD-09 坐标，包含 lat/lng
     */
    @CircuitBreaker(name = "baidu-map", fallbackMethod = "convertFallback")
    @TimeLimiter(name = "baidu-map")
    public CompletableFuture<Optional<double[]>> convertToBD09(double lat, double lng, String fromCoordType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String coords = lng + "," + lat;
                JsonNode response = baiduMapRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/geoconv/v1")
                                .queryParam("coords", coords)
                                .queryParam("from", fromCoordType)
                                .queryParam("to", "5") // 5 = BD-09LL
                                .queryParam("output", "json")
                                .queryParam("ak", config.getServerAk())
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                if (response == null || response.path("status").asInt() != 0) {
                    log.warn("Baidu coordinate conversion returned non-zero status: {}",
                            response != null ? response.path("status").asInt() : "null");
                    return Optional.empty();
                }

                JsonNode points = response.path("result");
                if (!points.isArray() || points.isEmpty()) {
                    log.warn("Baidu coordinate conversion returned empty result");
                    return Optional.empty();
                }

                JsonNode point = points.get(0);
                double[] result = new double[]{
                        point.path("y").asDouble(),
                        point.path("x").asDouble()
                };

                return Optional.of(result);
            } catch (Exception e) {
                log.warn("Baidu coordinate conversion failed for ({}, {}, {}): {}",
                        lat, lng, fromCoordType, e.getMessage());
                return Optional.empty();
            }
        });
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Optional<double[]>> convertFallback(double lat, double lng,
                                                                   String fromCoordType, Throwable t) {
        log.warn("Coordinate conversion circuit breaker fallback: {}", t.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
