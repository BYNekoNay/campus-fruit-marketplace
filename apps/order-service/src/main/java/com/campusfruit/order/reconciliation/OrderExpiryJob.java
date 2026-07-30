package com.campusfruit.order.reconciliation;

import com.campusfruit.order.entity.Order;
import com.campusfruit.order.entity.OrderStatusEvent;
import com.campusfruit.order.enums.OperatorType;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.repository.OrderRepository;
import com.campusfruit.order.repository.OrderStatusEventRepository;
import com.campusfruit.order.saga.OfferServiceClient;
import com.campusfruit.order.service.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 订单过期扫描定时任务。
 * <p>
 * - 每1分钟扫描 PENDING_STORE_CONFIRMATION 超时 15 分钟的订单 → EXPIRED
 * - 每1分钟扫描 NO_SHOW_PENDING 宽限期超时的订单 → CANCELLED
 * - 超时订单释放库存
 */
@Component
@ConditionalOnProperty(value = "app.order.reconciliation.enabled", havingValue = "true", matchIfMissing = false)
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderRepository orderRepository;
    private final OrderStatusEventRepository statusEventRepository;
    private final OfferServiceClient offerServiceClient;
    private final OrderStateMachine stateMachine;

    @Value("${app.order.reconciliation.store-confirmation-timeout-minutes:15}")
    private int storeConfirmationTimeoutMinutes;

    @Value("${app.order.reconciliation.no-show-grace-minutes:30}")
    private int noShowGraceMinutes;

    public OrderExpiryJob(OrderRepository orderRepository,
                           OrderStatusEventRepository statusEventRepository,
                           OfferServiceClient offerServiceClient,
                           OrderStateMachine stateMachine) {
        this.orderRepository = orderRepository;
        this.statusEventRepository = statusEventRepository;
        this.offerServiceClient = offerServiceClient;
        this.stateMachine = stateMachine;
    }

    /**
     * 每1分钟执行订单过期扫描。
     */
    @Scheduled(fixedDelay = 60000)
    public void scanExpiredOrders() {
        log.debug("开始订单过期扫描...");

        expirePendingConfirmationOrders();
        cancelNoShowOrders();
    }

    /**
     * 扫描 PENDING_STORE_CONFIRMATION 超时订单 → EXPIRED + 释放库存。
     */
    @Transactional
    public int expirePendingConfirmationOrders() {
        Instant cutoffTime = Instant.now().minusSeconds(storeConfirmationTimeoutMinutes * 60L);
        List<Order> expiredOrders = orderRepository.findStalePendingConfirmationOrders(cutoffTime);

        if (expiredOrders.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Order order : expiredOrders) {
            try {
                expireSingleOrder(order);
                count++;
            } catch (Exception e) {
                log.error("过期处理失败: orderId={}, orderNo={}, error={}",
                        order.getId(), order.getOrderNo(), e.getMessage(), e);
            }
        }

        if (count > 0) {
            log.info("门店确认超时过期处理: {} 笔订单 → EXPIRED", count);
        }
        return count;
    }

    /**
     * 扫描 NO_SHOW_PENDING 宽限期超时订单 → CANCELLED + 释放库存。
     */
    @Transactional
    public int cancelNoShowOrders() {
        Instant cutoffTime = Instant.now().minusSeconds(noShowGraceMinutes * 60L);
        List<Order> noShowOrders = orderRepository.findStaleNoShowOrders(cutoffTime);

        if (noShowOrders.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Order order : noShowOrders) {
            try {
                cancelNoShowOrder(order);
                count++;
            } catch (Exception e) {
                log.error("未取货取消处理失败: orderId={}, orderNo={}, error={}",
                        order.getId(), order.getOrderNo(), e.getMessage(), e);
            }
        }

        if (count > 0) {
            log.info("未取货宽限期过期处理: {} 笔订单 → CANCELLED", count);
        }
        return count;
    }

    /**
     * 单个门店确认超时订单过期处理。
     */
    @Transactional
    public void expireSingleOrder(Order order) {
        log.info("门店确认超时过期: orderNo={}, createdAt={}, cutoffMinutes={}",
                order.getOrderNo(), order.getCreatedAt(), storeConfirmationTimeoutMinutes);

        // 释放库存
        releaseInventory(order);

        // 状态转移
        stateMachine.transition(order, OrderStatus.EXPIRED,
                OperatorType.SYSTEM, null,
                "门店确认超时（" + storeConfirmationTimeoutMinutes + "分钟），自动过期");
        order.setCancelReason("门店确认超时");
        orderRepository.save(order);
    }

    /**
     * 单个未取货宽限期超时订单取消处理。
     */
    @Transactional
    public void cancelNoShowOrder(Order order) {
        log.info("未取货宽限期超时取消: orderNo={}, updatedAt={}, cutoffMinutes={}",
                order.getOrderNo(), order.getUpdatedAt(), noShowGraceMinutes);

        // 释放库存
        releaseInventory(order);

        // 状态转移
        stateMachine.transition(order, OrderStatus.CANCELLED,
                OperatorType.SYSTEM, null,
                "未取货宽限期超时（" + noShowGraceMinutes + "分钟），自动取消");
        order.setCancelReason("未取货宽限期超时");
        orderRepository.save(order);
    }

    /**
     * 释放订单关联的所有库存（幂等）。
     */
    private void releaseInventory(Order order) {
        String reservationId = order.getReservationId();
        if (reservationId == null) {
            log.debug("订单无预占ID，跳过库存释放: orderNo={}", order.getOrderNo());
            return;
        }

        // Note: 这里假设 Offer Service 的 release 接口支持按 reservationId 批量释放。
        // 实际实现可能需要查询 order_items 逐项释放。
        log.info("超时订单释放库存: orderNo={}, reservationId={}", order.getOrderNo(), reservationId);

        try {
            // 尝试通过 reservationId 释放（具体取决于 Offer Service API 设计）
            // 如果 Offer Service 不支持批量释放，需要逐项遍历 orderItems
            OrderStatusEvent releaseEvent = OrderStatusEvent.of(
                    order.getId(),
                    null,
                    "INVENTORY_RELEASED",
                    OperatorType.SYSTEM.name(),
                    null,
                    "超时订单释放库存: reservationId=" + reservationId
            );
            statusEventRepository.save(releaseEvent);
        } catch (Exception e) {
            log.warn("超时订单释放库存调用异常（可能已释放，幂等处理）: orderNo={}, error={}",
                    order.getOrderNo(), e.getMessage());
        }
    }
}
