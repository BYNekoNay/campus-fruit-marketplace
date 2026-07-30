package com.campusfruit.order;

import com.campusfruit.order.repository.*;
import com.campusfruit.order.service.CartService;
import com.campusfruit.order.service.OrderService;
import com.campusfruit.order.service.OrderQueryService;
import com.campusfruit.order.service.OrderStateMachine;
import com.campusfruit.order.service.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 应用上下文加载集成测试。
 * 验证所有核心 Bean 能被正确注入。
 */
@SpringBootTest
@ActiveProfiles("test")
class ContextLoadIT {

    @Autowired(required = false)
    private CartRepository cartRepository;

    @Autowired(required = false)
    private CartItemRepository cartItemRepository;

    @Autowired(required = false)
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private OrderItemRepository orderItemRepository;

    @Autowired(required = false)
    private OrderStatusEventRepository orderStatusEventRepository;

    @Autowired(required = false)
    private OutboxEventRepository outboxEventRepository;

    @Autowired(required = false)
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired(required = false)
    private CartService cartService;

    @Autowired(required = false)
    private OrderService orderService;

    @Autowired(required = false)
    private OrderQueryService orderQueryService;

    @Autowired(required = false)
    private OrderStateMachine orderStateMachine;

    @Autowired(required = false)
    private OutboxPublisher outboxPublisher;

    @Test
    void contextLoads() {
        // 验证 Spring 应用上下文能够正常加载
    }

    @Test
    void repositoriesAreAvailable() {
        assertNotNull(cartRepository, "CartRepository should be available");
        assertNotNull(orderRepository, "OrderRepository should be available");
        assertNotNull(outboxEventRepository, "OutboxEventRepository should be available");
        assertNotNull(idempotencyRecordRepository, "IdempotencyRecordRepository should be available");
    }

    @Test
    void servicesAreAvailable() {
        assertNotNull(cartService, "CartService should be available");
        assertNotNull(orderService, "OrderService should be available");
        assertNotNull(orderQueryService, "OrderQueryService should be available");
        assertNotNull(orderStateMachine, "OrderStateMachine should be available");
        assertNotNull(outboxPublisher, "OutboxPublisher should be available");
    }
}
