package com.campusfruit.review;

import com.campusfruit.review.entity.Review;
import com.campusfruit.review.entity.ReviewEligibility;
import com.campusfruit.review.entity.ReviewReport;
import com.campusfruit.review.entity.RatingAggregate;
import com.campusfruit.review.enums.ReportStatus;
import com.campusfruit.review.enums.ReviewStatus;
import com.campusfruit.review.moderation.AuditLog;
import com.campusfruit.review.moderation.AuditLogRepository;
import com.campusfruit.review.moderation.ReportService;
import com.campusfruit.review.repository.RatingAggregateRepository;
import com.campusfruit.review.repository.ReviewEligibilityRepository;
import com.campusfruit.review.repository.ReviewReportRepository;
import com.campusfruit.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 举报审核系统集成测试。
 * <p>
 * 场景：
 * 1. 用户举报评价
 * 2. 管理员采纳举报 → 评价隐藏 + 评分重算
 * 3. 管理员驳回举报 → 评价保持
 * 4. 管理员恢复被隐藏评价 → 评价重新可见 + 评分重算
 * 5. 版本号条件更新防重复审核
 */
@SpringBootTest
@ActiveProfiles("test")
class ReviewModerationIT {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReviewReportRepository reportRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewEligibilityRepository eligibilityRepository;

    @Autowired
    private RatingAggregateRepository ratingAggregateRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final Long REPORTER_ID = 30001L;
    private static final Long REVIEWER_ID = 30002L;
    private static final Long ANOTHER_ADMIN_ID = 30003L;
    private static final Long USER_ID = 31001L;
    private static final Long STORE_ID = 32001L;

    private Long reviewId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        reportRepository.deleteAll();
        reviewRepository.deleteAll();
        eligibilityRepository.deleteAll();
        ratingAggregateRepository.deleteAll();

        // 创建一条已发布的评价
        Review review = new Review();
        review.setUserId(USER_ID);
        review.setStoreId(STORE_ID);
        review.setOrderId(55001L);
        review.setRating(2);
        review.setContent("水果不新鲜，太失望了");
        review.setStatus(ReviewStatus.ACTIVE);
        review.setVisible(true);
        review.setCurrentVersion(1);
        review = reviewRepository.save(review);
        reviewId = review.getId();

        // 创建评分聚合初始记录
        RatingAggregate aggregate = new RatingAggregate();
        aggregate.setStoreId(STORE_ID);
        aggregate.setAvgRating(new BigDecimal("2.00"));
        aggregate.setBayesianRating(new BigDecimal("2.00"));
        aggregate.setTotalRatings(1);
        aggregate.setRatingDistribution("{\"1\":0,\"2\":1,\"3\":0,\"4\":0,\"5\":0}");
        aggregate.setVersion(1);
        ratingAggregateRepository.save(aggregate);
    }

    /**
     * 完整流程：举报 → 采纳 → 评价隐藏 → 恢复 → 评分重算。
     */
    @Test
    void shouldCompleteModerationLifecycle() {
        // --- 1. 用户举报 ---
        ReviewReport report = reportService.submitReport(REPORTER_ID, reviewId, "评价虚假，从未购买过");
        assertNotNull(report.getId(), "举报单应创建成功");
        assertEquals(ReportStatus.PENDING, report.getStatus(), "初始状态应为 PENDING");
        assertEquals(REPORTER_ID, report.getReporterId());
        assertEquals(reviewId, report.getReviewId());

        // --- 2. 管理员采纳举报 ---
        reportService.reviewReport(report.getId(), REVIEWER_ID, "ACCEPTED", "经核实，确实为虚假评价");
        report = reportRepository.findById(report.getId()).orElseThrow();
        assertEquals(ReportStatus.ACCEPTED, report.getStatus(), "举报应已采纳");
        assertEquals(REVIEWER_ID, report.getReviewerId());

        Review review = reviewRepository.findById(reviewId).orElseThrow();
        assertEquals(ReviewStatus.HIDDEN, review.getStatus(), "评价应已隐藏");
        assertFalse(review.getVisible(), "评价应不可见");

        // 验证评分聚合已重算
        RatingAggregate aggregate = ratingAggregateRepository.findByStoreId(STORE_ID).orElseThrow();
        assertEquals(2, aggregate.getVersion(), "版本号应已递增");

        // --- 3. 恢复评价 ---
        reportService.restoreReview(ANOTHER_ADMIN_ID, reviewId);
        review = reviewRepository.findById(reviewId).orElseThrow();
        assertEquals(ReviewStatus.ACTIVE, review.getStatus(), "评价应已恢复");
        assertTrue(review.getVisible(), "评价应重新可见");

        // 验证再次重算评分
        aggregate = ratingAggregateRepository.findByStoreId(STORE_ID).orElseThrow();
        assertEquals(3, aggregate.getVersion(), "恢复后版本号应再次递增");

        // --- 4. 验证审核日志完整 ---
        var logs = auditLogRepository.findByReviewIdOrderByCreatedAtDesc(reviewId);
        assertTrue(logs.size() >= 2, "至少应有举报采纳和恢复两条审计日志");
        boolean hasAcceptLog = logs.stream().anyMatch(l -> "REPORT_ACCEPT".equals(l.getActionType()));
        boolean hasRestoreLog = logs.stream().anyMatch(l -> "RESTORE_REVIEW".equals(l.getActionType()));
        assertTrue(hasAcceptLog, "应有采纳审计日志");
        assertTrue(hasRestoreLog, "应有恢复审计日志");
    }

    /**
     * 举报驳回：评价保持原状态不变。
     */
    @Test
    void shouldDismissReportAndKeepReviewActive() {
        ReviewReport report = reportService.submitReport(REPORTER_ID, reviewId, "误点举报");

        reportService.reviewReport(report.getId(), REVIEWER_ID, "DISMISSED", "评价内容正常，驳回举报");

        report = reportRepository.findById(report.getId()).orElseThrow();
        assertEquals(ReportStatus.DISMISSED, report.getStatus(), "举报应已驳回");

        Review review = reviewRepository.findById(reviewId).orElseThrow();
        assertEquals(ReviewStatus.ACTIVE, review.getStatus(), "评价应保持 ACTIVE");
        assertTrue(review.getVisible(), "评价应保持可见");
    }

    /**
     * 举报已被处理后不允许重复审核。
     */
    @Test
    void shouldPreventDuplicateReview() {
        ReviewReport report = reportService.submitReport(REPORTER_ID, reviewId, "内容有误");

        // 第一次审核通过
        reportService.reviewReport(report.getId(), REVIEWER_ID, "ACCEPTED", "已处理");

        // 第二次审核应失败
        assertThrows(IllegalStateException.class, () ->
                reportService.reviewReport(report.getId(), ANOTHER_ADMIN_ID, "DISMISSED", "想撤回"),
                "已处理的举报不允许重复审核");
    }

    /**
     * 管理员直接隐藏评价（绕过举报流程）。
     */
    @Test
    void shouldHideReviewDirectly() {
        reportService.hideReview(REVIEWER_ID, reviewId, "商家投诉该评价不实");
        Review review = reviewRepository.findById(reviewId).orElseThrow();
        assertEquals(ReviewStatus.HIDDEN, review.getStatus());
        assertFalse(review.getVisible());

        RatingAggregate aggregate = ratingAggregateRepository.findByStoreId(STORE_ID).orElseThrow();
        assertEquals(2, aggregate.getVersion(), "隐藏后评分应重算");
    }

    /**
     * 不能恢复 ACTIVE 状态的评价。
     */
    @Test
    void shouldRejectRestoreActiveReview() {
        assertThrows(IllegalArgumentException.class,
                () -> reportService.restoreReview(REVIEWER_ID, reviewId),
                "不能恢复非隐藏的评价");
    }

    /**
     * 不允许举报已删除的评价。
     */
    @Test
    void shouldRejectReportOnDeletedReview() {
        Review review = reviewRepository.findById(reviewId).orElseThrow();
        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);

        assertThrows(IllegalArgumentException.class,
                () -> reportService.submitReport(REPORTER_ID, reviewId, "已删除"),
                "不能举报已删除的评价");
    }
}
