package com.campusfruit.discovery.ranking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人工干预服务 — 管理员降权/排除，需带原因、审批、最长 7 天有效期、可撤销。
 * 当前为本地内存实现，生产环境应持久化到数据库。
 */
@Service
public class InterventionService {
    private static final Logger log = LoggerFactory.getLogger(InterventionService.class);

    /** storeId → {action, reason, approvedBy, validUntil, createdAt} */
    private final ConcurrentHashMap<Long, Map<String, Object>> interventions = new ConcurrentHashMap<>();

    public void applyDowngrade(Long storeId, String reason, String approvedBy) {
        interventions.put(storeId, Map.of(
            "action", "DOWNGRADE",
            "reason", reason,
            "approvedBy", approvedBy,
            "validUntil", Instant.now().plusSeconds(7 * 86400),
            "createdAt", Instant.now()
        ));
        log.warn("人工降权: store={}, by={}, reason={}", storeId, approvedBy, reason);
    }

    public void applyExclusion(Long storeId, String reason, String approvedBy) {
        interventions.put(storeId, Map.of(
            "action", "EXCLUSION",
            "reason", reason,
            "approvedBy", approvedBy,
            "validUntil", Instant.now().plusSeconds(7 * 86400),
            "createdAt", Instant.now()
        ));
        log.warn("人工排除: store={}, by={}, reason={}", storeId, approvedBy, reason);
    }

    public void revoke(Long storeId, String revokedBy) {
        if (interventions.remove(storeId) != null) {
            log.info("撤销干预: store={}, by={}", storeId, revokedBy);
        }
    }

    public boolean isExcluded(Long storeId) {
        Map<String, Object> intervention = interventions.get(storeId);
        if (intervention == null) return false;
        Instant validUntil = (Instant) intervention.get("validUntil");
        if (validUntil != null && validUntil.isBefore(Instant.now())) {
            interventions.remove(storeId);
            return false;
        }
        return "EXCLUSION".equals(intervention.get("action"));
    }

    public double getDowngradeFactor(Long storeId) {
        Map<String, Object> intervention = interventions.get(storeId);
        if (intervention == null || !"DOWNGRADE".equals(intervention.get("action"))) return 1.0;
        Instant validUntil = (Instant) intervention.get("validUntil");
        if (validUntil != null && validUntil.isBefore(Instant.now())) {
            interventions.remove(storeId);
            return 1.0;
        }
        return 0.5; // 降权50%
    }

    public Map<String, Object> getIntervention(Long storeId) {
        return interventions.get(storeId);
    }
}
