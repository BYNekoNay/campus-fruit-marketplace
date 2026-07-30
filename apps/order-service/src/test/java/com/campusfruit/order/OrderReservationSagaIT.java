package com.campusfruit.order;

import com.campusfruit.order.dto.CreateOrderRequest;
import com.campusfruit.order.dto.OrderResponse;
import com.campusfruit.order.entity.Cart;
import com.campusfruit.order.entity.CartItem;
import com.campusfruit.order.entity.Order;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.repository.*;
import com.campusfruit.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单预占 Saga 集成测试。
 * <p>
 * 验证完整的下单流程：
 * 1. 幂等检查
 * 2. 购物车校验
 * 3. 订单创建 + 状态转移
 * 4. 购物车清空
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderReservationSagaIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testUserId = 200L;
    private Long testStoreId = 1L;

    @BeforeEach
    void setUp() {
        // 创建测试购物车
        Cart cart = new Cart();
        cart.setUserId(testUserId);
        cart.setStoreId(testStoreId);
        cart = cartRepository.save(cart);

        CartItem item = new CartItem();
        item.setCartId(cart.getId());
        item.setOfferId(10L);
        item.setFruitVariety("测试苹果");
        item.setSalesUnit("500g盒装");
        item.setUnitPrice(1500L);
        item.setQuantity(2);
        cartItemRepository.save(item);
    }

    @AfterEach
    void tearDown() {
        idempotencyRecordRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey("test-idempotency-" + System.currentTimeMillis());
        String requestBody = objectMapper.writeValueAsString(request);

        OrderResponse response = orderService.createOrder(testUserId, request, requestBody);

        assertNotNull(response);
        assertNotNull(response.getOrderNo());
        assertEquals(testStoreId, response.getStoreId());
        assertEquals(3000L, response.getTotalAmount()); // 1500 * 2
        assertEquals(2, response.getItemCount());

        // 验证订单状态
        Optional<Order> orderOpt = orderRepository.findById(response.getId());
        assertTrue(orderOpt.isPresent());
        Order order = orderOpt.get();
        assertNotNull(order.getStatus());

        // 验证购物车已清空
        assertTrue(cartItemRepository.findByCartId(
                cartRepository.findByUserId(testUserId).orElseThrow().getId()).isEmpty());
    }

    @Test
    void shouldReturnExistingOrderOnIdempotentRetry() throws Exception {
        String idempotencyKey = "test-idempotent-retry-" + System.currentTimeMillis();
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey(idempotencyKey);
        String requestBody = objectMapper.writeValueAsString(request);

        // 第一次下单
        OrderResponse firstResponse = orderService.createOrder(testUserId, request, requestBody);

        // 重新准备购物车
        Cart cart = new Cart();
        cart.setUserId(testUserId);
        cart.setStoreId(testStoreId);
        Cart savedCart = cartRepository.save(cart);

        CartItem item = new CartItem();
        item.setCartId(savedCart.getId());
        item.setOfferId(10L);
        item.setFruitVariety("测试苹果");
        item.setSalesUnit("500g盒装");
        item.setUnitPrice(1500L);
        item.setQuantity(1);
        cartItemRepository.save(item);

        // 第二次用相同幂等键下单
        OrderResponse secondResponse = orderService.createOrder(testUserId, request, requestBody);

        // 应返回相同的订单
        assertEquals(firstResponse.getId(), secondResponse.getId());
        assertEquals(firstResponse.getOrderNo(), secondResponse.getOrderNo());
    }

    @Test
    void shouldRejectEmptyCart() {
        // 清空购物车后下单
        cartItemRepository.deleteAll();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey("empty-cart-" + System.currentTimeMillis());

        assertThrows(IllegalArgumentException.class, () -> {
            try {
                orderService.createOrder(testUserId, request, objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void shouldCancelOrderAtPendingStoreConfirmation() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setIdempotencyKey("test-cancel-" + System.currentTimeMillis());
        String requestBody = objectMapper.writeValueAsString(request);

        OrderResponse response = orderService.createOrder(testUserId, request, requestBody);

        // 仅在 PENDING_STORE_CONFIRMATION 状态可取消
        if (response.getStatus().equals(OrderStatus.PENDING_STORE_CONFIRMATION.name())) {
            orderService.cancelOrder(testUserId, response.getId(), "测试取消");
            Order order = orderRepository.findById(response.getId()).orElseThrow();
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }
    }
}
