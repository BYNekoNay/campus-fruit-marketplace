package com.campusfruit.order.controller;

import com.campusfruit.order.dto.CreateOrderRequest;
import com.campusfruit.order.dto.OrderResponse;
import com.campusfruit.order.dto.QuoteValidationResult;
import com.campusfruit.order.service.OrderQueryService;
import com.campusfruit.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final ObjectMapper objectMapper;

    public OrderController(OrderService orderService, OrderQueryService orderQueryService,
                            ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.orderQueryService = orderQueryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 下单。
     * 幂等键由前端生成（UUID）。
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            OrderResponse response = orderService.createOrder(userId, request, requestBody);
            return ResponseEntity.ok(response);
        } catch (OrderService.StaleQuoteException e) {
            QuoteValidationResult validationResult = e.getValidationResult();
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "STALE_QUOTE");
            errorBody.put("message", e.getMessage());
            errorBody.put("priceChanged", validationResult.isPriceChanged());
            errorBody.put("stockChanged", validationResult.isStockChanged());
            errorBody.put("storeStatusChanged", validationResult.isStoreStatusChanged());

            List<Map<String, String>> changes = new ArrayList<>();
            for (QuoteValidationResult.ChangeDetail detail : validationResult.getChanges()) {
                Map<String, String> change = new HashMap<>();
                change.put("field", detail.getField());
                change.put("description", detail.getDescription());
                changes.add(change);
            }
            errorBody.put("changes", changes);

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody);
        } catch (Exception e) {
            throw new RuntimeException("下单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 我的订单列表（分页）。
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = Long.valueOf(jwt.getSubject());
        Page<OrderResponse> orders = orderQueryService.getMyOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * 订单详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long orderId) {
        Long userId = Long.valueOf(jwt.getSubject());
        OrderResponse response = orderQueryService.getOrder(userId, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 用户取消订单（仅 PENDING_STORE_CONFIRMATION 状态可取消）。
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long orderId,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = Long.valueOf(jwt.getSubject());
        String reason = body != null ? body.getOrDefault("reason", "用户取消") : "用户取消";
        orderService.cancelOrder(userId, orderId, reason);
        return ResponseEntity.ok(Map.of("message", "订单已取消"));
    }

    /**
     * 获取自取码（仅 READY_FOR_PICKUP）。
     */
    @GetMapping("/{id}/pickup-code")
    public ResponseEntity<Map<String, String>> getPickupCode(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long orderId) {
        Long userId = Long.valueOf(jwt.getSubject());
        String code = orderService.getPickupCode(userId, orderId);
        return ResponseEntity.ok(Map.of("code", code));
    }
}
