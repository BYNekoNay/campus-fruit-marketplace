package com.campusfruit.order.repository;

import com.campusfruit.order.entity.OrderStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusEventRepository extends JpaRepository<OrderStatusEvent, Long> {

    List<OrderStatusEvent> findByOrderIdOrderByCreatedAt(Long orderId);
}
