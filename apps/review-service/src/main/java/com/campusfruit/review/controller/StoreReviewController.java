package com.campusfruit.review.controller;

import com.campusfruit.review.dto.ReviewListResponse;
import com.campusfruit.review.dto.ReviewResponse;
import com.campusfruit.review.repository.MerchantReplyRepository;
import com.campusfruit.review.repository.ReviewRepository;
import com.campusfruit.review.repository.RatingAggregateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店评价查询控制器。不要求认证，公开接口。
 */
@RestController
public class StoreReviewController {

    private final ReviewRepository reviewRepository;
    private final RatingAggregateRepository ratingAggregateRepository;
    private final MerchantReplyRepository merchantReplyRepository;
    private final ObjectMapper objectMapper;

    public StoreReviewController(ReviewRepository reviewRepository,
                                   RatingAggregateRepository ratingAggregateRepository,
                                   MerchantReplyRepository merchantReplyRepository,
                                   ObjectMapper objectMapper) {
        this.reviewRepository = reviewRepository;
        this.ratingAggregateRepository = ratingAggregateRepository;
        this.merchantReplyRepository = merchantReplyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 门店评价列表（含评分统计）。
     */
    @GetMapping("/api/stores/{storeId}/reviews")
    public ResponseEntity<ReviewListResponse> getStoreReviews(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReviewResponse> reviewPage = reviewRepository
                .findByStoreIdAndVisibleTrueOrderByCreatedAtDesc(storeId, PageRequest.of(page, size))
                .map(this::toReviewResponse);

        ReviewListResponse response = new ReviewListResponse();
        response.setItems(reviewPage.getContent());

        // 填充评分统计
        ratingAggregateRepository.findByStoreId(storeId).ifPresent(agg -> {
            response.setAvgRating(agg.getAvgRating());
            response.setBayesianRating(agg.getBayesianRating());
            response.setTotalRatings(agg.getTotalRatings());
            if (agg.getRatingDistribution() != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> dist = objectMapper.readValue(
                            agg.getRatingDistribution(), Map.class);
                    Map<Integer, Integer> distribution = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : dist.entrySet()) {
                        distribution.put(Integer.parseInt(entry.getKey()), entry.getValue());
                    }
                    response.setDistribution(distribution);
                } catch (Exception e) {
                    // ignore
                }
            }
        });

        return ResponseEntity.ok(response);
    }

    private ReviewResponse toReviewResponse(
            com.campusfruit.review.entity.Review review) {
        ReviewResponse dto = new ReviewResponse();
        dto.setId(review.getId());
        dto.setUserId(review.getUserId());
        dto.setOrderId(review.getOrderId());
        dto.setStoreId(review.getStoreId());
        dto.setRating(review.getRating());
        dto.setContent(review.getContent());
        dto.setTags(review.getTags() != null ? review.getTags().split(",") : null);
        dto.setStatus(review.getStatus().name());
        dto.setVersion(review.getCurrentVersion());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());

        merchantReplyRepository.findByReviewId(review.getId()).ifPresent(reply -> {
            if ("ACTIVE".equals(reply.getStatus())) {
                dto.setMerchantReply(reply.getContent());
            }
        });

        return dto;
    }
}
