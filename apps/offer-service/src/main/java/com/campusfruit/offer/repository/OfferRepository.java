package com.campusfruit.offer.repository;

import com.campusfruit.offer.entity.Offer;
import com.campusfruit.offer.enums.OfferStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByStoreId(Long storeId);

    List<Offer> findByCanonicalFruitId(Long canonicalFruitId);

    Optional<Offer> findByStoreIdAndCanonicalFruitId(Long storeId, Long canonicalFruitId);

    List<Offer> findByStoreIdAndStatus(Long storeId, OfferStatus status);

    Page<Offer> findAllByOrderByIdAsc(Pageable pageable);

    Page<Offer> findByIdGreaterThan(Long cursorId, Pageable pageable);

    /**
     * 条件更新预占库存：available >= quantity 时才执行扣减。
     * 返回影响行数，0 表示库存不足。
     */
    @Modifying
    @Transactional
    @Query("UPDATE Offer o SET o.availableQuantity = o.availableQuantity - :quantity, " +
           "o.reservedQuantity = o.reservedQuantity + :quantity " +
           "WHERE o.id = :id AND o.availableQuantity >= :quantity")
    int reserveStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 查找指定状态且最后确认时间早于给定时间的报价。
     */
    @Query("SELECT o FROM Offer o WHERE o.status = :status AND o.lastConfirmedAt IS NOT NULL AND o.lastConfirmedAt < :before")
    List<Offer> findByStatusAndLastConfirmedAtBefore(@Param("status") OfferStatus status, @Param("before") Instant before);

    /**
     * 查找指定状态且更新时间早于给定时间的报价。
     */
    @Query("SELECT o FROM Offer o WHERE o.status = :status AND o.updatedAt < :before")
    List<Offer> findByStatusAndUpdatedAtBefore(@Param("status") OfferStatus status, @Param("before") Instant before);
}
