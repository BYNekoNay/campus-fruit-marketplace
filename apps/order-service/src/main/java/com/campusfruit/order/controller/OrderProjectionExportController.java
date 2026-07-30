package com.campusfruit.order.controller;

import com.campusfruit.order.dto.OrderResponse;
import com.campusfruit.order.service.OrderQueryService;
import com.campusfruit.order.repository.OrderItemRepository;
import com.campusfruit.order.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 内部投影导出接口（供其他微服务调用）。
 */
@RestController
@RequestMapping("/api/internal/order")
public class OrderProjectionExportController {

    private final OrderQueryService orderQueryService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderProjectionExportController(OrderQueryService orderQueryService,
                                            OrderRepository orderRepository,
                                            OrderItemRepository orderItemRepository) {
        this.orderQueryService = orderQueryService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
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
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Object[]> results = orderRepository.countCompletedOrdersByStoreSince(since);
        Map<Long, Long> stats = new HashMap<>();
        for (Object[] row : results) {
            Long storeId = (Long) row[0];
            Long count = (Long) row[1];
            stats.put(storeId, count);
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * 近30天指定门店 offer 销量统计（按 offer_id 分组）。
     */
    @GetMapping("/sales-stats/store/{storeId}")
    public ResponseEntity<Map<Long, Long>> getSalesStatsByStoreOffers(@PathVariable Long storeId) {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Object[]> results = orderItemRepository.countSalesByOfferIdAndStoreSince(storeId, since);
        Map<Long, Long> stats = new HashMap<>();
        for (Object[] row : results) {
            Long offerId = ((Number) row[0]).longValue();
            Long quantity = ((Number) row[1]).longValue();
            stats.put(offerId, quantity);
        }
        return ResponseEntity.ok(stats);
    }
}
