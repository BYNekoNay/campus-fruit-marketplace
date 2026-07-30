package com.campusfruit.order;

import com.campusfruit.order.dto.ReconciliationResult;
import com.campusfruit.order.entity.Order;
import com.campusfruit.order.entity.OrderItem;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.reconciliation.OrderExpiryJob;
import com.campusfruit.order.reconciliation.ReservationReconciliationJob;
import com.campusfruit.order.repository.OrderItemRepository;
import com.campusfruit.order.repository.OrderRepository;
import com.campusfruit.order.repository.OrderStatusEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对账集成测试。
 * <p>
 * 验证：
 * 1. 孤儿订单被对账修复
 * 2. 超时订单释放库存
 * 3. 双向对账一致性
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationReconciliationIT {

    @Autowired
    private ReservationReconciliationJob reconciliationJob;

    @Autowired
    private OrderExpiryJob expiryJob;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusEventRepository statusEventRepository;

    private Order orphanOrder;
    private Order expiredOrder;
    private Order noShowOrder;

    @BeforeEach
    void setUp() {
        // 清理
        statusEventRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        // 1. 创建孤儿订单：PENDING_RESERVATION 超过5分钟
        orphanOrder = createOrder("ORD-ORPHAN-001", OrderStatus.PENDING_RESERVATION, 10);
        orphanOrder.setCreatedAt(Instant.now().minus(10, ChronoUnit.MINUTES));

        // 2. 创建门店确认超时订单：PENDING_STORE_CONFIRMATION 超过15分钟
        expiredOrder = createOrder("ORD-EXPIRED-001", OrderStatus.PENDING_STORE_CONFIRMATION, 20);
        expiredOrder.setCreatedAt(Instant.now().minus(20, ChronoUnit.MINUTES));

        // 3. 创建未取货超时订单：NO_SHOW_PENDING 超过30分钟
        noShowOrder = createOrder("ORD-NOSHOW-001", OrderStatus.NO_SHOW_PENDING, 40);
        noShowOrder.setUpdatedAt(Instant.now().minus(40, ChronoUnit.MINUTES));

        orderRepository.flush();
    }

    @AfterEach
    void tearDown() {
        statusEventRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void shouldReconcileOrphanReservationOrder() {
        // 对账修复孤儿订单
        ReconciliationResult result = reconciliationJob.reconcileSingleOrder(orphanOrder);

        assertNotNull(result);
        assertEquals(orphanOrder.getId(), result.getOrderId());
        assertNotNull(result.getActionTaken());
        assertNotNull(result.getReason());
        assertNotNull(result.getReconciledAt());

        // 重新加载订单，验证状态已变更
        Order updated = orderRepository.findById(orphanOrder.getId()).orElseThrow();
        assertNotEquals(OrderStatus.PENDING_RESERVATION, updated.getStatus(),
                "孤儿订单应对账修复，不再保持 PENDING_RESERVATION 状态");

        // 验证对账事件已写入
        assertFalse(statusEventRepository.findByOrderIdOrderByCreatedAt(updated.getId()).isEmpty(),
                "应写入对账状态事件");
    }

    @Test
    void shouldExpireOverdueConfirmationOrder() {
        // 执行过期扫描
        int expiredCount = expiryJob.expirePendingConfirmationOrders();

        assertTrue(expiredCount > 0, "应有超时门店确认订单被过期");

        // 验证订单已过期
        Order updated = orderRepository.findById(expiredOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.EXPIRED, updated.getStatus());
        assertNotNull(updated.getCancelReason());
        assertTrue(updated.getCancelReason().contains("超时"));

        // 验证事件已写入
        assertFalse(statusEventRepository.findByOrderIdOrderByCreatedAt(updated.getId()).isEmpty());
    }

    @Test
    void shouldCancelOverdueNoShowOrder() {
        // 执行未取货取消扫描
        int cancelledCount = expiryJob.cancelNoShowOrders();

        assertTrue(cancelledCount > 0, "应有超时未取货订单被取消");

        // 验证订单已取消
        Order updated = orderRepository.findById(noShowOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updated.getStatus());
        assertNotNull(updated.getCancelReason());
        assertTrue(updated.getCancelReason().contains("宽限期") || updated.getCancelReason().contains("超时"));

        // 验证事件已写入
        assertFalse(statusEventRepository.findByOrderIdOrderByCreatedAt(updated.getId()).isEmpty());
    }

    @Test
    void shouldNotAffectFreshOrders() {
        // 创建正常时效内的订单
        Order freshOrder = createOrder("ORD-FRESH-001", OrderStatus.PENDING_RESERVATION, 1);
        freshOrder.setCreatedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        orderRepository.flush();

        // 执行扫描
        reconciliationJob.reconcileOrphanReservations();

        // 验证正常订单未被影响
        Order updated = orderRepository.findById(freshOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.PENDING_RESERVATION, updated.getStatus(),
                "时效内订单不应被对账修改");
    }

    // --- 辅助方法 ---

    private Order createOrder(String orderNo, OrderStatus status, int ageMinutes) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(999L);
        order.setStoreId(1L);
        order.setStatus(status);
        order.setTotalAmount(5000L);
        order.setItemCount(1);
        order.setReservationId("RESV-TEST-" + System.currentTimeMillis());
        return orderRepository.save(order);
    }
}
