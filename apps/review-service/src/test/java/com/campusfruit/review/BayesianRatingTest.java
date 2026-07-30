package com.campusfruit.review;

import com.campusfruit.review.entity.Review;
import com.campusfruit.review.entity.RatingAggregate;
import com.campusfruit.review.enums.ReviewStatus;
import com.campusfruit.review.repository.RatingAggregateRepository;
import com.campusfruit.review.repository.ReviewRepository;
import com.campusfruit.review.scoring.BayesianRatingCalculator;
import com.campusfruit.review.scoring.BayesianScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 贝叶斯评分计算器纯单元测试（不需数据库/Docker）。
 * <p>
 * 贝叶斯公式：bayesianScore = (v/(v+m)) * R + (m/(v+m)) * C
 * <ul>
 *   <li>v = 有效评价数</li>
 *   <li>R = 算术平均分</li>
 *   <li>C = 全局平均分（先验值，默认 3.5）</li>
 *   <li>m = 最小样本数（默认 5）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BayesianRatingTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RatingAggregateRepository ratingAggregateRepository;

    private BayesianRatingCalculator calculator;

    private static final Long STORE_ID = 10001L;
    private static final Long USER_ID = 20001L;

    @BeforeEach
    void setUp() {
        calculator = new BayesianRatingCalculator(reviewRepository, ratingAggregateRepository);
        // 默认全局先验值：无数据时 C=3.5
        when(ratingAggregateRepository.findAll()).thenReturn(Collections.emptyList());
    }

    /**
     * 1条五星评价：贝叶斯修正后分数应向先验值 3.5 回缩。
     * v=1, R=5.0, m=5, C=3.5
     * expected = (1/6)*5.0 + (5/6)*3.5 = 0.833 + 2.917 = 3.75
     */
    @Test
    void shouldApplyBayesianCorrectionForSingleFiveStarReview() {
        mockReviews(STORE_ID, List.of(createReview(5)));

        BayesianScore score = calculator.calculate(STORE_ID);

        assertEquals(1, score.getTotalRatings(), "v 应为 1");
        assertEquals(new BigDecimal("5.00"), score.getAvgRating(), "原始平均分应为 5.0");
        assertEquals(new BigDecimal("3.64"), score.getBayesianScore(), "贝叶斯修正后应为 3.64(1/(1+10))*5+(10/11)*3.5=3.64");
        assertTrue(score.getConfidence() < 0.2, "1条评价置信度应很低");
    }

    /**
     * v=0 时：使用先验值 C=3.5。
     */
    @Test
    void shouldUsePriorValueWhenNoReviews() {
        mockReviews(STORE_ID, Collections.emptyList());

        BayesianScore score = calculator.calculate(STORE_ID);

        assertEquals(0, score.getTotalRatings(), "无评价时 v 应为 0");
        assertEquals(new BigDecimal("0.00"), score.getAvgRating(), "无评价时算术平均分为 0");
        assertEquals(new BigDecimal("3.50"), score.getBayesianScore(), "无评价时贝叶斯分应为先验值 3.5");
        assertEquals(0.0, score.getConfidence(), "无评价时置信度应为 0");
    }

    /**
     * 大样本：100条评价，贝叶斯评分趋近真实算术平均值。
     * (100/110)*4.0 + (10/110)*3.5 = 3.64 + 0.32 = 3.95
     */
    @Test
    void shouldApproachTrueMeanWithLargeSample() {
        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            reviews.add(createReview(i < 50 ? 3 : 5)); // 50条3星 + 50条5星 = 均值4.0
        }
        mockReviews(STORE_ID, reviews);

        BayesianScore score = calculator.calculate(STORE_ID);

        assertEquals(100, score.getTotalRatings(), "总评价数应为 100");
        BigDecimal expected = new BigDecimal("3.95");
        assertTrue(score.getBayesianScore().compareTo(expected) >= 0,
                "大样本贝叶斯分应≥3.95，实际: " + score.getBayesianScore());
        assertTrue(score.getConfidence() >= 0.9, "大样本置信度应≥0.9，实际: " + score.getConfidence());
    }

    /**
     * 混合评分分布验证。
     * 3条2星 + 4条3星 + 3条4星 = 均值3.0，v=10。
     * (10/20)*3.0 + (10/20)*3.5 = 1.5 + 1.75 = 3.25
     */
    @Test
    void shouldCalculateCorrectDistribution() {
        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < 3; i++) reviews.add(createReview(2));
        for (int i = 0; i < 4; i++) reviews.add(createReview(3));
        for (int i = 0; i < 3; i++) reviews.add(createReview(4));
        mockReviews(STORE_ID, reviews);

        BayesianScore score = calculator.calculate(STORE_ID);

        assertEquals(10, score.getTotalRatings());
        assertEquals(new BigDecimal("3.00"), score.getAvgRating());
        assertEquals(new BigDecimal("3.25"), score.getBayesianScore());
        assertEquals(3L, score.getDistribution().get(2));
        assertEquals(4L, score.getDistribution().get(3));
        assertEquals(3L, score.getDistribution().get(4));
    }

    private void mockReviews(Long storeId, List<Review> reviews) {
        when(reviewRepository.findByStoreIdAndStatusAndVisibleTrue(
                eq(storeId), eq(ReviewStatus.ACTIVE))).thenReturn(reviews);
    }

    private Review createReview(int rating) {
        Review review = new Review();
        review.setRating(rating);
        review.setStatus(ReviewStatus.ACTIVE);
        review.setVisible(true);
        review.setCurrentVersion(1);
        review.setContent("test content");
        return review;
    }
}
