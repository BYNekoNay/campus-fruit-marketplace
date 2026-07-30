package com.campusfruit.review.service;

import com.campusfruit.review.dto.ReviewResponse;
import com.campusfruit.review.dto.SubmitReviewRequest;
import com.campusfruit.review.dto.UpdateReviewRequest;
import com.campusfruit.review.entity.MerchantReply;
import com.campusfruit.review.entity.Review;
import com.campusfruit.review.entity.ReviewEligibility;
import com.campusfruit.review.entity.ReviewVersion;
import com.campusfruit.review.enums.ReviewStatus;
import com.campusfruit.review.repository.*;
import com.campusfruit.review.scoring.RatingAggregateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评价服务。处理评价的提交、修改、删除和查询。
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private static final int MAX_REVIEW_VERSIONS = 2;

    private final ReviewRepository reviewRepository;
    private final ReviewVersionRepository reviewVersionRepository;
    private final ReviewEligibilityRepository eligibilityRepository;
    private final MerchantReplyRepository merchantReplyRepository;
    private final RatingAggregateRepository ratingAggregateRepository;
    private final RatingAggregateService ratingAggregateService;

    public ReviewService(ReviewRepository reviewRepository,
                          ReviewVersionRepository reviewVersionRepository,
                          ReviewEligibilityRepository eligibilityRepository,
                          MerchantReplyRepository merchantReplyRepository,
                          RatingAggregateRepository ratingAggregateRepository,
                          RatingAggregateService ratingAggregateService) {
        this.reviewRepository = reviewRepository;
        this.reviewVersionRepository = reviewVersionRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.merchantReplyRepository = merchantReplyRepository;
        this.ratingAggregateRepository = ratingAggregateRepository;
        this.ratingAggregateService = ratingAggregateService;
    }

    /**
     * 提交评价。
     */
    @Transactional
    public ReviewResponse submitReview(Long userId, SubmitReviewRequest dto) {
        // 1. 查询资格
        ReviewEligibility eligibility = eligibilityRepository
                .findByUserIdAndStoreIdAndOrderIdAndUsedFalseAndTombstoneFalse(
                        userId, dto.getStoreId(), dto.getOrderId())
                .orElseThrow(() -> new SecurityException("无评价资格，仅完成订单后可评价"));

        // 2. 检查是否已评价该订单
        reviewRepository.findByOrderId(dto.getOrderId()).ifPresent(existing -> {
            throw new IllegalStateException("该订单已评价，不能重复提交");
        });

        // 3. 创建评价
        Review review = new Review();
        review.setUserId(userId);
        review.setStoreId(dto.getStoreId());
        review.setOrderId(dto.getOrderId());
        review.setRating(dto.getRating());
        review.setContent(stripHtml(dto.getContent()));
        review.setTags(joinTags(dto.getTags()));
        review.setStatus(ReviewStatus.ACTIVE);
        review.setCurrentVersion(1);
        review.setVisible(true);
        review = reviewRepository.save(review);

        // 4. 创建初始版本记录
        ReviewVersion version = new ReviewVersion();
        version.setReviewId(review.getId());
        version.setVersion(1);
        version.setRating(review.getRating());
        version.setContent(review.getContent());
        version.setTags(review.getTags());
        reviewVersionRepository.save(version);

        // 5. 标记资格已使用
        eligibility.setUsed(true);
        eligibilityRepository.save(eligibility);

        // 6. 异步重算评分聚合
        recalculateRating(review.getStoreId());

        log.info("用户 {} 提交评价: reviewId={}, storeId={}, orderId={}, rating={}",
                userId, review.getId(), dto.getStoreId(), dto.getOrderId(), dto.getRating());

        return toReviewResponse(review);
    }

    /**
     * 修改评价（仅允许修改一次）。
     */
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, UpdateReviewRequest dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("评价不存在"));

        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("无权修改他人评价");
        }

        // 检查版本数（允许修改一次 = 最多2个版本）
        if (review.getCurrentVersion() >= MAX_REVIEW_VERSIONS) {
            throw new IllegalStateException("评价只能修改一次");
        }

        int newVersion = review.getCurrentVersion() + 1;

        // 保存旧版本历史
        ReviewVersion version = new ReviewVersion();
        version.setReviewId(review.getId());
        version.setVersion(newVersion);
        version.setRating(dto.getRating() != null ? dto.getRating() : review.getRating());
        version.setContent(dto.getContent() != null ? stripHtml(dto.getContent()) : review.getContent());
        version.setTags(dto.getTags() != null ? joinTags(dto.getTags()) : review.getTags());
        reviewVersionRepository.save(version);

        // 更新当前评价
        if (dto.getRating() != null) review.setRating(dto.getRating());
        if (dto.getContent() != null) review.setContent(stripHtml(dto.getContent()));
        if (dto.getTags() != null) review.setTags(joinTags(dto.getTags()));
        review.setCurrentVersion(newVersion);
        reviewRepository.save(review);

        // 重算评分
        recalculateRating(review.getStoreId());

        log.info("用户 {} 修改评价: reviewId={}, newVersion={}", userId, reviewId, newVersion);

        return toReviewResponse(review);
    }

    /**
     * 删除评价（软删除）。
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("评价不存在"));

        if (!review.getUserId().equals(userId)) {
            throw new SecurityException("无权删除他人评价");
        }

        review.setStatus(ReviewStatus.DELETED);
        review.setVisible(false);
        reviewRepository.save(review);

        // 重算评分
        recalculateRating(review.getStoreId());

        log.info("用户 {} 删除评价: reviewId={}", userId, reviewId);
    }

    /**
     * 查询我的评价。
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReviewResponse> getMyReviews(Long userId, int page, int size) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId,
                        org.springframework.data.domain.PageRequest.of(page, size))
                .map(this::toReviewResponse);
    }

    /**
     * 重算门店评分聚合（委托给贝叶斯评分服务异步执行）。
     */
    private void recalculateRating(Long storeId) {
        try {
            ratingAggregateService.recalculate(storeId);
            log.debug("已触发门店 {} 评分重算", storeId);
        } catch (Exception e) {
            log.error("触发重算评分聚合失败: storeId={}, error={}", storeId, e.getMessage());
        }
    }

    private ReviewResponse toReviewResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setOrderId(review.getOrderId());
        response.setStoreId(review.getStoreId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setTags(review.getTags() != null ? review.getTags().split(",") : null);
        response.setStatus(review.getStatus().name());
        response.setVersion(review.getCurrentVersion());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        // 附加商家回复
        merchantReplyRepository.findByReviewId(review.getId()).ifPresent(reply -> {
            if ("ACTIVE".equals(reply.getStatus())) {
                response.setMerchantReply(reply.getContent());
            }
        });

        return response;
    }

    private String stripHtml(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }

    private String joinTags(String[] tags) {
        if (tags == null || tags.length == 0) return null;
        return String.join(",", tags);
    }
}
