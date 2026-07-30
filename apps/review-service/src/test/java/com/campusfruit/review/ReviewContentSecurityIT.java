package com.campusfruit.review;

import com.campusfruit.review.dto.SubmitReviewRequest;
import com.campusfruit.review.entity.Review;
import com.campusfruit.review.entity.ReviewEligibility;
import com.campusfruit.review.enums.ReviewStatus;
import com.campusfruit.review.repository.ReviewEligibilityRepository;
import com.campusfruit.review.repository.ReviewRepository;
import com.campusfruit.review.risk.RiskRuleEngine;
import com.campusfruit.review.risk.RiskRuleEngine.RiskLevel;
import com.campusfruit.review.risk.RiskRuleEngine.RiskResult;
import com.campusfruit.review.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 评价内容安全与风控集成测试。
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>XSS 注入拦截（HTML 标签被剥离）</li>
 *   <li>超长内容处理</li>
 *   <li>风控频率检测（SUSPICIOUS_FREQUENCY）</li>
 *   <li>重复内容检测（DUPLICATE_CONTENT）</li>
 *   <li>评价激增检测（REVIEW_SPIKE）</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class ReviewContentSecurityIT {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewEligibilityRepository eligibilityRepository;

    @Autowired
    private RiskRuleEngine riskRuleEngine;

    private static final Long USER_ID = 41001L;
    private static final Long USER_ID_2 = 41002L;
    private static final Long STORE_ID = 42001L;
    private static final Long ORDER_ID_BASE = 56001L;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        eligibilityRepository.deleteAll();
    }

    /**
     * XSS 注入：HTML 标签应被剔除。
     */
    @Test
    void shouldStripHtmlTagsFromContent() {
        createEligibility(USER_ID, STORE_ID, ORDER_ID_BASE);

        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(ORDER_ID_BASE);
        request.setStoreId(STORE_ID);
        request.setRating(4);
        request.setContent("<script>alert('xss')</script>水果很好<b>推荐</b>");

        var response = reviewService.submitReview(USER_ID, request);

        String content = response.getContent();
        assertFalse(content.contains("<script>"), "script 标签应被剥离");
        assertFalse(content.contains("<b>"), "b 标签应被剥离");
        assertFalse(content.contains("alert"), "脚本内容应被剥离");
        assertTrue(content.contains("水果很好"), "正常文本应保留");
        assertTrue(content.contains("推荐"), "b 标签内文本应保留");

        // 验证持久化
        Review saved = reviewRepository.findById(response.getId()).orElseThrow();
        assertEquals("alert('xss')水果很好推荐", saved.getContent(),
                "HTML 标签应被剥离，仅保留文本内容");
    }

    /**
     * 超长内容：验证长文本评价可以正常存储。
     */
    @Test
    void shouldHandleLongContent() {
        createEligibility(USER_ID, STORE_ID, ORDER_ID_BASE);

        StringBuilder sb = new StringBuilder();
        sb.append("这是一段详细评价。");
        for (int i = 0; i < 30; i++) {
            sb.append("第").append(i + 1).append("次购买的水果质量都很好，发货速度快，包装完整。");
        }
        String longContent = sb.toString();

        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(ORDER_ID_BASE);
        request.setStoreId(STORE_ID);
        request.setRating(5);
        request.setContent(longContent);

        var response = reviewService.submitReview(USER_ID, request);

        assertNotNull(response.getId());
        assertTrue(response.getContent().length() > 100, "长文本应完整保留");
        // 验证首尾内容完整
        assertTrue(response.getContent().startsWith("这是一段详细评价。"));
        assertTrue(response.getContent().contains("第30次购买"));

        // 验证持久化完整性
        Review saved = reviewRepository.findById(response.getId()).orElseThrow();
        assertEquals(longContent, saved.getContent(), "持久化的内容应完整");
    }

    /**
     * 风控：SUSPICIOUS_FREQUENCY — 短时间内大量评价触发频率检测。
     */
    @Test
    void shouldDetectSuspiciousFrequency() {
        // 快速创建多条评价（模拟 10 分钟内 > 5 条）
        int reviewCount = 6;
        for (int i = 0; i < reviewCount; i++) {
            Review review = new Review();
            review.setUserId(USER_ID);
            review.setStoreId(STORE_ID);
            review.setOrderId(58000L + i);
            review.setRating(5);
            review.setContent("非常好 " + i);
            review.setStatus(ReviewStatus.ACTIVE);
            review.setVisible(true);
            review.setCurrentVersion(1);
            // 设置为刚刚创建
            reviewRepository.save(review);
        }

        // 检查最后一条评价的风险
        Long lastReviewId = reviewRepository.findByOrderId(58000L + (reviewCount - 1))
                .orElseThrow()
                .getId();

        RiskResult result = riskRuleEngine.evaluate(lastReviewId);

        assertNotNull(result);
        assertEquals(RiskLevel.HIGH, result.getRiskLevel(),
                "高频发布应触发 HIGH 风控级别");
        assertTrue(result.getSignals().stream()
                .anyMatch(s -> "SUSPICIOUS_FREQUENCY".equals(s.getSignalType())),
                "应包含 SUSPICIOUS_FREQUENCY 信号");
    }

    /**
     * 风控：DUPLICATE_CONTENT — 完全相同的内容触发重复检测。
     */
    @Test
    void shouldDetectDuplicateContent() {
        String duplicateText = "非常好吃，强烈推荐！已多次购买，品质始终如一。";

        // 先创建第一条评价
        Review r1 = new Review();
        r1.setUserId(USER_ID);
        r1.setStoreId(STORE_ID);
        r1.setOrderId(59001L);
        r1.setRating(5);
        r1.setContent(duplicateText);
        r1.setStatus(ReviewStatus.ACTIVE);
        r1.setVisible(true);
        r1.setCurrentVersion(1);
        reviewRepository.save(r1);

        // 创建第二条相同内容评价
        Review r2 = new Review();
        r2.setUserId(USER_ID);
        r2.setStoreId(STORE_ID + 1); // 不同门店
        r2.setOrderId(59002L);
        r2.setRating(5);
        r2.setContent(duplicateText); // 完全相同
        r2.setStatus(ReviewStatus.ACTIVE);
        r2.setVisible(true);
        r2.setCurrentVersion(1);
        r2 = reviewRepository.save(r2);

        RiskResult result = riskRuleEngine.evaluate(r2.getId());

        assertTrue(result.getSignals().stream()
                .anyMatch(s -> "DUPLICATE_CONTENT".equals(s.getSignalType())),
                "应检测到重复内容信号");
        assertEquals(RiskLevel.HIGH, result.getRiskLevel(),
                "DUPLICATE_CONTENT 应触发 HIGH 级别");
    }

    /**
     * 风控：REVIEW_SPIKE — 门店评价激增检测。
     */
    @Test
    void shouldDetectReviewSpike() {
        // 为同一门店快速创建 10+ 条评价
        int spikeCount = 11;
        for (int i = 0; i < spikeCount; i++) {
            Review review = new Review();
            review.setUserId(USER_ID + i);
            review.setStoreId(STORE_ID);
            review.setOrderId(60000L + i);
            review.setRating(i % 5 + 1);
            review.setContent("评价内容 " + i);
            review.setStatus(ReviewStatus.ACTIVE);
            review.setVisible(true);
            review.setCurrentVersion(1);
            reviewRepository.save(review);
        }

        Long lastReviewId = reviewRepository.findByOrderId(60000L + (spikeCount - 1))
                .orElseThrow()
                .getId();

        RiskResult result = riskRuleEngine.evaluate(lastReviewId);

        assertFalse(result.getSignals().isEmpty(), "应检测到风控信号");
        assertTrue(result.getSignals().stream()
                .anyMatch(s -> "REVIEW_SPIKE".equals(s.getSignalType())),
                "应包含 REVIEW_SPIKE 信号");
    }

    /**
     * 正常评价不应触发任何风控信号。
     */
    @Test
    void shouldNotTriggerRiskForNormalReview() {
        Review review = new Review();
        review.setUserId(USER_ID);
        review.setStoreId(STORE_ID);
        review.setOrderId(61001L);
        review.setRating(4);
        review.setContent("正常评价内容，质量不错");
        review.setStatus(ReviewStatus.ACTIVE);
        review.setVisible(true);
        review.setCurrentVersion(1);
        review = reviewRepository.save(review);

        RiskResult result = riskRuleEngine.evaluate(review.getId());

        assertEquals(RiskLevel.SAFE, result.getRiskLevel(),
                "正常评价应为 SAFE 级别");
        assertTrue(result.getSignals().isEmpty(), "正常评价不应有风控信号");
    }

    /**
     * 超长 EMOJI 和特殊字符混合内容。
     */
    @Test
    void shouldHandleEmojiAndSpecialChars() {
        createEligibility(USER_ID, STORE_ID, ORDER_ID_BASE);

        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setOrderId(ORDER_ID_BASE);
        request.setStoreId(STORE_ID);
        request.setRating(5);
        request.setContent("水果质量非常好！\uD83C\uDF4E\uD83C\uDF4A\uD83C\uDF4B 物流也很快，下次还会再买！\u2600\uFE0F");

        var response = reviewService.submitReview(USER_ID, request);

        assertNotNull(response.getId());
        assertTrue(response.getContent().contains("\uD83C\uDF4E"), "Emoji 应保留");
        assertTrue(response.getContent().contains("\u2600\uFE0F"), "特殊字符应保留");
    }

    private void createEligibility(Long userId, Long storeId, Long orderId) {
        ReviewEligibility eligibility = new ReviewEligibility();
        eligibility.setUserId(userId);
        eligibility.setStoreId(storeId);
        eligibility.setOrderId(orderId);
        eligibility.setOrderCompletedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        eligibility.setUsed(false);
        eligibility.setTombstone(false);
        eligibilityRepository.save(eligibility);
    }
}
