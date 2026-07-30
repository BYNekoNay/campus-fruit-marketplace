package com.campusfruit.discovery.ranking;

import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.ranking.SortModeHandler.RankedOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 冷启动与探索处理器。
 *
 * - 新门店（reviewCount < minReviewCount 或 营业天数 < minOpeningDays）：赋予探索分，占配置的探索位
 * - 低评分门店（avgRating < minThreshold 且 reviewCount > minReviewCount）：降权
 * - 被暂停/关闭门店不进入排名
 */
@Component
public class ColdStartHandler {

    private static final Logger log = LoggerFactory.getLogger(ColdStartHandler.class);

    private final RankingVersionConfig config;

    public ColdStartHandler(RankingVersionConfig config) {
        this.config = config;
    }

    /**
     * 过滤不可参与排名的门店（暂停/关闭）。
     */
    public List<RankedOffer> filterIneligible(List<RankedOffer> ranked) {
        return ranked.stream()
                .filter(r -> isEligible(r.projection))
                .collect(Collectors.toList());
    }

    /**
     * 应用冷启动探索：前 N 个位置保留给新门店。
     *
     * @param ranked  已排序列表
     * @param maxExplorationSlots 最大探索位
     * @return 处理后的列表（冷启动加分后重排）
     */
    public List<RankedOffer> applyColdStart(List<RankedOffer> ranked, int maxExplorationSlots) {
        List<RankedOffer> newStores = new ArrayList<>();
        List<RankedOffer> normalStores = new ArrayList<>();

        for (RankedOffer r : ranked) {
            if (isNewStore(r.projection)) {
                newStores.add(r);
            } else {
                normalStores.add(r);
            }
        }

        // 对低评分门店降权
        for (RankedOffer r : normalStores) {
            if (isLowRatedStore(r.projection)) {
                r.result.getSubScores().put("rating",
                        r.result.getSubScores().getOrDefault("rating", 0.5) * 0.5);
            }
        }

        List<RankedOffer> result = new ArrayList<>();

        // 前 maxExplorationSlots 个位置给新门店
        int slots = Math.min(maxExplorationSlots, newStores.size());
        for (int i = 0; i < slots; i++) {
            result.add(newStores.get(i));
        }

        // 其余正常门店
        result.addAll(normalStores);

        // 剩余的新门店追加到末尾
        for (int i = slots; i < newStores.size(); i++) {
            result.add(newStores.get(i));
        }

        return result;
    }

    /**
     * 判断门店是否有资格参与排名。
     */
    public boolean isEligible(StoreOfferProjection p) {
        String status = p.getStoreStatus();
        if (status == null) return true;
        String upper = status.toUpperCase();
        return !"SUSPENDED".equals(upper) && !"CLOSED".equals(upper) && !"BLOCKED".equals(upper);
    }

    /**
     * 判断是否新门店（评论数低于阈值 或 营业天数低于阈值）。
     */
    public boolean isNewStore(StoreOfferProjection p) {
        int reviewCount = p.getReviewCount() != null ? p.getReviewCount() : 0;
        if (reviewCount < config.getColdStartMinReviewCount()) {
            return true;
        }

        // 检查营业天数：使用 lastEventAt 作为门店首次活动时间的近似值
        int openingDays = estimateOpeningDays(p);
        return openingDays < config.getColdStartMinOpeningDays();
    }

    /**
     * 判断是否低评分门店（评分低于阈值且评论数足够）。
     */
    public boolean isLowRatedStore(StoreOfferProjection p) {
        int reviewCount = p.getReviewCount() != null ? p.getReviewCount() : 0;
        double avgRating = p.getAvgRating() != null ? p.getAvgRating().doubleValue() : 0;
        return avgRating < config.getColdStartMinRatingThreshold()
                && reviewCount >= config.getColdStartLowRatingMinReviewCount();
    }

    /**
     * 获取探索加分。
     */
    public double getExplorationBonus() {
        return config.getColdStartExplorationBonus();
    }

    /**
     * 获取最大探索位数。
     */
    public int getMaxExplorationSlots() {
        return config.getColdStartMaxExplorationSlots();
    }

    /**
     * 获取最少评论数阈值。
     */
    public int getMinReviewCount() {
        return config.getColdStartMinReviewCount();
    }

    /**
     * 估算门店营业天数。
     * 使用 lastEventAt 作为门店首次活动时间的近似值。
     */
    private int estimateOpeningDays(StoreOfferProjection p) {
        if (p.getLastEventAt() == null) {
            return 0; // 无活动记录，视为当天新开
        }
        long days = ChronoUnit.DAYS.between(p.getLastEventAt(), Instant.now());
        return (int) Math.max(0, days);
    }
}
