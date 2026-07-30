package com.campusfruit.order.controller;

import com.campusfruit.order.dto.OrderResponse;
import com.campusfruit.order.service.OrderQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内部投影导出接口（供其他微服务调用）。
 */
@RestController
@RequestMapping("/api/internal/order")
public class OrderProjectionExportController {

    private final OrderQueryService orderQueryService;

    public OrderProjectionExportController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    /**
     * 游标分页投影导出。
     *
     * @param lastId 上一页最后一条记录的 ID，首次传 0
     * @param limit  每页条数（默认 100）
     */
    @GetMapping("/projection/export")
    public ResponseEntity<List<OrderResponse>> exportProjection(
            @RequestParam(defaultValue = "0") Long lastId,
            @RequestParam(defaultValue = "100") int limit) {
        List<OrderResponse> orders = orderQueryService.exportOrders(lastId, limit);
        return ResponseEntity.ok(orders);
    }

    /**
     * 近30天门店销量统计（按 store_id 分组）。
     */
    @GetMapping("/sales-stats/per-store")
    public ResponseEntity<Map<Long, Long>> getSalesStatsPerStore() {
        return ResponseEntity.ok(orderQueryService.getSalesStatsPerStore());
    }

    /**
     * 近30天指定门店 offer 销量统计（按 offer_id 分组）。
     */
    @GetMapping("/sales-stats/store/{storeId}")
    public ResponseEntity<Map<Long, Long>> getSalesStatsByStoreOffers(@PathVariable Long storeId) {
        return ResponseEntity.ok(orderQueryService.getSalesStatsByStoreOffers(storeId));
    }
}
