package com.campusfruit.review.repository;

import com.campusfruit.review.entity.ReviewVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewVersionRepository extends JpaRepository<ReviewVersion, Long> {

    List<ReviewVersion> findByReviewIdOrderByVersionDesc(Long reviewId);
}
