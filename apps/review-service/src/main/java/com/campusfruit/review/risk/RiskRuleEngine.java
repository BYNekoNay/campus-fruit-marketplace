package com.campusfruit.review.risk;

import com.campusfruit.review.entity.Review;
import com.campusfruit.review.entity.ReviewEligibility;
import com.campusfruit.review.repository.ReviewEligibilityRepository;
import com.campusfruit.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 风控规则引擎。
 * <p>
 * 评估评价风险，执行隔离或降权策略。
 * 高风险处置（隐藏）需要人工复核，引擎仅做标记隔离。
 * <p>
 * 首版风险信号：
 * <ul>
 *   <li>SUSPICIOUS_FREQUENCY — 同设备短时间内大量评价</li>
 *   <li>SUSPICIOUS_TIMING — 订单完成后异常短时间评价</li>
 *   <li>DUPLICATE_CONTENT — 评价文本与用户其他评价高度重复</li>
 *   <li>REVIEW_SPIKE — 门店短时间内评价激增</li>
 * </ul>
 */
@Service
public class RiskRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskRuleEngine.class);

    private final ReviewRepository reviewRepository;
    private final ReviewEligibilityRepository eligibilityRepository;
    private final RiskRuleConfig config;

    public RiskRuleEngine(ReviewRepository reviewRepository,
                           ReviewEligibilityRepository eligibilityRepository,
                           RiskRuleConfig config) {
        this.reviewRepository = reviewRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.config = config;
    }

    /**
     * 评估指定评价的风险。
     *
     * @param reviewId 评价 ID
     * @return 风险信号列表，空列表表示无风险
     */
    @Transactional(readOnly = true)
    public RiskResult evaluate(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("评价不存在"));

        List<RiskSignal> signals = new ArrayList<>();

        // 1. 检测刷评频率
        if (detectSuspiciousFrequency(review.getUserId())) {
            signals.add(new RiskSignal("SUSPICIOUS_FREQUENCY", "用户在短时间内发布了过多评价"));
        }

        // 2. 检测异常快速评价
        if (detectSuspiciousTiming(review.getUserId(), review.getStoreId(), review.getOrderId())) {
            signals.add(new RiskSignal("SUSPICIOUS_TIMING", "订单完成后评价时间异常短"));
        }

        // 3. 检测重复内容
        if (detectDuplicateContent(review.getUserId(), review.getContent())) {
            signals.add(new RiskSignal("DUPLICATE_CONTENT", "评价文本与用户历史评价高度重复"));
        }

        // 4. 检测评价激增
        if (detectReviewSpike(review.getStoreId())) {
            signals.add(new RiskSignal("REVIEW_SPIKE", "门店评价在短时间内激增"));
        }

        RiskResult result = new RiskResult();
        result.setReviewId(reviewId);
        result.setRiskLevel(signals.isEmpty() ? RiskLevel.SAFE : determineRiskLevel(signals));
        result.setSignals(signals);
        result.setEvaluatedAt(Instant.now());

        if (!signals.isEmpty()) {
            log.warn("评价 {} 命中风险信号: {}", reviewId, signals);
        }

        return result;
    }

    /**
     * 检测同用户短时间内大量评价。
     */
    private boolean detectSuspiciousFrequency(Long userId) {
        Instant since = Instant.now().minus(config.getSuspiciousWindowMinutes(), ChronoUnit.MINUTES);
        Long count = reviewRepository.countByUserIdAndCreatedAtAfter(userId, since);
        boolean suspicious = count >= config.getSuspiciousMaxReviews();
        if (suspicious) {
            log.debug("用户 {} 最近 {} 分钟内评价 {} 次，触发 SUSPICIOUS_FREQUENCY",
                    userId, config.getSuspiciousWindowMinutes(), count);
        }
        return suspicious;
    }

    /**
     * 检测订单完成后评价时间异常短。
     */
    private boolean detectSuspiciousTiming(Long userId, Long storeId, Long orderId) {
        Optional<ReviewEligibility> eligibility =
                eligibilityRepository.findByUserIdAndStoreIdAndOrderIdAndUsedFalseAndTombstoneFalse(
                        userId, storeId, orderId);
        // 如果资格已被消费，查找已消费的资格
        if (eligibility.isEmpty()) {
            eligibility = eligibilityRepository.findByUserIdAndStoreIdAndOrderId(userId, storeId, orderId);
        }

        if (eligibility.isPresent() && eligibility.get().getOrderCompletedAt() != null) {
            Instant completedAt = eligibility.get().getOrderCompletedAt();
            long secondsSinceCompletion = ChronoUnit.SECONDS.between(completedAt, Instant.now());
            boolean suspicious = secondsSinceCompletion < config.getMinReviewTimeSeconds();
            if (suspicious) {
                log.debug("订单 {} 完成后仅 {} 秒即评价，触发 SUSPICIOUS_TIMING",
                        orderId, secondsSinceCompletion);
            }
            return suspicious;
        }
        return false;
    }

    /**
     * 检测评价文本与用户历史评价重复。
     */
    private boolean detectDuplicateContent(Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        List<Review> userReviews = reviewRepository.findByUserIdAndContent(userId, content);
        boolean suspicious = userReviews.size() > 1;
        if (suspicious) {
            log.debug("用户 {} 的评价内容与历史评价重复，触发 DUPLICATE_CONTENT", userId);
        }
        return suspicious;
    }

    /**
     * 检测门店评价激增。
     */
    private boolean detectReviewSpike(Long storeId) {
        Instant since = Instant.now().minus(config.getSpikeWindowMinutes(), ChronoUnit.MINUTES);
        Long count = reviewRepository.countByStoreIdAndCreatedAtAfter(storeId, since);
        boolean suspicious = count >= config.getSpikeThreshold();
        if (suspicious) {
            log.debug("门店 {} 最近 {} 分钟内新增 {} 条评价，触发 REVIEW_SPIKE",
                    storeId, config.getSpikeWindowMinutes(), count);
        }
        return suspicious;
    }

    /**
     * 根据信号确定风险等级。
     */
    private RiskLevel determineRiskLevel(List<RiskSignal> signals) {
        // DUPLICATE_CONTENT 或 SUSPICIOUS_FREQUENCY → HIGH
        boolean hasHighSignal = signals.stream()
                .anyMatch(s -> "DUPLICATE_CONTENT".equals(s.getSignalType())
                        || "SUSPICIOUS_FREQUENCY".equals(s.getSignalType()));
        if (hasHighSignal) {
            return RiskLevel.HIGH;
        }
        // 其他 → MEDIUM
        return RiskLevel.MEDIUM;
    }

    // --- 内部类 ---

    public enum RiskLevel {
        SAFE,    // 无风险
        MEDIUM,  // 中等风险（降权，不进入评分计算）
        HIGH     // 高风险（自动隔离，需人工复核）
    }

    public static class RiskSignal {
        private String signalType;
        private String description;

        public RiskSignal() {}
        public RiskSignal(String signalType, String description) {
            this.signalType = signalType;
            this.description = description;
        }

        public String getSignalType() { return signalType; }
        public void setSignalType(String signalType) { this.signalType = signalType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        @Override
        public String toString() {
            return signalType + ": " + description;
        }
    }

    public static class RiskResult {
        private Long reviewId;
        private RiskLevel riskLevel;
        private List<RiskSignal> signals;
        private Instant evaluatedAt;

        public Long getReviewId() { return reviewId; }
        public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
        public RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
        public List<RiskSignal> getSignals() { return signals; }
        public void setSignals(List<RiskSignal> signals) { this.signals = signals; }
        public Instant getEvaluatedAt() { return evaluatedAt; }
        public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    }
}
