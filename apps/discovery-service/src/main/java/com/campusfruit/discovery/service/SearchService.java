package com.campusfruit.discovery.service;

import com.campusfruit.discovery.dto.*;
import com.campusfruit.discovery.entity.PriceDailyStat;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.fallback.DiscoveryFallbackConfig;
import com.campusfruit.discovery.ranking.*;
import com.campusfruit.discovery.ranking.SortModeHandler.RankedOffer;
import com.campusfruit.discovery.repository.PriceDailyStatRepository;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final double KM_TO_LAT = 1.0 / 111.0;
    private static final double KM_TO_LNG_AT_30N = 1.0 / (111.0 * Math.cos(Math.toRadians(30)));

    private final StoreOfferProjectionRepository projectionRepository;
    private final PriceDailyStatRepository statsRepository;
    private final DiscoveryFallbackConfig fallbackConfig;
    private final SortModeHandler sortModeHandler;
    private final ColdStartHandler coldStartHandler;
    private final RankingExplainabilityService explainabilityService;

    public SearchService(StoreOfferProjectionRepository projectionRepository,
                          PriceDailyStatRepository statsRepository,
                          DiscoveryFallbackConfig fallbackConfig,
                          SortModeHandler sortModeHandler,
                          ColdStartHandler coldStartHandler,
                          RankingExplainabilityService explainabilityService) {
        this.projectionRepository = projectionRepository;
        this.statsRepository = statsRepository;
        this.fallbackConfig = fallbackConfig;
        this.sortModeHandler = sortModeHandler;
        this.coldStartHandler = coldStartHandler;
        this.explainabilityService = explainabilityService;
    }

    /**
     * 多条件组合搜索（集成排名引擎）。
     */
    public SearchResponse search(SearchRequest request) {
        // 降级：投影表为空时返回空结果 + 提示
        if (!fallbackConfig.isDataAvailable()) {
            SearchResponse response = new SearchResponse();
            response.setTotalCount(0);
            response.setItems(List.of());
            return response;
        }
        Page<StoreOfferProjection> page = queryByConditions(request);
        List<StoreOfferProjection> projections = page.getContent();

        // 使用排名引擎排序
        List<RankedOffer> ranked = sortModeHandler.sort(
                projections,
                request.getSortBy(),
                request.getLat(), request.getLng(),
                request.getKeyword());

        // 冷启动处理（仅综合排序模式下应用）
        if (request.getSortBy() == SearchRequest.SortBy.COMPREHENSIVE) {
            ranked = coldStartHandler.filterIneligible(ranked);
            ranked = coldStartHandler.applyColdStart(ranked, coldStartHandler.getMaxExplorationSlots());
        } else {
            ranked = coldStartHandler.filterIneligible(ranked);
        }

        // 转换为 DTO 并缓存排名 trace
        List<StoreOfferProjectionDTO> items = ranked.stream()
                .map(r -> {
                    StoreOfferProjectionDTO dto = toDTO(r.projection, request.getLat(), request.getLng());
                    dto.setRankingScore(r.result.getScore());
                    dto.setRankingTraceId(r.result.getRankingTraceId());
                    dto.setColdStart(coldStartHandler.isNewStore(r.projection));
                    // 标记销量来源
                    Double salesSource = r.result.getSubScores().get("salesSource");
                    if (salesSource != null) {
                        dto.setSalesSource(salesSource == 1.0 ? "基于近30天销售" : "基于评价热度（参考）");
                    }
                    explainabilityService.cacheTrace(r.result);
                    return dto;
                })
                .collect(Collectors.toList());

        SearchResponse response = new SearchResponse(page.getTotalElements(), items);

        // 附带价格统计
        PriceStatsResponse priceStats = computePriceStatsForResults(projections);
        response.setPriceStats(priceStats);

        return response;
    }

    private Page<StoreOfferProjection> queryByConditions(SearchRequest request) {
        // 综合排序模式下使用无分页查询，由应用层排序
        Sort sort = buildSort(request.getSortBy());
        int pageSize = request.getSortBy() == SearchRequest.SortBy.COMPREHENSIVE
                ? Math.max(request.getSize() * 3, 100) // 取更多数据供排名引擎使用
                : request.getSize();
        PageRequest pageRequest = PageRequest.of(0, pageSize, sort);

        boolean hasKeyword = request.getKeyword() != null && !request.getKeyword().isBlank();
        boolean hasBbox = request.getLat() != null && request.getLng() != null && request.getRadiusKm() != null;

        if (hasBbox) {
            double latDelta = request.getRadiusKm() * KM_TO_LAT;
            double lngDelta = request.getRadiusKm() * KM_TO_LNG_AT_30N;
            double latMin = request.getLat() - latDelta;
            double latMax = request.getLat() + latDelta;
            double lngMin = request.getLng() - lngDelta;
            double lngMax = request.getLng() + lngDelta;

            if (hasKeyword) {
                return projectionRepository.findByBoundingBoxAndKeyword(
                        latMin, latMax, lngMin, lngMax, request.getKeyword(), pageRequest);
            }
            return projectionRepository.findByBoundingBox(latMin, latMax, lngMin, lngMax, pageRequest);
        }

        if (hasKeyword && request.getCategory() != null && !request.getCategory().isBlank()) {
            return projectionRepository.searchByKeywordAndCategory(
                    request.getKeyword(), request.getCategory(), pageRequest);
        }

        if (hasKeyword) {
            return projectionRepository.searchByKeyword(request.getKeyword(), pageRequest);
        }

        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            BigDecimal min = request.getMinPrice() != null ? request.getMinPrice() : BigDecimal.ZERO;
            BigDecimal max = request.getMaxPrice() != null ? request.getMaxPrice() : BigDecimal.valueOf(999999);
            return projectionRepository.findByPriceRange(min, max, pageRequest);
        }

        if (request.getMinRating() != null && request.getMinRating() > 0) {
            return projectionRepository.findByMinRating(
                    BigDecimal.valueOf(request.getMinRating()), pageRequest);
        }

        return projectionRepository.findAll(pageRequest);
    }

    private Sort buildSort(SearchRequest.SortBy sortBy) {
        return switch (sortBy) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "standardPricePer500g");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "standardPricePer500g");
            case RATING -> Sort.by(Sort.Direction.DESC, "avgRating");
            case SALES -> Sort.by(Sort.Direction.DESC, "reviewCount");
            case COMPREHENSIVE -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> Sort.by(Sort.Direction.DESC, "updatedAt");
        };
    }

    /**
     * 为搜索结果计算价格统计。
     */
    private PriceStatsResponse computePriceStatsForResults(List<StoreOfferProjection> projections) {
        PriceStatsResponse stats = new PriceStatsResponse();
        if (projections.isEmpty()) {
            stats.setSampleInsufficient(true);
            return stats;
        }

        List<BigDecimal> prices = projections.stream()
                .filter(p -> p.getStandardPricePer500g() != null)
                .map(StoreOfferProjection::getStandardPricePer500g)
                .sorted()
                .toList();

        if (prices.isEmpty()) {
            stats.setSampleInsufficient(true);
            return stats;
        }

        stats.setMinPrice(prices.get(0));
        stats.setMaxPrice(prices.get(prices.size() - 1));
        stats.setAvgPrice(prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP));
        stats.setMedianPrice(prices.get(prices.size() / 2));
        stats.setSampleCount(prices.size());

        long storeCount = projections.stream()
                .map(StoreOfferProjection::getStoreId)
                .distinct()
                .count();
        stats.setStoreCount((int) storeCount);
        stats.setSampleTime(LocalDate.now());
        stats.setSampleInsufficient(prices.size() < 3);

        return stats;
    }

    /**
     * 获取最多 5 个报价的对比数据。
     */
    public CompareResponse getComparison(List<Long> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) {
            return new CompareResponse(List.of(), null);
        }

        List<Long> ids = offerIds.size() > 5 ? offerIds.subList(0, 5) : offerIds;
        List<StoreOfferProjection> projections = projectionRepository.findByOfferIdIn(ids);

        List<CompareResponse.CompareItem> items = projections.stream()
                .map(this::toCompareItem)
                .collect(Collectors.toList());

        PriceStatsResponse stats = computeStats(projections);

        return new CompareResponse(items, stats);
    }

    private CompareResponse.CompareItem toCompareItem(StoreOfferProjection p) {
        CompareResponse.CompareItem item = new CompareResponse.CompareItem();
        item.setOfferId(p.getOfferId());
        item.setStoreName(p.getStoreName());
        item.setFruitVariety(p.getFruitVariety());
        item.setFruitGrade(p.getFruitGrade());
        item.setFruitOrigin(p.getFruitOrigin());
        item.setSalesUnit(p.getSalesUnit());
        item.setUnitPriceYuan(p.getUnitPrice() != null
                ? BigDecimal.valueOf(p.getUnitPrice()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : null);
        item.setStandardPricePer500g(p.getStandardPricePer500g());
        item.setIsComparable(p.getIsComparable());
        item.setAvgRating(p.getAvgRating());
        item.setReviewCount(p.getReviewCount());
        item.setPriceStale(p.getPriceStale());
        return item;
    }

    private PriceStatsResponse computeStats(List<StoreOfferProjection> projections) {
        PriceStatsResponse stats = new PriceStatsResponse();
        if (projections.isEmpty()) {
            stats.setSampleInsufficient(true);
            return stats;
        }

        List<BigDecimal> prices = projections.stream()
                .filter(p -> p.getStandardPricePer500g() != null)
                .map(StoreOfferProjection::getStandardPricePer500g)
                .sorted()
                .toList();

        if (prices.isEmpty()) {
            stats.setSampleInsufficient(true);
            return stats;
        }

        stats.setMinPrice(prices.get(0));
        stats.setMaxPrice(prices.get(prices.size() - 1));
        stats.setAvgPrice(prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP));
        stats.setMedianPrice(prices.get(prices.size() / 2));
        stats.setSampleCount(prices.size());

        long storeCount = projections.stream()
                .map(StoreOfferProjection::getStoreId)
                .distinct()
                .count();
        stats.setStoreCount((int) storeCount);
        stats.setSampleTime(LocalDate.now());
        stats.setSampleInsufficient(prices.size() < 3);

        return stats;
    }

    /**
     * 获取价格统计（指定水果 ID）。
     */
    public PriceStatsResponse getPriceStats(Long canonicalFruitId) {
        List<StoreOfferProjection> projections = projectionRepository.findByCanonicalFruitId(canonicalFruitId);
        PriceStatsResponse stats = computeStats(projections);

        // 尝试从 price_daily_stats 表获取历史数据
        List<PriceDailyStat> dailyStats = statsRepository.findByCanonicalFruitIdAndStatDateBetween(
                canonicalFruitId, LocalDate.now().minusDays(1), LocalDate.now());

        if (!dailyStats.isEmpty()) {
            PriceDailyStat latest = dailyStats.get(dailyStats.size() - 1);
            stats.setSampleTime(latest.getStatDate());
            if (stats.isSampleInsufficient() && latest.getSampleCount() >= 3) {
                stats.setMinPrice(latest.getMinPrice());
                stats.setMaxPrice(latest.getMaxPrice());
                stats.setMedianPrice(latest.getMedianPrice());
                stats.setAvgPrice(latest.getAvgPrice());
                stats.setStoreCount(latest.getStoreCount());
                stats.setSampleCount(latest.getSampleCount());
                stats.setSampleInsufficient(false);
            }
        }

        return stats;
    }

    /**
     * 获取所有水果品类列表。
     */
    public List<String> getCategories() {
        return projectionRepository.findDistinctCategories();
    }

    /**
     * 获取门店的报价列表。
     */
    public List<StoreOfferProjectionDTO> getStoreOffers(Long storeId) {
        List<StoreOfferProjection> projections = projectionRepository.findByStoreId(storeId);
        return projections.stream()
                .filter(p -> p.getOfferId() != null && p.getOfferId() != 0L)
                .map(p -> toDTO(p, null, null))
                .collect(Collectors.toList());
    }

    private StoreOfferProjectionDTO toDTO(StoreOfferProjection p, Double userLat, Double userLng) {
        StoreOfferProjectionDTO dto = new StoreOfferProjectionDTO();
        dto.setStoreId(p.getStoreId());
        dto.setStoreName(p.getStoreName());
        dto.setStoreLat(p.getStoreLat());
        dto.setStoreLng(p.getStoreLng());
        dto.setOfferId(p.getOfferId());
        dto.setFruitVariety(p.getFruitVariety());
        dto.setFruitGrade(p.getFruitGrade());
        dto.setSalesUnit(p.getSalesUnit());
        dto.setUnitPriceYuan(p.getUnitPrice() != null
                ? BigDecimal.valueOf(p.getUnitPrice()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : null);
        dto.setStandardPricePer500g(p.getStandardPricePer500g());
        dto.setIsComparable(p.getIsComparable());
        dto.setAvgRating(p.getAvgRating());
        dto.setReviewCount(p.getReviewCount());
        dto.setPriceStale(p.getPriceStale());

        // 计算距离
        if (userLat != null && userLng != null
                && p.getStoreLat() != null && p.getStoreLng() != null) {
            double distanceKm = RankingFormula.haversineKm(userLat, userLng, p.getStoreLat(), p.getStoreLng());
            dto.setDistance(Math.round(distanceKm * 100.0) / 100.0);
        }

        return dto;
    }
}
