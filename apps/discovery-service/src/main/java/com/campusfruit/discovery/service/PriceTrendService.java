package com.campusfruit.discovery.service;

import com.campusfruit.discovery.entity.PriceDailyStat;
import com.campusfruit.discovery.repository.PriceDailyStatRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 价格趋势计算：提供 7/30 天最低价、中位价和趋势方向。
 */
@Service
public class PriceTrendService {

    private final PriceDailyStatRepository statRepository;

    public PriceTrendService(PriceDailyStatRepository statRepository) {
        this.statRepository = statRepository;
    }

    public Map<String, Object> getTrends(Long canonicalFruitId) {
        LocalDate today = LocalDate.now();
        List<PriceDailyStat> weekStats = statRepository
            .findByCanonicalFruitIdAndStatDateBetween(canonicalFruitId, today.minusDays(7), today);
        List<PriceDailyStat> monthStats = statRepository
            .findByCanonicalFruitIdAndStatDateBetween(canonicalFruitId, today.minusDays(30), today);

        return Map.of(
            "week7", buildTrend(weekStats, "近7天"),
            "month30", buildTrend(monthStats, "近30天"),
            "latestDate", today.toString()
        );
    }

    private Map<String, Object> buildTrend(List<PriceDailyStat> stats, String label) {
        if (stats.isEmpty()) return Map.of("label", label, "insufficientData", true);

        double min = stats.stream().mapToDouble(s -> s.getMinPrice().doubleValue()).min().orElse(0);
        double max = stats.stream().mapToDouble(s -> s.getMaxPrice().doubleValue()).max().orElse(0);
        double avg = stats.stream().mapToDouble(s -> s.getAvgPrice().doubleValue()).average().orElse(0);

        // 趋势：对比首日与最后一日
        String direction = "stable";
        if (stats.size() >= 2) {
            double first = stats.get(0).getAvgPrice().doubleValue();
            double last = stats.get(stats.size() - 1).getAvgPrice().doubleValue();
            if (last < first * 0.95) direction = "down"; else if (last > first * 1.05) direction = "up";
        }

        return Map.of(
            "label", label, "insufficientData", false,
            "minPrice", BigDecimal.valueOf(min), "maxPrice", BigDecimal.valueOf(max),
            "avgPrice", BigDecimal.valueOf(avg), "direction", direction,
            "sampleDays", stats.size()
        );
    }
}
