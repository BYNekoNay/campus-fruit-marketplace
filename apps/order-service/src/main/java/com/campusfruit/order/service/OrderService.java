package com.campusfruit.order.service;

import com.campusfruit.order.dto.CreateOrderRequest;
import com.campusfruit.order.dto.OrderItemResponse;
import com.campusfruit.order.dto.OrderResponse;
import com.campusfruit.order.dto.QuoteValidationResult;
import com.campusfruit.order.entity.*;
import com.campusfruit.order.enums.OperatorType;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.enums.PaymentStatus;
import com.campusfruit.order.repository.*;
import com.campusfruit.order.saga.OfferServiceClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusEventRepository statusEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderStateMachine stateMachine;
    private final OfferServiceClient offerServiceClient;
    private final ObjectMapper objectMapper;

    public OrderService(CartRepository cartRepository,
                         CartItemRepository cartItemRepository,
                         OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         OrderStatusEventRepository statusEventRepository,
                         IdempotencyRecordRepository idempotencyRecordRepository,
                         OutboxEventRepository outboxEventRepository,
                         OrderStateMachine stateMachine,
                         OfferServiceClient offerServiceClient,
                         ObjectMapper objectMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusEventRepository = statusEventRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.stateMachine = stateMachine;
        this.offerServiceClient = offerServiceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 核心下单流程。
     * <pre>
     * 1. 幂等检查(idempotencyKey) → 已存在返回已有结果
     * 2. 校验购物车非空、门店一致
     * 3. 创建订单(PENDING_RESERVATION) + items
     * 4. 保存幂等记录(PROCESSING)
     * 5. 写入 outbox 事件
     * 6. 调用 Offer Service 预占(通过 RestClient)
     * 7. 预占成功 → 状态转为 PENDING_STORE_CONFIRMATION
     * 8. 预占失败 → 状态转为 REJECTED
     * 9. 更新幂等记录为 COMPLETED/REJECTED
     * 10. 清空购物车
     * 11. 发布 OrderCreated 事件(outbox)
     * </pre>
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request, String requestBody) {
        String idempotencyKey = request.getIdempotencyKey();

        // 1. 幂等检查
        Optional<IdempotencyRecord> existingRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            if ("COMPLETED".equals(record.getStatus()) && record.getResourceId() != null) {
                Order order = orderRepository.findById(Long.valueOf(record.getResourceId()))
                        .orElseThrow(() -> new IllegalStateException("订单不存在但幂等记录已完成"));
                return toOrderResponse(order);
            }
            if ("PROCESSING".equals(record.getStatus())) {
                throw new IllegalStateException("订单处理中，请勿重复提交");
            }
            throw new IllegalStateException("订单已被拒绝，请使用新的幂等键重试");
        }

        // 2. 校验购物车
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("购物车为空"));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("购物车为空");
        }

        // 计算总金额和数量
        long totalAmount = 0L;
        int itemCount = 0;
        for (CartItem item : cartItems) {
            totalAmount += item.getUnitPrice() * item.getQuantity();
            itemCount += item.getQuantity();
        }

        // 2.5 报价时效性校验：比对购物车快照价格与当前报价
        QuoteValidationResult quoteResult = validateQuotes(cartItems);
        if (!quoteResult.isValid()) {
            log.warn("STALE_QUOTE: 用户 {} 的购物车报价已过期", userId);
            throw new StaleQuoteException("报价已变更，请重新确认", quoteResult);
        }

        // 4. 保存幂等记录(PROCESSING)
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord();
        idempotencyRecord.setIdempotencyKey(idempotencyKey);
        idempotencyRecord.setSubjectId(userId);
        idempotencyRecord.setEndpoint("POST /api/orders");
        idempotencyRecord.setRequestBodyHash(sha256(requestBody));
        idempotencyRecord.setStatus("PROCESSING");
        idempotencyRecord.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        idempotencyRecordRepository.save(idempotencyRecord);

        // 3. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStoreId(cart.getStoreId());
        order.setStatus(OrderStatus.PENDING_RESERVATION);
        order.setTotalAmount(totalAmount);
        order.setItemCount(itemCount);
        order.setIdempotencyKey(idempotencyKey);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order = orderRepository.save(order);

        // 保存订单商品快照
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setOfferId(cartItem.getOfferId());
            orderItem.setFruitVariety(cartItem.getFruitVariety());
            orderItem.setSalesUnit(cartItem.getSalesUnit());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        // 记录初始状态事件
        OrderStatusEvent initEvent = OrderStatusEvent.of(
                order.getId(), null, OrderStatus.PENDING_RESERVATION.name(),
                OperatorType.SYSTEM.name(), null, "订单创建"
        );
        statusEventRepository.save(initEvent);

        // 5. 写入 outbox 事件
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", order.getId());
            payload.put("orderNo", order.getOrderNo());
            payload.put("userId", userId);
            payload.put("storeId", order.getStoreId());
            payload.put("totalAmount", totalAmount);

            OutboxEvent outbox = OutboxEvent.of(
                    "Order", order.getId().toString(),
                    "com.campusfruit.order.OrderCreated",
                    objectMapper.writeValueAsString(payload)
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("写入 outbox 事件失败", e);
        }

        // 6-8. 模拟预占逻辑（实际应通过 RestClient 调用 Offer Service）
        boolean reservationSuccess = simulateReservation(order, orderItems);

        idempotencyRecord.setResourceId(order.getId().toString());

        if (reservationSuccess) {
            // 预占成功 → PENDING_STORE_CONFIRMATION
            stateMachine.transition(order, OrderStatus.PENDING_STORE_CONFIRMATION,
                    OperatorType.SYSTEM, null, "预占成功");
            idempotencyRecord.setStatus("COMPLETED");
            idempotencyRecord.setResponseBody("{\"status\":\"COMPLETED\",\"orderNo\":\"" + order.getOrderNo() + "\"}");
        } else {
            // 预占失败 → REJECTED
            stateMachine.transition(order, OrderStatus.REJECTED,
                    OperatorType.SYSTEM, null, "库存预占失败");
            order.setCancelReason("库存不足，预占失败");
            idempotencyRecord.setStatus("REJECTED");
            idempotencyRecord.setResponseBody("{\"status\":\"REJECTED\",\"reason\":\"库存不足\"}");
        }
        orderRepository.save(order);
        idempotencyRecordRepository.save(idempotencyRecord);

        // 10. 清空购物车
        cartItemRepository.deleteByCartId(cart.getId());

        // 11. 发布 OrderCreated 事件(outbox)
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("orderId", order.getId());
            eventPayload.put("orderNo", order.getOrderNo());
            eventPayload.put("userId", userId);
            eventPayload.put("storeId", order.getStoreId());
            eventPayload.put("status", order.getStatus().name());
            eventPayload.put("totalAmount", totalAmount);
            eventPayload.put("itemCount", itemCount);

            OutboxEvent createdEvent = OutboxEvent.of(
                    "Order", order.getId().toString(),
                    "com.campusfruit.order.OrderCreated",
                    objectMapper.writeValueAsString(eventPayload)
            );
            outboxEventRepository.save(createdEvent);
        } catch (JsonProcessingException e) {
            log.error("写入 OrderCreated outbox 事件失败", e);
        }

        log.info("用户 {} 下单完成: orderNo={}, status={}, totalAmount={}",
                userId, order.getOrderNo(), order.getStatus(), totalAmount);

        return toOrderResponse(order);
    }

    /**
     * 用户取消订单（仅 PENDING_STORE_CONFIRMATION 状态可取消）。
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此订单");
        }

        if (order.getStatus() != OrderStatus.PENDING_STORE_CONFIRMATION) {
            throw new IllegalArgumentException("当前状态不允许取消，状态: " + order.getStatus().getLabel());
        }

        stateMachine.transition(order, OrderStatus.CANCELLED,
                OperatorType.USER, userId, reason != null ? reason : "用户取消");
        order.setCancelReason(reason);
        orderRepository.save(order);

        log.info("用户 {} 取消订单: orderNo={}, reason={}", userId, order.getOrderNo(), reason);
    }

    /**
     * 门店接单（PENDING_STORE_CONFIRMATION → ACCEPTED）。
     */
    @Transactional
    public void acceptOrder(Long storeId, Long orderId, Long staffId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("订单不属于该门店");
        }

        if (order.getStatus() != OrderStatus.PENDING_STORE_CONFIRMATION) {
            throw new IllegalArgumentException("当前状态不允许接单，状态: " + order.getStatus().getLabel());
        }

        stateMachine.transition(order, OrderStatus.ACCEPTED,
                OperatorType.STORE_STAFF, staffId, "门店接单");
        orderRepository.save(order);

        log.info("门店 {} 接单: orderNo={}, staffId={}", storeId, order.getOrderNo(), staffId);
    }

    /**
     * 门店拒单（→ REJECTED）。
     */
    @Transactional
    public void rejectOrder(Long storeId, Long orderId, Long staffId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("订单不属于该门店");
        }

        if (order.getStatus() != OrderStatus.PENDING_STORE_CONFIRMATION) {
            throw new IllegalArgumentException("当前状态不允许拒单，状态: " + order.getStatus().getLabel());
        }

        stateMachine.transition(order, OrderStatus.REJECTED,
                OperatorType.STORE_STAFF, staffId, reason != null ? reason : "门店拒单");
        order.setCancelReason(reason);
        orderRepository.save(order);

        log.info("门店 {} 拒单: orderNo={}, reason={}", storeId, order.getOrderNo(), reason);
    }

    /**
     * 备货完成（ACCEPTED → READY_FOR_PICKUP）。
     */
    @Transactional
    public void readyOrder(Long storeId, Long orderId, Long staffId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("订单不属于该门店");
        }

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalArgumentException("当前状态不允许标记备货完成，状态: " + order.getStatus().getLabel());
        }

        // 生成自取码
        String pickupCode = generatePickupCode();
        order.setPickupCodeHash(sha256(pickupCode));
        order.setPickupCodeExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));

        stateMachine.transition(order, OrderStatus.READY_FOR_PICKUP,
                OperatorType.STORE_STAFF, staffId, "备货完成，自取码: " + pickupCode);
        orderRepository.save(order);

        log.info("门店 {} 备货完成: orderNo={}, pickupCode={}", storeId, order.getOrderNo(), pickupCode);
    }

    /**
     * 核销完成（READY_FOR_PICKUP → COMPLETED，同时 payment_status → PAID_AT_PICKUP）。
     */
    @Transactional
    public void completeOrder(Long storeId, Long orderId, Long staffId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("订单不属于该门店");
        }

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalArgumentException("当前状态不允许核销，状态: " + order.getStatus().getLabel());
        }

        stateMachine.transition(order, OrderStatus.COMPLETED,
                OperatorType.STORE_STAFF, staffId, "核销完成");
        order.setPaymentStatus(PaymentStatus.PAID_AT_PICKUP);
        orderRepository.save(order);

        log.info("门店 {} 核销完成: orderNo={}", storeId, order.getOrderNo());
    }

    /**
     * 标记未取货（READY_FOR_PICKUP → NO_SHOW_PENDING）。
     */
    @Transactional
    public void markNoShow(Long storeId, Long orderId, Long staffId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getStoreId().equals(storeId)) {
            throw new IllegalArgumentException("订单不属于该门店");
        }

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalArgumentException("当前状态不允许标记未取货，状态: " + order.getStatus().getLabel());
        }

        stateMachine.transition(order, OrderStatus.NO_SHOW_PENDING,
                OperatorType.STORE_STAFF, staffId, "用户未取货");
        orderRepository.save(order);

        log.info("门店 {} 标记未取货: orderNo={}", storeId, order.getOrderNo());
    }

    /**
     * 获取订单自取码（仅 READY_FOR_PICKUP 状态且未过期）。
     */
    @Transactional(readOnly = true)
    public String getPickupCode(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权查看此订单");
        }

        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalArgumentException("订单未就绪，无法获取自取码");
        }

        if (order.getPickupCodeExpiresAt() != null
                && order.getPickupCodeExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("自取码已过期");
        }

        return "已生成自取码（实际返回需解密）";
    }

    // --- 私有辅助方法 ---

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setStoreId(order.getStoreId());
        response.setStatus(order.getStatus().name());
        response.setStatusLabel(order.getStatus().getLabel());
        response.setTotalAmount(order.getTotalAmount());
        response.setItemCount(order.getItemCount());
        response.setPaymentStatus(order.getPaymentStatus().name());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getPickupCodeHash() != null) {
            response.setPickupCode("***");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemResponse ir = new OrderItemResponse();
            ir.setId(item.getId());
            ir.setOfferId(item.getOfferId());
            ir.setFruitVariety(item.getFruitVariety());
            ir.setSalesUnit(item.getSalesUnit());
            ir.setUnitPrice(item.getUnitPrice());
            ir.setQuantity(item.getQuantity());
            itemResponses.add(ir);
        }
        response.setItems(itemResponses);

        return response;
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }

    private String generatePickupCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 模拟库存预占。
     * 实际应通过 RestClient 调用 Offer Service 的预占接口。
     */
    private boolean simulateReservation(Order order, List<OrderItem> items) {
        // TODO: 实际实现应调用 Offer Service HTTP API
        log.info("模拟预占订单 {} 的 {} 个商品", order.getOrderNo(), items.size());
        order.setReservationId("RESV-" + UUID.randomUUID().toString().substring(0, 8));
        return true;
    }

    /**
     * 报价时效性校验：比对购物车中保存的快照价格与当前报价。
     */
    private QuoteValidationResult validateQuotes(List<CartItem> cartItems) {
        QuoteValidationResult result = new QuoteValidationResult();

        for (CartItem item : cartItems) {
            Map<String, Object> currentOffer;
            try {
                currentOffer = offerServiceClient.getOfferDetail(item.getOfferId());
            } catch (Exception e) {
                log.warn("无法获取报价详情 offerId={}: {}", item.getOfferId(), e.getMessage());
                result.addChange(new QuoteValidationResult.ChangeDetail(
                        "offer_unavailable", "报价 " + item.getOfferId() + " 暂不可用"));
                continue;
            }

            // 检查 current offer 是否返回有效数据
            if (currentOffer.containsKey("error")) {
                log.warn("获取报价详情返回错误 offerId={}", item.getOfferId());
                result.addChange(new QuoteValidationResult.ChangeDetail(
                        "offer_error", "获取报价 " + item.getOfferId() + " 失败"));
                continue;
            }

            // 比较价格
            Object currentPriceObj = currentOffer.get("unitPrice");
            if (currentPriceObj != null) {
                long currentPrice = ((Number) currentPriceObj).longValue();
                if (currentPrice != item.getUnitPrice()) {
                    result.setPriceChanged(true);
                    result.setCurrentUnitPrice(currentPrice);
                    result.setSnapshotUnitPrice(item.getUnitPrice());
                    result.addChange(new QuoteValidationResult.ChangeDetail(
                            "price", "报价 " + item.getOfferId() + " 价格已变更: "
                                    + item.getUnitPrice() + " -> " + currentPrice));
                }
            }

            // 比较 offerVersion
            Object versionObj = currentOffer.get("offerVersion");
            if (versionObj != null) {
                int currentVersion = ((Number) versionObj).intValue();
                result.setCurrentOfferVersion(currentVersion);
                if (item.getOfferVersion() != null && currentVersion != item.getOfferVersion()) {
                    result.setSnapshotOfferVersion(item.getOfferVersion());
                    if (!result.isPriceChanged()) {
                        result.addChange(new QuoteValidationResult.ChangeDetail(
                                "version", "报价 " + item.getOfferId() + " 版本已更新: "
                                        + item.getOfferVersion() + " -> " + currentVersion));
                    }
                }
            }

            // 检查库存
            Object qtyObj = currentOffer.get("availableQuantity");
            if (qtyObj != null) {
                int availableQty = ((Number) qtyObj).intValue();
                if (availableQty < item.getQuantity()) {
                    result.setStockChanged(true);
                    result.addChange(new QuoteValidationResult.ChangeDetail(
                            "stock", "报价 " + item.getOfferId() + " 库存不足: 需要"
                                    + item.getQuantity() + " 但有 " + availableQty));
                }
            }

            // 检查门店营业状态
            Object storeStatusObj = currentOffer.get("storeStatus");
            if (storeStatusObj != null && !"ACTIVE".equalsIgnoreCase(storeStatusObj.toString())) {
                result.setStoreStatusChanged(true);
                result.addChange(new QuoteValidationResult.ChangeDetail(
                        "store_status", "门店 " + item.getOfferId() + " 已暂停营业"));
            }
        }

        return result;
    }

    /**
     * 报价过期异常（STALE_QUOTE）。
     */
    public static class StaleQuoteException extends RuntimeException {
        private final QuoteValidationResult validationResult;

        public StaleQuoteException(String message, QuoteValidationResult validationResult) {
            super(message);
            this.validationResult = validationResult;
        }

        public QuoteValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
