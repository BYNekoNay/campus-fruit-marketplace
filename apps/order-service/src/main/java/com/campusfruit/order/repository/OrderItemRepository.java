package com.campusfruit.order.repository;

import com.campusfruit.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    /**
     * 查询指定门店在某时间范围内所有已完成订单的订单商品，按 offer_id 分组统计销量。
     */
    @Query(value = "SELECT oi.offer_id, SUM(oi.quantity) FROM order_items oi " +
           "JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.store_id = :storeId AND o.status = 'COMPLETED' AND o.updated_at >= :since " +
           "GROUP BY oi.offer_id", nativeQuery = true)
    List<Object[]> countSalesByOfferIdAndStoreSince(@Param("storeId") Long storeId, @Param("since") Instant since);
}
