package com.campusfruit.review.service;

import com.campusfruit.review.entity.MerchantReply;
import com.campusfruit.review.entity.Review;
import com.campusfruit.review.repository.MerchantReplyRepository;
import com.campusfruit.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商家回复服务。商家对评价的回复管理。
 */
@Service
public class MerchantReplyService {

    private static final Logger log = LoggerFactory.getLogger(MerchantReplyService.class);

    private final MerchantReplyRepository replyRepository;
    private final ReviewRepository reviewRepository;

    public MerchantReplyService(MerchantReplyRepository replyRepository,
                                 ReviewRepository reviewRepository) {
        this.replyRepository = replyRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * 添加商家回复。
     */
    @Transactional
    public MerchantReply addReply(Long merchantId, Long storeId, Long reviewId, String content) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("评价不存在"));

        if (!review.getStoreId().equals(storeId)) {
            throw new SecurityException("无权回复非本门店评价");
        }

        // 检查是否已有回复
        replyRepository.findByReviewId(reviewId).ifPresent(existing -> {
            throw new IllegalStateException("已回复过该评价，请使用修改接口");
        });

        MerchantReply reply = new MerchantReply();
        reply.setReviewId(reviewId);
        reply.setMerchantId(merchantId);
        reply.setStoreId(storeId);
        reply.setContent(stripHtml(content));
        reply.setStatus("ACTIVE");
        reply = replyRepository.save(reply);

        log.info("商家 {} 回复评价: reviewId={}", merchantId, reviewId);
        return reply;
    }

    /**
     * 修改商家回复。
     */
    @Transactional
    public MerchantReply updateReply(Long merchantId, Long storeId, Long reviewId, String content) {
        MerchantReply reply = replyRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("回复不存在"));

        if (!reply.getMerchantId().equals(merchantId) || !reply.getStoreId().equals(storeId)) {
            throw new SecurityException("无权修改他人回复");
        }

        reply.setContent(stripHtml(content));
        reply = replyRepository.save(reply);

        log.info("商家 {} 修改回复: reviewId={}", merchantId, reviewId);
        return reply;
    }

    /**
     * 删除商家回复（软删除）。
     */
    @Transactional
    public void deleteReply(Long merchantId, Long storeId, Long reviewId) {
        MerchantReply reply = replyRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("回复不存在"));

        if (!reply.getMerchantId().equals(merchantId) || !reply.getStoreId().equals(storeId)) {
            throw new SecurityException("无权删除他人回复");
        }

        reply.setStatus("DELETED");
        replyRepository.save(reply);

        log.info("商家 {} 删除回复: reviewId={}", merchantId, reviewId);
    }

    private String stripHtml(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
