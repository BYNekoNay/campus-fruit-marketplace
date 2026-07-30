package com.campusfruit.review;

import com.campusfruit.review.dto.SubmitReviewRequest;
import com.campusfruit.review.dto.UpdateReviewRequest;
import com.campusfruit.review.entity.ReviewEligibility;
import com.campusfruit.review.enums.ReviewStatus;
import com.campusfruit.review.repository.ReviewEligibilityRepository;
import com.campusfruit.review.repository.ReviewRepository;
import com.campusfruit.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 评价资格校验、提交、修改、重复提交拦截 集成测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class VerifiedReviewIT {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewEligibilityRepository eligibilityRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_STORE_ID = 2001L;
    private static final Long TEST_ORDER_ID = 3001L;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        eligibilityRepository.deleteAll();
    }

    @Test
    void shouldRejectReviewWhenNoEligibility() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(TEST_ORDER_ID);
        request.setStoreId(TEST_STORE_ID);
        request.setRating(5);
        request.setContent("很好吃");

        SecurityException ex = assertThrows(SecurityException.class,
                () -> reviewService.submitReview(TEST_USER_ID, request));

        assertTrue(ex.getMessage().contains("无评价资格"));
    }

    @Test
    void shouldSubmitReviewSuccessfully() {
        // 创建评价资格
        createEligibility();

        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(TEST_ORDER_ID);
        request.setStoreId(TEST_STORE_ID);
        request.setRating(4);
        request.setContent("不错的水果");
        request.setTags(new String[]{"新鲜", "实惠"});

        var response = reviewService.submitReview(TEST_USER_ID, request);

        assertNotNull(response.getId());
        assertEquals(4, response.getRating());
        assertEquals("不错的水果", response.getContent());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(1, response.getVersion());

        // 验证资格已标记为 used
        var elig = eligibilityRepository
                .findByUserIdAndStoreIdAndOrderIdAndUsedFalseAndTombstoneFalse(
                        TEST_USER_ID, TEST_STORE_ID, TEST_ORDER_ID);
        assertTrue(elig.isEmpty(), "评价资格应已被使用");
    }

    @Test
    void shouldRejectDuplicateReview() {
        createEligibility();

        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(TEST_ORDER_ID);
        request.setStoreId(TEST_STORE_ID);
        request.setRating(5);
        request.setContent("第一次评价");

        reviewService.submitReview(TEST_USER_ID, request);

        // 尝试重复提交
        // 需要重新创建资格（因为第一次已消费）
        // 实际不应该能重复提交同一订单
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reviewService.submitReview(TEST_USER_ID, request));

        assertTrue(ex.getMessage().contains("已评价"));
    }

    @Test
    void shouldUpdateReviewOnce() {
        createEligibility();

        SubmitReviewRequest submit = new SubmitReviewRequest();
        submit.setOrderId(TEST_ORDER_ID);
        submit.setStoreId(TEST_STORE_ID);
        submit.setRating(3);
        submit.setContent("一般般");

        var review = reviewService.submitReview(TEST_USER_ID, submit);

        // 第一次修改
        UpdateReviewRequest update = new UpdateReviewRequest();
        update.setRating(4);
        update.setContent("第二次吃觉得还不错了");

        var updated = reviewService.updateReview(TEST_USER_ID, review.getId(), update);

        assertEquals(4, updated.getRating());
        assertEquals("第二次吃觉得还不错了", updated.getContent());
        assertEquals(2, updated.getVersion());
    }

    @Test
    void shouldRejectSecondUpdate() {
        createEligibility();

        SubmitReviewRequest submit = new SubmitReviewRequest();
        submit.setOrderId(TEST_ORDER_ID);
        submit.setStoreId(TEST_STORE_ID);
        submit.setRating(4);
        submit.setContent("第一次");

        var review = reviewService.submitReview(TEST_USER_ID, submit);

        // 第一次修改
        UpdateReviewRequest update1 = new UpdateReviewRequest();
        update1.setRating(5);
        update1.setContent("修改了");
        reviewService.updateReview(TEST_USER_ID, review.getId(), update1);

        // 第二次修改应该被拒绝
        UpdateReviewRequest update2 = new UpdateReviewRequest();
        update2.setRating(3);
        update2.setContent("又想改");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reviewService.updateReview(TEST_USER_ID, review.getId(), update2));

        assertTrue(ex.getMessage().contains("只能修改一次"));
    }

    @Test
    void shouldDeleteReview() {
        createEligibility();

        SubmitReviewRequest submit = new SubmitReviewRequest();
        submit.setOrderId(TEST_ORDER_ID);
        submit.setStoreId(TEST_STORE_ID);
        submit.setRating(5);
        submit.setContent("很好的体验");

        var review = reviewService.submitReview(TEST_USER_ID, submit);

        reviewService.deleteReview(TEST_USER_ID, review.getId());

        var deleted = reviewRepository.findById(review.getId()).orElseThrow();
        assertEquals(ReviewStatus.DELETED, deleted.getStatus());
        assertFalse(deleted.getVisible());
    }

    private void createEligibility() {
        ReviewEligibility eligibility = new ReviewEligibility();
        eligibility.setUserId(TEST_USER_ID);
        eligibility.setStoreId(TEST_STORE_ID);
        eligibility.setOrderId(TEST_ORDER_ID);
        eligibility.setUsed(false);
        eligibility.setTombstone(false);
        eligibilityRepository.save(eligibility);
    }
}
