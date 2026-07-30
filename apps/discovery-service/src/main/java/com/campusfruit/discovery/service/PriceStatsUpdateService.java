package com.campusfruit.discovery.service;

import com.campusfruit.discovery.entity.PriceDailyStat;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.PriceDailyStatRepository;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class PriceStatsUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PriceStatsUpdateService.class);

    private final PriceDailyStatRepository statsRepository;
    private final StoreOfferProjectionRepository projectionRepository;

    public PriceStatsUpdateService(PriceDailyStatRepository statsRepository,
                                    StoreOfferProjectionRepository projectionRepository) {
        this.statsRepository = statsRepository;
        this.projectionRepository = projectionRepository;
    }

    /**
     * 异步更新指定水果的每日价格统计。
     */
    @Async
    public void updateStatsAsync(Long canonicalFruitId) {
        try {
            log.debug("Updating price stats for canonicalFruitId={}", canonicalFruitId);
            recalculateAndSave(canonicalFruitId);
        } catch (Exception e) {
            log.error("Failed to update price stats for canonicalFruitId={}", canonicalFruitId, e);
        }
    }

    private void recalculateAndSave(Long canonicalFruitId) {
        List<StoreOfferProjection> projections = projectionRepository.findByCanonicalFruitId(canonicalFruitId);

        List<BigDecimal> prices = projections.stream()
                .filter(p -> p.getStandardPricePer500g() != null)
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getOfferStatus()))
                .map(StoreOfferProjection::getStandardPricePer500g)
                .sorted()
                .toList();

        if (prices.isEmpty()) {
            log.debug("No active prices for canonicalFruitId={}", canonicalFruitId);
            return;
        }

        PriceDailyStat stat = new PriceDailyStat();
        stat.setCanonicalFruitId(canonicalFruitId);
        stat.setStatDate(LocalDate.now());
        stat.setMinPrice(prices.get(0));
        stat.setMaxPrice(prices.get(prices.size() - 1));
        stat.setAvgPrice(prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP));
        stat.setMedianPrice(prices.get(prices.size() / 2));
        stat.setSampleCount(prices.size());

        long storeCount = projections.stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getOfferStatus()))
                .map(StoreOfferProjection::getStoreId)
                .filter(id -> id != null)
                .distinct()
                .count();
        stat.setStoreCount((int) storeCount);

        statsRepository.save(stat);
        log.info("Updated price_daily_stats for canonicalFruitId={}: min={}, max={}, avg={}, samples={}",
                canonicalFruitId, stat.getMinPrice(), stat.getMaxPrice(), stat.getAvgPrice(), stat.getSampleCount());
    }
}
