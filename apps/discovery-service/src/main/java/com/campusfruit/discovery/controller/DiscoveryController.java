package com.campusfruit.discovery.controller;

import com.campusfruit.discovery.dto.*;
import com.campusfruit.discovery.ranking.RankingExplanation;
import com.campusfruit.discovery.ranking.RankingExplainabilityService;
import com.campusfruit.discovery.service.FavoritesService;
import com.campusfruit.discovery.service.NearbyStoreService;
import com.campusfruit.discovery.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class DiscoveryController {

    private final SearchService searchService;
    private final FavoritesService favoritesService;
    private final NearbyStoreService nearbyStoreService;
    private final RankingExplainabilityService explainabilityService;

    public DiscoveryController(SearchService searchService, FavoritesService favoritesService,
                                NearbyStoreService nearbyStoreService,
                                RankingExplainabilityService explainabilityService) {
        this.searchService = searchService;
        this.favoritesService = favoritesService;
        this.nearbyStoreService = nearbyStoreService;
        this.explainabilityService = explainabilityService;
    }

    /**
     * 搜索报价。
     */
    @PostMapping("/api/discovery/search")
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 比价（GET 参数方式）。
     */
    @GetMapping("/api/discovery/compare")
    public ResponseEntity<CompareResponse> compare(@RequestParam("ids") String ids) {
        List<Long> offerIds = parseIdList(ids);
        CompareResponse response = searchService.getComparison(offerIds);
        return ResponseEntity.ok(response);
    }

    /**
     * 比价（POST 请求体方式）。
     */
    @PostMapping("/api/discovery/compare")
    public ResponseEntity<CompareResponse> comparePost(@Valid @RequestBody CompareRequest request) {
        CompareResponse response = searchService.getComparison(request.getOfferIds());
        return ResponseEntity.ok(response);
    }

    /**
     * 价格统计。
     */
    @GetMapping("/api/discovery/stats/{fruitId}")
    public ResponseEntity<PriceStatsResponse> priceStats(@PathVariable Long fruitId) {
        PriceStatsResponse response = searchService.getPriceStats(fruitId);
        return ResponseEntity.ok(response);
    }

    /**
     * 品类列表。
     */
    @GetMapping("/api/discovery/categories")
    public ResponseEntity<List<String>> categories() {
        List<String> categories = searchService.getCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * 附近门店查询。
     */
    @GetMapping("/api/discovery/nearby")
    public ResponseEntity<List<NearbyStoreDTO>> nearby(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "radius", defaultValue = "3") double radius,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        List<NearbyStoreDTO> stores = nearbyStoreService.findNearbyStores(lat, lng, radius, limit);
        return ResponseEntity.ok(stores);
    }

    /**
     * 门店报价列表。
     */
    @GetMapping("/api/discovery/stores/{storeId}/offers")
    public ResponseEntity<List<StoreOfferProjectionDTO>> storeOffers(@PathVariable Long storeId) {
        List<StoreOfferProjectionDTO> offers = searchService.getStoreOffers(storeId);
        return ResponseEntity.ok(offers);
    }

    /**
     * 我的收藏（返回 storeId 列表 + 门店信息可扩展）。
     */
    @GetMapping("/api/favorites")
    public ResponseEntity<List<Long>> myFavorites(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserId(jwt);
        List<Long> favorites = favoritesService.getMyFavorites(userId, page, size);
        return ResponseEntity.ok(favorites);
    }

    /**
     * 收藏门店。
     */
    @PostMapping("/api/favorites/{storeId}")
    public ResponseEntity<Void> addFavorite(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable Long storeId) {
        Long userId = extractUserId(jwt);
        favoritesService.addFavorite(userId, storeId);
        return ResponseEntity.ok().build();
    }

    /**
     * 取消收藏。
     */
    @DeleteMapping("/api/favorites/{storeId}")
    public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable Long storeId) {
        Long userId = extractUserId(jwt);
        favoritesService.removeFavorite(userId, storeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取排序解释。
     */
    @GetMapping("/api/discovery/ranking/explain/{offerId}")
    public ResponseEntity<RankingExplanation> explainRanking(
            @PathVariable Long offerId,
            @RequestParam String traceId) {
        RankingExplanation explanation = explainabilityService.explain(offerId, traceId);
        return ResponseEntity.ok(explanation);
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalStateException("No authenticated user");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("Invalid JWT: missing subject");
        }
        return Long.parseLong(subject);
    }

    private List<Long> parseIdList(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .limit(5)
                .collect(Collectors.toList());
    }
}
