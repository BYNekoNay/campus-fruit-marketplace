package com.campusfruit.order.repository;

import com.campusfruit.order.entity.Order;
import com.campusfruit.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByStoreIdAndStatus(Long storeId, OrderStatus status);

    Page<Order> findByStoreId(Long storeId, Pageable pageable);

    Page<Order> findByStoreIdAndStatus(Long storeId, OrderStatus status, Pageable pageable);

    Optional<Order> findByOrderNo(String orderNo);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    // ======== 对账相关查询 ========

    /**
     * 查询超时未完成的 PENDING_RESERVATION 订单（孤儿订单扫描），使用 SKIP LOCKED 避免重复处理。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_RESERVATION' AND o.createdAt < :beforeTime ORDER BY o.id")
    List<Order> findStalePendingReservationOrders(@Param("beforeTime") Instant beforeTime);

    /**
     * 查询 PENDING_STORE_CONFIRMATION 超时订单（门店确认超时）。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_STORE_CONFIRMATION' AND o.createdAt < :beforeTime ORDER BY o.id")
    List<Order> findStalePendingConfirmationOrders(@Param("beforeTime") Instant beforeTime);

    /**
     * 查询 NO_SHOW_PENDING 宽限期超时订单。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.status = 'NO_SHOW_PENDING' AND o.updatedAt < :beforeTime ORDER BY o.id")
    List<Order> findStaleNoShowOrders(@Param("beforeTime") Instant beforeTime);

    /**
     * 近30天已完成订单的销售统计：按门店分组统计订单数。
     */
    @Query("SELECT o.storeId, COUNT(o) FROM Order o WHERE o.status = 'COMPLETED' AND o.updatedAt >= :since GROUP BY o.storeId")
    List<Object[]> countCompletedOrdersByStoreSince(@Param("since") Instant since);
}
