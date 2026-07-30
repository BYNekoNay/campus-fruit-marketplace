package com.campusfruit.review.repository;

import com.campusfruit.review.entity.ReviewEligibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewEligibilityRepository extends JpaRepository<ReviewEligibility, Long> {

    Optional<ReviewEligibility> findByUserIdAndStoreIdAndOrderIdAndUsedFalseAndTombstoneFalse(
            Long userId, Long storeId, Long orderId);

    Optional<ReviewEligibility> findByUserIdAndStoreIdAndOrderId(
            Long userId, Long storeId, Long orderId);

    Optional<ReviewEligibility> findByOrderId(Long orderId);
}
