package com.campusfruit.discovery.analytics;

import com.campusfruit.discovery.ranking.RankingExplanation;
import com.campusfruit.discovery.ranking.RankingExplainabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final FunnelAnalyticsService funnelAnalyticsService;
    private final RankingExplainabilityService explainabilityService;

    public AnalyticsController(FunnelAnalyticsService funnelAnalyticsService,
                                RankingExplainabilityService explainabilityService) {
        this.funnelAnalyticsService = funnelAnalyticsService;
        this.explainabilityService = explainabilityService;
    }

    /**
     * 记录曝光事件（内部调用）。
     */
    @PostMapping("/internal/analytics/impression")
    public ResponseEntity<Void> trackImpression(@RequestBody Map<String, Object> body) {
        String userId = (String) body.getOrDefault("userId", "anonymous");
        @SuppressWarnings("unchecked")
        List<Integer> offerIdInts = (List<Integer>) body.get("offerIds");
        if (offerIdInts == null || offerIdInts.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<Long> offerIds = offerIdInts.stream().map(Integer::longValue).toList();
        funnelAnalyticsService.trackImpression(userId, offerIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 记录点击事件（内部调用）。
     */
    @PostMapping("/internal/analytics/click")
    public ResponseEntity<Void> trackClick(@RequestBody Map<String, Object> body) {
        String userId = (String) body.getOrDefault("userId", "anonymous");
        Object offerIdObj = body.get("offerId");
        if (offerIdObj == null) {
            return ResponseEntity.badRequest().build();
        }
        Long offerId = offerIdObj instanceof Integer ? ((Integer) offerIdObj).longValue() : ((Number) offerIdObj).longValue();
        funnelAnalyticsService.trackClick(userId, offerId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取商家漏斗数据（管理端）。
     */
    @GetMapping("/admin/analytics/funnel")
    public ResponseEntity<MerchantFunnelAnalytics> getFunnel(
            @RequestParam Long merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        MerchantFunnelAnalytics funnel = funnelAnalyticsService.getDailyFunnel(merchantId, date);
        return ResponseEntity.ok(funnel);
    }

    /**
     * 获取排序解释。
     */
    @GetMapping("/discovery/ranking/explain/{offerId}")
    public ResponseEntity<RankingExplanation> explainRanking(
            @PathVariable Long offerId,
            @RequestParam String traceId) {
        RankingExplanation explanation = explainabilityService.explain(offerId, traceId);
        return ResponseEntity.ok(explanation);
    }
}
