package com.campusfruit.order.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Offer Service 内部 API 客户端。
 * 用于库存预占和释放。
 */
@Service
public class OfferServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OfferServiceClient.class);

    private final RestClient restClient;

    @Value("${app.internal.api-key:internal-api-key-change-me}")
    private String internalApiKey;

    public OfferServiceClient(@Qualifier("offerServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 预占库存。
     *
     * @param offerId        报价ID
     * @param quantity       预占数量
     * @param reservationId  预占ID（订单级别）
     * @return 预占结果
     */
    public Map<String, Object> reserveStock(Long offerId, Integer quantity, String reservationId) {
        log.info("调用 Offer Service 预占库存: offerId={}, quantity={}, reservationId={}",
                offerId, quantity, reservationId);

        Map<String, Object> requestBody = Map.of(
                "offerId", offerId,
                "quantity", quantity,
                "reservationId", reservationId
        );

        Map<String, Object> response = restClient.post()
                .uri("/api/internal/offers/reserve")
                .header("X-Internal-API-Key", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        log.info("Offer Service 预占响应: {}", response);
        return response;
    }

    /**
     * 释放已预占库存（幂等）。
     *
     * @param offerId       报价ID
     * @param reservationId 预占ID
     */
    public void releaseStock(Long offerId, String reservationId) {
        log.info("调用 Offer Service 释放库存: offerId={}, reservationId={}", offerId, reservationId);

        try {
            Map<String, Object> requestBody = Map.of(
                    "offerId", offerId,
                    "reservationId", reservationId
            );

            restClient.post()
                    .uri("/api/internal/offers/release")
                    .header("X-Internal-API-Key", internalApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("库存释放成功: offerId={}, reservationId={}", offerId, reservationId);
        } catch (Exception e) {
            log.warn("库存释放调用异常（可能已释放，幂等处理）: offerId={}, reservationId={}, error={}",
                    offerId, reservationId, e.getMessage());
        }
    }

    /**
     * 查询预占状态（用于对账）。
     *
     * @param reservationId 预占ID
     * @return 预占状态信息
     */
    public Map<String, Object> getReservationStatus(String reservationId) {
        log.debug("查询预占状态: reservationId={}", reservationId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/internal/offers/reserve/{reservationId}")
                            .build(reservationId))
                    .header("X-Internal-API-Key", internalApiKey)
                    .retrieve()
                    .body(Map.class);

            return response;
        } catch (Exception e) {
            log.warn("查询预占状态失败: reservationId={}, error={}", reservationId, e.getMessage());
            return Map.of("status", "UNKNOWN", "error", e.getMessage());
        }
    }

    /**
     * 获取报价详情（用于下单前价格校验）。
     *
     * @param offerId 报价ID
     * @return 报价详情（含 unitPrice, offerVersion, availableQuantity, storeStatus）
     */
    public Map<String, Object> getOfferDetail(Long offerId) {
        log.debug("查询报价详情: offerId={}", offerId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/internal/offers/{offerId}", offerId)
                    .header("X-Internal-API-Key", internalApiKey)
                    .retrieve()
                    .body(Map.class);

            return response;
        } catch (Exception e) {
            log.warn("查询报价详情失败: offerId={}, error={}", offerId, e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
