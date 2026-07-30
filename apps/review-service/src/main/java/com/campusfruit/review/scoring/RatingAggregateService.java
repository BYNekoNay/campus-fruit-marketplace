package com.campusfruit.review.scoring;

import com.campusfruit.review.entity.RatingAggregate;
import com.campusfruit.review.repository.RatingAggregateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 评分聚合服务。
 * 异步重算门店评分聚合（贝叶斯评分 + 分布），支持全量重建。
 */
@Service
public class RatingAggregateService {

    private static final Logger log = LoggerFactory.getLogger(RatingAggregateService.class);

    private final BayesianRatingCalculator calculator;
    private final RatingAggregateRepository aggregateRepository;
    private final ObjectMapper objectMapper;

    public RatingAggregateService(BayesianRatingCalculator calculator,
                                   RatingAggregateRepository aggregateRepository,
                                   ObjectMapper objectMapper) {
        this.calculator = calculator;
        this.aggregateRepository = aggregateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 异步重算单个门店的评分聚合。
     * 1. 调用 BayesianRatingCalculator 计算贝叶斯评分
     * 2. 更新/INSERT rating_aggregates
     * 3. 更新 rating_distribution (JSON)
     * 4. 版本号 +1
     */
    @Async
    @Transactional
    public void recalculate(Long storeId) {
        try {
            BayesianScore bayesianScore = calculator.calculate(storeId);

            RatingAggregate aggregate = aggregateRepository.findByStoreId(storeId)
                    .orElseGet(() -> {
                        RatingAggregate a = new RatingAggregate();
                        a.setStoreId(storeId);
                        a.setTotalRatings(0);
                        return a;
                    });

            aggregate.setAvgRating(bayesianScore.getAvgRating());
            aggregate.setBayesianRating(bayesianScore.getBayesianScore());
            aggregate.setTotalRatings(bayesianScore.getTotalRatings());

            // 序列化评分分布为 JSON
            String distributionJson = toDistributionJson(bayesianScore.getDistribution());
            aggregate.setRatingDistribution(distributionJson);

            // 版本号 +1
            aggregate.setVersion(aggregate.getVersion() + 1);
            aggregate.setCalculatedAt(Instant.now());

            aggregateRepository.save(aggregate);

            log.info("门店 {} 评分聚合已重算: bayesian={}, avg={}, totalRatings={}, version={}",
                    storeId, aggregate.getBayesianRating(), aggregate.getAvgRating(),
                    aggregate.getTotalRatings(), aggregate.getVersion());

        } catch (Exception e) {
            log.error("重算门店 {} 评分聚合失败: {}", storeId, e.getMessage(), e);
        }
    }

    /**
     * 全量重算所有门店评分（管理员触发）。
     */
    @Async
    @Transactional
    public void rebuildAll() {
        log.info("开始全量重算所有门店评分...");
        long start = System.currentTimeMillis();
        int count = 0;

        for (RatingAggregate agg : aggregateRepository.findAll()) {
            recalculate(agg.getStoreId());
            count++;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("全量重算完成: {} 家门店, 耗时 {}ms", count, elapsed);
    }

    /**
     * 获取指定门店的评分聚合（同步查询）。
     */
    @Transactional(readOnly = true)
    public Optional<RatingAggregate> getAggregate(Long storeId) {
        return aggregateRepository.findByStoreId(storeId);
    }

    /**
     * 分页获取所有评分聚合。
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RatingAggregate> listAggregates(
            org.springframework.data.domain.Pageable pageable) {
        return aggregateRepository.findAll(pageable);
    }

    private String toDistributionJson(Map<Integer, Long> distribution) {
        try {
            return objectMapper.writeValueAsString(distribution);
        } catch (JsonProcessingException e) {
            log.error("序列化评分分布失败", e);
            return "{}";
        }
    }
}
