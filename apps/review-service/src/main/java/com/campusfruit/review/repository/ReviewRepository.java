package com.campusfruit.review.repository;

import com.campusfruit.review.entity.Review;
import com.campusfruit.review.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByStoreIdAndVisibleTrueOrderByCreatedAtDesc(Long storeId, Pageable pageable);

    Optional<Review> findByOrderId(Long orderId);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Long countByStoreIdAndVisibleTrue(Long storeId);

    /** 查询门店所有 ACTIVE 且 visible 的评价（用于贝叶斯评分计算） */
    List<Review> findByStoreIdAndStatusAndVisibleTrue(Long storeId, ReviewStatus status);

    /** 查询用户在指定时间窗口内的评价数（用于风控频率检测） */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.userId = :userId AND r.createdAt >= :since")
    Long countByUserIdAndCreatedAtAfter(Long userId, Instant since);

    /** 查询指定时间窗口内门店的新增评价数（用于风控激增检测） */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.storeId = :storeId AND r.createdAt >= :since")
    Long countByStoreIdAndCreatedAtAfter(Long storeId, Instant since);

    /** 查找与指定文本高度相似的评价（用于风控重复内容检测） */
    List<Review> findByUserIdAndContent(Long userId, String content);
}
