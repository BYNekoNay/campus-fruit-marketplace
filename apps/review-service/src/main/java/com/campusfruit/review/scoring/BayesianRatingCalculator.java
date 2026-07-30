package com.campusfruit.review.scoring;

import com.campusfruit.review.entity.Review;
import com.campusfruit.review.entity.RatingAggregate;
import com.campusfruit.review.enums.ReviewStatus;
import com.campusfruit.review.repository.RatingAggregateRepository;
import com.campusfruit.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 贝叶斯评分计算器。
 * <p>
 * 贝叶斯平均公式：bayesianScore = (v/(v+m)) * R + (m/(v+m)) * C
 * <ul>
 *   <li>R = 门店算术平均分</li>
 *   <li>v = 门店有效评价数</li>
 *   <li>C = 全局平均分（先验均值）</li>
 *   <li>m = 最小样本数（权重，默认为5）</li>
 * </ul>
 * 核心思想：少量评价时向先验值回缩，避免1条五星就排第一；大样本时趋近真实均值。
 */
@Component
public class BayesianRatingCalculator {

    private static final Logger log = LoggerFactory.getLogger(BayesianRatingCalculator.class);

    private static final double DEFAULT_GLOBAL_AVG_C = 3.5;
    private static final int DEFAULT_MIN_VOTES_M = 10;

    private final ReviewRepository reviewRepository;
    private final RatingAggregateRepository ratingAggregateRepository;

    public BayesianRatingCalculator(ReviewRepository reviewRepository,
                                     RatingAggregateRepository ratingAggregateRepository) {
        this.reviewRepository = reviewRepository;
        this.ratingAggregateRepository = ratingAggregateRepository;
    }

    /**
     * 计算指定门店的贝叶斯评分。
     *
     * @param storeId 门店 ID
     * @return BayesianScore 包含贝叶斯评分、原始均分、评价总数、置信度、分布
     */
    @Transactional(readOnly = true)
    public BayesianScore calculate(Long storeId) {
        // 1. 查询该门店所有 ACTIVE + visible 评价
        List<Review> reviews = reviewRepository.findByStoreIdAndStatusAndVisibleTrue(storeId, ReviewStatus.ACTIVE);
        int v = reviews.size();

        // 2. 获取全局参数
        double[] globalParams = getGlobalAverage();
        double C = globalParams[0];
        double m = globalParams[1];

        // 3. 计算评分分布
        Map<Integer, Long> distribution = reviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));
        // 确保 1-5 星都有条目
        for (int star = 1; star <= 5; star++) {
            distribution.putIfAbsent(star, 0L);
        }

        // 4. 小样本保护：v=0 时不按 0 分处理，返回先验值 C
        if (v == 0) {
            BayesianScore priorScore = new BayesianScore();
            priorScore.setBayesianScore(BigDecimal.valueOf(C).setScale(2, RoundingMode.HALF_UP));
            priorScore.setAvgRating(BigDecimal.ZERO.setScale(2));
            priorScore.setTotalRatings(0);
            priorScore.setConfidence(0.0);
            priorScore.setDistribution(distribution);
            log.debug("门店 {} 无评价，返回先验值 C={}", storeId, C);
            return priorScore;
        }

        // 5. 计算门店算术平均分 R
        double sum = reviews.stream().mapToInt(Review::getRating).sum();
        double R = sum / v;

        // 6. 贝叶斯公式
        double vD = (double) v;
        double bayesianRaw = (vD / (vD + m)) * R + (m / (vD + m)) * C;

        // 7. 置信度：v 越大越接近 1，但最多到 0.99
        double confidence = Math.min(vD / (vD + m), 0.99);

        BayesianScore score = new BayesianScore();
        score.setBayesianScore(BigDecimal.valueOf(bayesianRaw).setScale(2, RoundingMode.HALF_UP));
        score.setAvgRating(BigDecimal.valueOf(R).setScale(2, RoundingMode.HALF_UP));
        score.setTotalRatings(v);
        score.setConfidence(Math.round(confidence * 100.0) / 100.0);
        score.setDistribution(distribution);

        log.debug("门店 {} 贝叶斯评分: raw={}, bayesian={}, avg={}, v={}, confidence={}",
                storeId, R, score.getBayesianScore(), score.getAvgRating(), v, score.getConfidence());

        return score;
    }

    /**
     * 获取全局平均分 C 和最小样本数 m。
     * 优先从 rating_aggregates 中计算加权全局均值；数据库无数据时使用默认值 C=3.5, m=5。
     *
     * @return [C, m]
     */
    double[] getGlobalAverage() {
        List<RatingAggregate> all = ratingAggregateRepository.findAll();
        if (all.isEmpty()) {
            return new double[]{DEFAULT_GLOBAL_AVG_C, DEFAULT_MIN_VOTES_M};
        }

        // 加权平均：sum(avgRating * totalRatings) / sum(totalRatings)
        double weightedSum = 0;
        long totalCount = 0;
        for (RatingAggregate agg : all) {
            if (agg.getAvgRating() != null && agg.getTotalRatings() != null && agg.getTotalRatings() > 0) {
                weightedSum += agg.getAvgRating().doubleValue() * agg.getTotalRatings();
                totalCount += agg.getTotalRatings();
            }
        }

        if (totalCount == 0) {
            return new double[]{DEFAULT_GLOBAL_AVG_C, DEFAULT_MIN_VOTES_M};
        }

        double C = weightedSum / totalCount;
        return new double[]{C, DEFAULT_MIN_VOTES_M};
    }
}
