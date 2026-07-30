package com.campusfruit.order.service;

import com.campusfruit.order.entity.Order;
import com.campusfruit.order.entity.OrderStatusEvent;
import com.campusfruit.order.enums.OperatorType;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.repository.OrderStatusEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态机。
 * <p>
 * 校验状态转移合法性并记录状态变更事件。
 * <pre>
 * PENDING_RESERVATION → PENDING_STORE_CONFIRMATION / REJECTED
 * PENDING_STORE_CONFIRMATION → ACCEPTED / REJECTED / CANCELLED / EXPIRED
 * ACCEPTED → READY_FOR_PICKUP
 * READY_FOR_PICKUP → COMPLETED / NO_SHOW_PENDING
 * NO_SHOW_PENDING → COMPLETED / CANCELLED
 * </pre>
 */
@Component
public class OrderStateMachine {

    private static final Logger log = LoggerFactory.getLogger(OrderStateMachine.class);

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING_RESERVATION, Set.of(OrderStatus.PENDING_STORE_CONFIRMATION, OrderStatus.REJECTED),
            OrderStatus.PENDING_STORE_CONFIRMATION, Set.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.CANCELLED, OrderStatus.EXPIRED),
            OrderStatus.ACCEPTED, Set.of(OrderStatus.READY_FOR_PICKUP),
            OrderStatus.READY_FOR_PICKUP, Set.of(OrderStatus.COMPLETED, OrderStatus.NO_SHOW_PENDING),
            OrderStatus.NO_SHOW_PENDING, Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
    );

    private final OrderStatusEventRepository statusEventRepository;

    public OrderStateMachine(OrderStatusEventRepository statusEventRepository) {
        this.statusEventRepository = statusEventRepository;
    }

    /**
     * 校验并执行状态转移。
     *
     * @param order        订单实体（会被修改状态）
     * @param newStatus    目标状态
     * @param operatorType 操作者类型
     * @param operatorId   操作者ID
     * @param note         备注
     * @throws IllegalStateException 如果状态转移非法
     */
    @Transactional
    public void transition(Order order, OrderStatus newStatus,
                            OperatorType operatorType, Long operatorId, String note) {
        OrderStatus fromStatus = order.getStatus();

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(fromStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    String.format("非法的状态转移: %s → %s (订单 %s)", fromStatus, newStatus, order.getOrderNo()));
        }

        // 更新订单状态
        order.setStatus(newStatus);

        // 记录状态变更事件
        OrderStatusEvent event = OrderStatusEvent.of(
                order.getId(),
                fromStatus.name(),
                newStatus.name(),
                operatorType != null ? operatorType.name() : null,
                operatorId,
                note
        );
        statusEventRepository.save(event);

        log.info("订单 {} 状态转移: {} → {}, 操作者: [{}]{}",
                order.getOrderNo(), fromStatus, newStatus,
                operatorType, operatorId);
    }

    /**
     * 校验状态转移是否合法。
     *
     * @param fromStatus 当前状态
     * @param toStatus   目标状态
     * @return true 如果转移合法
     */
    public boolean isValidTransition(OrderStatus fromStatus, OrderStatus toStatus) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(fromStatus);
        return allowed != null && allowed.contains(toStatus);
    }
}
