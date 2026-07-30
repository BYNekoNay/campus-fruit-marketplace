package com.campusfruit.order.controller;

import com.campusfruit.order.dto.OrderResponse;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.pickup.PickupCodeService;
import com.campusfruit.order.service.OrderQueryService;
import com.campusfruit.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 门店/员工订单操作接口。
 */
@RestController
@RequestMapping("/api/store/orders")
public class StoreOrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final PickupCodeService pickupCodeService;

    public StoreOrderController(OrderService orderService, OrderQueryService orderQueryService,
                                 PickupCodeService pickupCodeService) {
        this.orderService = orderService;
        this.orderQueryService = orderQueryService;
        this.pickupCodeService = pickupCodeService;
    }

    /**
     * 门店订单列表（可选状态过滤）。
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getStoreOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "status", required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // 从 JWT 获取门店关联信息（简化：直接使用参数）
        Long effectiveStoreId = storeId != null ? storeId : 1L;
        Page<OrderResponse> orders = orderQueryService.getStoreOrders(effectiveStoreId, status, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * 接单（PENDING_STORE_CONFIRMATION → ACCEPTED）。
     */
    @PutMapping("/{id}/accept")
    public ResponseEntity<Map<String, String>> acceptOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long orderId,
            @RequestParam(value = "storeId", defaultValue = "1") Long storeId) {
        Long userId = Long.valueOf(jwt.getSubject());
        orderService.acceptOrder(storeId, orderId, userId);
        return ResponseEntity.ok(Map.of("message", "已接单"));
    }

    /**
     * 拒单（→ REJECTED）。
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long orderId,
            @RequestParam(value = "storeId", defaultValue = "1") Long storeId,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = Long.valueOf(jwt.getSubject());
        String reason = body != null ? body.getOrDefault("reason", "门店拒单") : "门店拒单";
        orderService.rejectOrder(storeId, orderId, userId, reason);
        return ResponseEntity.ok(Map.of("message", "已拒单"));
    }

    /**
     * 备货完成（ACCEPTED → READY_FOR_PICKUP）。
     */
    @PutMapping("/{id}/ready")
    public ResponseEntity<Map<String, String>> readyOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long orderId,
            @RequestParam(value = "storeId", defaultValue = "1") Long storeId) {
        Long userId = Long.valueOf(jwt.getSubject());
        orderService.readyOrder(storeId, orderId, userId);
        return ResponseEntity.ok(Map.of("message", "备货完成，已生成自取码"));
    }

    /**
     * 核销完成（READY_FOR_PICKUP → COMPLETED，payment_status → PAID_AT_PICKUP）。
     * 前端需传入用户出示的自取码进行核验。
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<Map<String, String>> completeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long orderId,
            @RequestParam(value = "storeId", defaultValue = "1") Long storeId,
            @RequestParam(value = "pickupCode") String pickupCode) {
        Long userId = Long.valueOf(jwt.getSubject());

        // 核验自取码
        boolean verified = pickupCodeService.verifyPickupCode(orderId, pickupCode);
        if (!verified) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "自取码无效或已过期"));
        }

        orderService.completeOrder(storeId, orderId, userId);
        return ResponseEntity.ok(Map.of("message", "核销完成"));
    }

    /**
     * 标记未取货（READY_FOR_PICKUP → NO_SHOW_PENDING）。
     */
    @PutMapping("/{id}/no-show")
    public ResponseEntity<Map<String, String>> noShow(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long orderId,
            @RequestParam(value = "storeId", defaultValue = "1") Long storeId) {
        Long userId = Long.valueOf(jwt.getSubject());
        orderService.markNoShow(storeId, orderId, userId);
        return ResponseEntity.ok(Map.of("message", "已标记未取货"));
    }
}
