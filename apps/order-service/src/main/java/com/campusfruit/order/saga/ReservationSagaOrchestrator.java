package com.campusfruit.order.saga;

import com.campusfruit.order.entity.Order;
import com.campusfruit.order.entity.OrderItem;
import com.campusfruit.order.entity.OrderStatusEvent;
import com.campusfruit.order.enums.OperatorType;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.repository.OrderItemRepository;
import com.campusfruit.order.repository.OrderRepository;
import com.campusfruit.order.repository.OrderStatusEventRepository;
import com.campusfruit.order.service.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 库存预占 Saga 编排器。
 * <p>
 * 逐项调用 Offer Service 预占库存，全部成功则推进订单状态，
 * 任一失败则补偿释放已预占项并标记订单为 REJECTED。
 */
@Service
public class ReservationSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ReservationSagaOrchestrator.class);

    private final OfferServiceClient offerServiceClient;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusEventRepository statusEventRepository;
    private final OrderStateMachine stateMachine;

    @Value("${app.order.saga.reservation-timeout:10s}")
    private Duration reservationTimeout;

    public ReservationSagaOrchestrator(OfferServiceClient offerServiceClient,
                                        OrderRepository orderRepository,
                                        OrderItemRepository orderItemRepository,
                                        OrderStatusEventRepository statusEventRepository,
                                        OrderStateMachine stateMachine) {
        this.offerServiceClient = offerServiceClient;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusEventRepository = statusEventRepository;
        this.stateMachine = stateMachine;
    }

    /**
     * 执行库存预占 Saga。
     *
     * @param order 订单实体
     * @param items 订单商品列表
     */
    @Transactional
    public void execute(Order order, List<OrderItem> items) {
        String reservationId = "RESV-" + UUID.randomUUID().toString().substring(0, 8);
        order.setReservationId(reservationId);
        orderRepository.save(order);

        log.info("开始预占 Saga: orderNo={}, reservationId={}, itemCount={}",
                order.getOrderNo(), reservationId, items.size());

        boolean allReserved = true;
        OrderItem failedItem = null;

        // 逐项预占
        for (OrderItem item : items) {
            try {
                Map<String, Object> result = offerServiceClient.reserveStock(
                        item.getOfferId(), item.getQuantity(), reservationId);

                if (result == null || Boolean.FALSE.equals(result.get("success"))) {
                    log.warn("预占失败: offerId={}, quantity={}, result={}",
                            item.getOfferId(), item.getQuantity(), result);
                    allReserved = false;
                    failedItem = item;
                    break;
                }

                // 记录预占事件
                OrderStatusEvent reserveEvent = OrderStatusEvent.of(
                        order.getId(),
                        null,
                        "RESERVED",
                        OperatorType.SYSTEM.name(),
                        null,
                        "预占成功: offerId=" + item.getOfferId() + ", qty=" + item.getQuantity()
                );
                statusEventRepository.save(reserveEvent);

                log.debug("预占成功: offerId={}, quantity={}", item.getOfferId(), item.getQuantity());
            } catch (Exception e) {
                log.error("预占调用异常: offerId={}, quantity={}, error={}",
                        item.getOfferId(), item.getQuantity(), e.getMessage());
                allReserved = false;
                failedItem = item;
                break;
            }
        }

        if (allReserved) {
            // 全部预占成功 → PENDING_STORE_CONFIRMATION
            stateMachine.transition(order, OrderStatus.PENDING_STORE_CONFIRMATION,
                    OperatorType.SYSTEM, null, "库存预占全部成功，待门店确认");
            orderRepository.save(order);
            log.info("Saga 全部预占成功: orderNo={}, reservationId={}", order.getOrderNo(), reservationId);
        } else {
            // 预占失败 → 补偿释放已预占项 + REJECTED
            compensateReservation(order, items, failedItem);
            stateMachine.transition(order, OrderStatus.REJECTED,
                    OperatorType.SYSTEM, null, "库存预占失败: "
                            + (failedItem != null ? "offerId=" + failedItem.getOfferId() : "未知"));
            order.setCancelReason("库存不足，预占失败");
            orderRepository.save(order);
            log.warn("Saga 预占失败: orderNo={}, reservationId={}", order.getOrderNo(), reservationId);
        }
    }

    /**
     * 取消订单时释放所有预占库存（幂等）。
     *
     * @param order 订单实体
     */
    @Transactional
    public void cancelReservation(Order order) {
        String reservationId = order.getReservationId();
        if (reservationId == null) {
            log.info("订单无预占记录，跳过释放: orderNo={}", order.getOrderNo());
            return;
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        log.info("释放库存: orderNo={}, reservationId={}, itemCount={}",
                order.getOrderNo(), reservationId, items.size());

        for (OrderItem item : items) {
            offerServiceClient.releaseStock(item.getOfferId(), reservationId);
        }

        // 记录释放事件
        OrderStatusEvent releaseEvent = OrderStatusEvent.of(
                order.getId(),
                null,
                "RESERVATION_RELEASED",
                OperatorType.SYSTEM.name(),
                null,
                "取消订单释放库存"
        );
        statusEventRepository.save(releaseEvent);

        log.info("库存释放完成: orderNo={}", order.getOrderNo());
    }

    /**
     * 异步超时补偿：用于主流程超时后的补偿释放。
     *
     * @param order 订单实体
     */
    @Async
    @Transactional
    public void compensateReservationAsync(Order order) {
        log.info("异步补偿释放库存: orderNo={}", order.getOrderNo());

        try {
            cancelReservation(order);

            // 确保订单状态为 REJECTED
            if (order.getStatus() != OrderStatus.REJECTED) {
                stateMachine.transition(order, OrderStatus.REJECTED,
                        OperatorType.SYSTEM, null, "预占超时，异步补偿");
                order.setCancelReason("预占超时");
                orderRepository.save(order);
            }
        } catch (Exception e) {
            log.error("异步补偿失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage(), e);
        }
    }

    /**
     * 执行带超时的 Saga 编排。
     * 使用 CompletableFuture 实现超时控制。
     */
    @Transactional
    public void executeWithTimeout(Order order, List<OrderItem> items) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(() -> execute(order, items));
            future.get(reservationTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("预占 Saga 超时: orderNo={}, timeout={}", order.getOrderNo(), reservationTimeout);

            // 标记 REJECTED + 异步补偿
            stateMachine.transition(order, OrderStatus.REJECTED,
                    OperatorType.SYSTEM, null, "预占超时（" + reservationTimeout.toSeconds() + "s）");
            order.setCancelReason("预占超时");
            orderRepository.save(order);

            // 异步补偿释放已预占库存
            compensateReservationAsync(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("预占 Saga 被中断: orderNo={}", order.getOrderNo());
            compensateReservationAsync(order);
        } catch (ExecutionException e) {
            log.error("预占 Saga 执行异常: orderNo={}, error={}",
                    order.getOrderNo(), e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            compensateReservationAsync(order);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 补偿释放：释放失败项之前已成功预占的库存。
     */
    private void compensateReservation(Order order, List<OrderItem> allItems, OrderItem failedItem) {
        String reservationId = order.getReservationId();
        if (reservationId == null) {
            return;
        }

        log.info("补偿释放库存: orderNo={}, reservationId={}", order.getOrderNo(), reservationId);

        for (OrderItem item : allItems) {
            // 跳过失败项（未预占成功，无需释放）
            if (failedItem != null && item.getOfferId().equals(failedItem.getOfferId())) {
                continue;
            }

            // 幂等释放
            offerServiceClient.releaseStock(item.getOfferId(), reservationId);

            // 记录补偿事件
            OrderStatusEvent compensateEvent = OrderStatusEvent.of(
                    order.getId(),
                    null,
                    "RESERVATION_COMPENSATED",
                    OperatorType.SYSTEM.name(),
                    null,
                    "补偿释放: offerId=" + item.getOfferId()
            );
            statusEventRepository.save(compensateEvent);
        }
    }
}
