package com.campusfruit.review.repository;

import com.campusfruit.review.entity.MerchantReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantReplyRepository extends JpaRepository<MerchantReply, Long> {

    Optional<MerchantReply> findByReviewId(Long reviewId);
}
