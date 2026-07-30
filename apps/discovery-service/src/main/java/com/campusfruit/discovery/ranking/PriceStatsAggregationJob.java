package com.campusfruit.discovery.ranking;

import com.campusfruit.discovery.entity.PriceDailyStat;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.PriceDailyStatRepository;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 价格日级统计定时任务：每天凌晨 2:00 聚合前一天的 price_daily_stats 数据。
 */
@Component
public class PriceStatsAggregationJob {

    private static final Logger log = LoggerFactory.getLogger(PriceStatsAggregationJob.class);

    private final StoreOfferProjectionRepository projectionRepository;
    private final PriceDailyStatRepository statsRepository;

    public PriceStatsAggregationJob(StoreOfferProjectionRepository projectionRepository,
                                     PriceDailyStatRepository statsRepository) {
        this.projectionRepository = projectionRepository;
        this.statsRepository = statsRepository;
    }

    /**
     * 每天凌晨 2:00 聚合前一天价格统计。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void aggregateDailyStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting price stats aggregation for date: {}", yesterday);

        try {
            List<StoreOfferProjection> projections = projectionRepository.findAll();

            // 按 canonical_fruit_id 分组
            Map<Long, List<StoreOfferProjection>> grouped = projections.stream()
                    .filter(p -> p.getCanonicalFruitId() != null)
                    .filter(p -> p.getStandardPricePer500g() != null)
                    .collect(Collectors.groupingBy(StoreOfferProjection::getCanonicalFruitId));

            int savedCount = 0;

            for (Map.Entry<Long, List<StoreOfferProjection>> entry : grouped.entrySet()) {
                Long fruitId = entry.getKey();
                List<BigDecimal> prices = entry.getValue().stream()
                        .map(StoreOfferProjection::getStandardPricePer500g)
                        .sorted()
                        .collect(Collectors.toList());

                if (prices.isEmpty()) continue;

                PriceDailyStat stat = new PriceDailyStat();
                stat.setCanonicalFruitId(fruitId);
                stat.setStatDate(yesterday);
                stat.setMinPrice(prices.get(0));
                stat.setMaxPrice(prices.get(prices.size() - 1));
                stat.setMedianPrice(prices.get(prices.size() / 2));
                stat.setAvgPrice(prices.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP));
                stat.setSampleCount(prices.size());

                // 统计不重复门店数
                long storeCount = entry.getValue().stream()
                        .map(StoreOfferProjection::getStoreId)
                        .distinct()
                        .count();
                stat.setStoreCount((int) storeCount);

                statsRepository.save(stat);
                savedCount++;
            }

            log.info("Price stats aggregation completed: {} fruit entries saved for {}", savedCount, yesterday);
        } catch (Exception e) {
            log.error("Price stats aggregation failed for date: {}", yesterday, e);
        }
    }
}
