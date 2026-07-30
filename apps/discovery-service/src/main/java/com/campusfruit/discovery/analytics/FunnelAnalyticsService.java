package com.campusfruit.discovery.analytics;

import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * 商家漏斗分析服务。
 *
 * 使用内存存储（24h 匿名 session），生产环境应替换为 Redis/DB。
 * 不使用精确坐标，session 级别匿名。
 */
@Service
public class FunnelAnalyticsService {

    private final StoreOfferProjectionRepository projectionRepository;

    /** sessionId -> 曝光 offerId 集合 */
    private final Map<String, Set<Long>> impressionSessions = new ConcurrentHashMap<>();

    /** sessionId -> 点击 offerId 集合 */
    private final Map<String, Set<Long>> clickSessions = new ConcurrentHashMap<>();

    /** sessionId -> 下单 offerId 集合 */
    private final Map<String, Set<Long>> orderSessions = new ConcurrentHashMap<>();

    /** 记录时间戳用于过期清理 */
    private final Map<String, Long> sessionTimestamps = new ConcurrentHashMap<>();
    private static final long SESSION_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours

    public FunnelAnalyticsService(StoreOfferProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    /**
     * 记录曝光事件。
     *
     * @param userId      用户 ID（匿名化后作为 session key）
     * @param offerIdList 曝光的 offer ID 列表
     */
    public void trackImpression(String userId, List<Long> offerIdList) {
        cleanExpiredSessions();
        String sessionKey = sanitizeSessionKey(userId);
        Set<Long> offerIds = impressionSessions.computeIfAbsent(sessionKey, k -> new CopyOnWriteArraySet<>());
        offerIds.addAll(offerIdList);
        sessionTimestamps.put(sessionKey, System.currentTimeMillis());
    }

    /**
     * 记录点击事件。
     *
     * @param userId  用户 ID
     * @param offerId 点击的 offer ID
     */
    public void trackClick(String userId, Long offerId) {
        cleanExpiredSessions();
        String sessionKey = sanitizeSessionKey(userId);
        Set<Long> offerIds = clickSessions.computeIfAbsent(sessionKey, k -> new CopyOnWriteArraySet<>());
        offerIds.add(offerId);
        sessionTimestamps.put(sessionKey, System.currentTimeMillis());
    }

    /**
     * 记录下单事件。
     */
    public void trackOrder(String userId, Long offerId) {
        cleanExpiredSessions();
        String sessionKey = sanitizeSessionKey(userId);
        Set<Long> offerIds = orderSessions.computeIfAbsent(sessionKey, k -> new CopyOnWriteArraySet<>());
        offerIds.add(offerId);
        sessionTimestamps.put(sessionKey, System.currentTimeMillis());
    }

    /**
     * 获取商家日级漏斗数据。
     *
     * @param merchantId 商家 ID
     * @param date       日期
     * @return 漏斗分析数据
     */
    public MerchantFunnelAnalytics getDailyFunnel(Long merchantId, LocalDate date) {
        cleanExpiredSessions();

        // 获取该商家下的所有 offerId
        List<StoreOfferProjection> projections = projectionRepository.findAll().stream()
                .filter(p -> p.getMerchantId() != null && p.getMerchantId().equals(merchantId))
                .toList();

        Set<Long> merchantOfferIds = projections.stream()
                .map(StoreOfferProjection::getOfferId)
                .collect(Collectors.toSet());

        if (merchantOfferIds.isEmpty()) {
            return new MerchantFunnelAnalytics(0L, 0L, 0L, date);
        }

        long impressionCount = countIntersections(impressionSessions, merchantOfferIds);
        long clickCount = countIntersections(clickSessions, merchantOfferIds);
        long orderCount = countIntersections(orderSessions, merchantOfferIds);

        return new MerchantFunnelAnalytics(impressionCount, clickCount, orderCount, date);
    }

    /**
     * 统计所有 session 中与商家 offerIds 有交集的条目数。
     */
    private long countIntersections(Map<String, Set<Long>> sessions, Set<Long> merchantOfferIds) {
        long count = 0;
        for (Set<Long> offerIds : sessions.values()) {
            for (Long id : offerIds) {
                if (merchantOfferIds.contains(id)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 清理过期 session（24小时）。
     */
    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        List<String> expiredKeys = sessionTimestamps.entrySet().stream()
                .filter(e -> now - e.getValue() > SESSION_TTL_MS)
                .map(Map.Entry::getKey)
                .toList();

        for (String key : expiredKeys) {
            impressionSessions.remove(key);
            clickSessions.remove(key);
            orderSessions.remove(key);
            sessionTimestamps.remove(key);
        }
    }

    /**
     * 匿名化 session key：hash 处理用户 ID。
     */
    private String sanitizeSessionKey(String userId) {
        if (userId == null || userId.isBlank()) {
            return "anon-" + Instant.now().toEpochMilli();
        }
        return "session-" + userId.hashCode();
    }
}
