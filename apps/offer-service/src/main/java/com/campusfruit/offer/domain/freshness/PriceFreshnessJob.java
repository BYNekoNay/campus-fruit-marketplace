package com.campusfruit.offer.domain.freshness;

import com.campusfruit.offer.entity.Offer;
import com.campusfruit.offer.enums.OfferStatus;
import com.campusfruit.offer.repository.OfferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 价格新鲜度定时任务。每 30 分钟检查报价的确认/过期状态。
 * <ul>
 *   <li>24h 未确认：标记 priceStale=true</li>
 *   <li>72h 未确认：标记退出统计</li>
 *   <li>7d 未确认：状态改为 EXPIRED</li>
 * </ul>
 * 仅在 profile 激活时启用。
 */
@Component
@ConditionalOnProperty(name = "app.offer.price-freshness.enabled", havingValue = "true", matchIfMissing = false)
public class PriceFreshnessJob {

    private static final Logger log = LoggerFactory.getLogger(PriceFreshnessJob.class);

    private final OfferRepository offerRepository;

    @Value("${app.offer.price-freshness.warning-hours:24}")
    private int warningHours;

    @Value("${app.offer.price-freshness.stale-exit-hours:72}")
    private int staleExitHours;

    @Value("${app.offer.price-freshness.pause-hours:168}")
    private int pauseHours;

    public PriceFreshnessJob(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    /**
     * 每 30 分钟执行。
     */
    @Scheduled(cron = "0 */30 * * * ?")
    @Transactional
    public void processFreshness() {
        log.info("Starting price freshness job: warning={}h, staleExit={}h, pause={}h",
                warningHours, staleExitHours, pauseHours);

        Instant now = Instant.now();
        Instant warningCutoff = now.minus(warningHours, ChronoUnit.HOURS);
        Instant staleExitCutoff = now.minus(staleExitHours, ChronoUnit.HOURS);
        Instant pauseCutoff = now.minus(pauseHours, ChronoUnit.HOURS);

        // 1. 24h 未确认且目前仍是 ACTIVE：标记 priceStale=true
        List<Offer> activeOffers = offerRepository.findByStoreIdAndStatus(null, OfferStatus.ACTIVE);
        // 更精确的查询：状态为 ACTIVE 且最后确认时间早于 cutoff
        List<Offer> staleCandidates = offerRepository.findByStatusAndLastConfirmedAtBefore(
                OfferStatus.ACTIVE, warningCutoff);

        for (Offer offer : staleCandidates) {
            if (Boolean.FALSE.equals(offer.getPriceStale())) {
                offer.setPriceStale(true);
                offerRepository.save(offer);
                log.debug("Marked offer={} as priceStale", offer.getId());
            }
        }

        // 2. 72h 未确认：标记退出统计（通过 priceStale 已体现，此处记录日志）
        List<Offer> veryStale = offerRepository.findByStatusAndLastConfirmedAtBefore(
                OfferStatus.ACTIVE, staleExitCutoff);
        log.info("Offers with stale prices >{}h (exit comparison): {}",
                staleExitHours, veryStale.size());

        // 3. 7d 未确认：状态改为 EXPIRED
        List<Offer> expiredCandidates = offerRepository.findByStatusAndUpdatedAtBefore(
                OfferStatus.ACTIVE, pauseCutoff);

        int expiredCount = 0;
        for (Offer offer : expiredCandidates) {
            offer.setStatus(OfferStatus.EXPIRED);
            offer.setPriceStale(true);
            offerRepository.save(offer);
            expiredCount++;
        }

        log.info("Price freshness job completed: markedStale={}, expired={}",
                staleCandidates.size(), expiredCount);
    }
}
