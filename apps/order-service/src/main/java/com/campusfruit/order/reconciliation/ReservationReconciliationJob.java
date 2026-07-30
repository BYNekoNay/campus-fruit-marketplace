package com.campusfruit.order.reconciliation;

import com.campusfruit.order.dto.ReconciliationResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 库存预占孤儿扫描与双向对账定时任务。
 * <p>
 * 每5分钟扫描 PENDING_RESERVATION 超过5分钟未推进的订单，
 * 查询 Offer Service 确认预占状态，修复订单状态并对账。
 */
@Component
@ConditionalOnProperty(value = "app.order.reconciliation.enabled", havingValue = "true", matchIfMissing = false)
public class ReservationReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationReconciliationJob.class);

    private final OrderRepository orderRepository;
    private final OrderStatusEventRepository statusEventRepository;
    private final OfferServiceClient offerServiceClient;
    private final OrderStateMachine stateMachine;

    @Value("${app.order.reconciliation.pending-reservation-max-minutes:5}")
    private int pendingReservationMaxMinutes;

    public ReservationReconciliationJob(OrderRepository orderRepository,
                                         OrderStatusEventRepository statusEventRepository,
                                         OfferServiceClient offerServiceClient,
                                         OrderStateMachine stateMachine) {
        this.orderRepository = orderRepository;
        this.statusEventRepository = statusEventRepository;
        this.offerServiceClient = offerServiceClient;
        this.stateMachine = stateMachine;
    }

    /**
     * 每5分钟执行一次孤儿订单扫描对账。
     */
    @Scheduled(fixedDelay = 300000)
    public void reconcileOrphanReservations() {
        log.info("开始孤儿订单扫描对账...");

        Instant cutoffTime = Instant.now().minusSeconds(pendingReservationMaxMinutes * 60L);
        List<Order> staleOrders = orderRepository.findStalePendingReservationOrders(cutoffTime);

        if (staleOrders.isEmpty()) {
            log.debug("无孤儿订单需要处理");
            return;
        }

        log.info("发现 {} 笔孤儿订单（PENDING_RESERVATION 超过 {} 分钟）", staleOrders.size(), pendingReservationMaxMinutes);

        List<ReconciliationResult> results = new ArrayList<>();
        for (Order order : staleOrders) {
            try {
                ReconciliationResult result = reconcileSingleOrder(order);
                results.add(result);
            } catch (Exception e) {
                log.error("对账处理失败: orderId={}, orderNo={}, error={}",
                        order.getId(), order.getOrderNo(), e.getMessage(), e);

                ReconciliationResult failResult = new ReconciliationResult();
                failResult.setOrderId(order.getId());
                failResult.setActionTaken("ERROR");
                failResult.setReason("对账处理异常: " + e.getMessage());
                failResult.setReconciledAt(Instant.now());
                results.add(failResult);
            }
        }

        log.info("孤儿订单对账完成，共处理 {} 笔", results.size());
    }

    /**
     * 对单个孤儿订单进行对账修复。
     */
    @Transactional
    public ReconciliationResult reconcileSingleOrder(Order order) {
        ReconciliationResult result = new ReconciliationResult();
        result.setOrderId(order.getId());
        result.setPreviousStatus(order.getStatus().name());
        result.setReconciledAt(Instant.now());

        String reservationId = order.getReservationId();

        if (reservationId == null) {
            // 无 reservationId，可能创建订单后预占未发起
            log.warn("孤儿订单无 reservationId: orderNo={}，标记为 REJECTED", order.getOrderNo());

            stateMachine.transition(order, OrderStatus.REJECTED,
                    OperatorType.SYSTEM, null, "对账：孤儿订单无预占ID");
            order.setCancelReason("预占未完成，对账修复");
            orderRepository.save(order);

            result.setActionTaken("MARKED_REJECTED");
            result.setReason("孤儿订单无 reservationId");
            result.setNewStatus(OrderStatus.REJECTED.name());
            return result;
        }

        // 查询 Offer Service 预占状态
        Map<String, Object> reserveStatus = offerServiceClient.getReservationStatus(reservationId);
        String offerStatus = reserveStatus != null ? (String) reserveStatus.getOrDefault("status", "UNKNOWN") : "UNKNOWN";

        switch (offerStatus) {
            case "RESERVED":
            case "ACTIVE":
                // Offer 已预占 → 订单推进到 PENDING_STORE_CONFIRMATION
                log.info("对账修复：offer 已预占 → 推进订单状态: orderNo={}", order.getOrderNo());

                stateMachine.transition(order, OrderStatus.PENDING_STORE_CONFIRMATION,
                        OperatorType.SYSTEM, null, "对账修复：Offer Service 确认已预占");
                result.setActionTaken("ADVANCED_TO_CONFIRMATION");
                result.setReason("对账修复：Offer 已预占");
                result.setNewStatus(OrderStatus.PENDING_STORE_CONFIRMATION.name());
                break;

            case "RELEASED":
            case "EXPIRED":
            case "FAILED":
                // Offer 未预占/已释放 → 订单 REJECTED
                log.info("对账修复：offer 未预占/已释放 → REJECTED: orderNo={}", order.getOrderNo());

                stateMachine.transition(order, OrderStatus.REJECTED,
                        OperatorType.SYSTEM, null, "对账修复：Offer Service 预占状态=" + offerStatus);
                order.setCancelReason("预占已失效（" + offerStatus + "），对账修复");
                result.setActionTaken("MARKED_REJECTED");
                result.setReason("对账修复：Offer 预占状态=" + offerStatus);
                result.setNewStatus(OrderStatus.REJECTED.name());
                break;

            default:
                log.warn("对账：Offer Service 返回未知状态={}，跳过: orderNo={}", offerStatus, order.getOrderNo());
                result.setActionTaken("SKIPPED");
                result.setReason("Offer Service 返回未知状态: " + offerStatus);
                result.setNewStatus(order.getStatus().name());
                return result;
        }

        orderRepository.save(order);

        // 写入对账日志
        OrderStatusEvent reconcileEvent = OrderStatusEvent.of(
                order.getId(),
                result.getPreviousStatus(),
                result.getNewStatus(),
                OperatorType.SYSTEM.name(),
                null,
                "对账修复: " + result.getReason()
        );
        statusEventRepository.save(reconcileEvent);

        return result;
    }
}
