package com.campusfruit.order.service;

import com.campusfruit.order.dto.OrderItemResponse;
import com.campusfruit.order.dto.OrderResponse;
import com.campusfruit.order.entity.Order;
import com.campusfruit.order.entity.OrderItem;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.repository.OrderItemRepository;
import com.campusfruit.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderQueryService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * 查询单个订单。
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权查看此订单");
        }

        return toOrderResponse(order);
    }

    /**
     * 我的订单列表（分页）。
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(this::toOrderResponse);
    }

    /**
     * 门店订单列表（分页，可选状态过滤）。
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getStoreOrders(Long storeId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStoreIdAndStatus(storeId, status, pageable);
        } else {
            orders = orderRepository.findByStoreId(storeId, pageable);
        }
        return orders.map(this::toOrderResponse);
    }

    /**
     * 游标分页投影导出（供内部服务调用）。
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> exportOrders(Long lastId, int limit) {
        // 简化实现：按ID升序取 limit 条
        // 实际应使用游标分页优化大表扫描
        List<Order> orders = orderRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, limit,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"))).getContent();
        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(toOrderResponse(order));
        }
        return responses;
    }

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
}
