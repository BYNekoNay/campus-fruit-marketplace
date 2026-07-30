package com.campusfruit.offer.domain.inventory;

import com.campusfruit.offer.entity.StockLedger;
import com.campusfruit.offer.enums.StockChangeType;
import com.campusfruit.offer.repository.StockLedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 过期预占自动释放任务。每 60 秒扫描一次，释放所有过期的预占记录。
 * 使用 SKIP LOCKED 避免多实例重复处理。
 */
@Component
@ConditionalOnProperty(name = "app.offer.reservation-expiry.enabled", havingValue = "true", matchIfMissing = false)
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final JdbcTemplate jdbcTemplate;
    private final InventoryService inventoryService;
    private final StockLedgerRepository stockLedgerRepository;

    public ReservationExpiryJob(JdbcTemplate jdbcTemplate,
                                InventoryService inventoryService,
                                StockLedgerRepository stockLedgerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryService = inventoryService;
        this.stockLedgerRepository = stockLedgerRepository;
    }

    /**
     * 每 60 秒扫描并释放过期预占。
     */
    @Scheduled(fixedDelay = 60000)
    public void releaseExpiredReservations() {
        // 15 分钟前
        Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);

        // 使用 SKIP LOCKED 获取一条需要处理的 RESERVE 记录
        List<StockLedger> expired = findExpiredReservations(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        for (StockLedger ledger : expired) {
            try {
                String referenceId = ledger.getReferenceId();
                Long offerId = ledger.getOfferId();
                log.info("Releasing expired reservation {} for offer={}", referenceId, offerId);
                inventoryService.release(offerId, referenceId);
            } catch (Exception e) {
                log.error("Failed to release expired reservation ledgerId={}", ledger.getId(), e);
            }
        }
    }

    /**
     * 查询过期的预留记录（未确认/未释放），使用 SKIP LOCKED。
     */
    private List<StockLedger> findExpiredReservations(Instant cutoff) {
        // 查找 RESERVE 类型的记录，且未随后被 CONFIRM/RELEASE
        // 即：存在 RESERVE 记录但不存在相同 reference_id 的 CONFIRM/RELEASE 记录，且创建时间早于 cutoff
        String sql = """
                SELECT sl.* FROM stock_ledger sl
                WHERE sl.change_type = 'RESERVE'
                AND sl.created_at < ?
                AND sl.reference_id IS NOT NULL
                AND NOT EXISTS (
                    SELECT 1 FROM stock_ledger sl2
                    WHERE sl2.reference_id = sl.reference_id
                    AND sl2.change_type IN ('CONFIRM', 'RELEASE')
                )
                ORDER BY sl.created_at ASC
                LIMIT 100
                FOR UPDATE SKIP LOCKED
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                StockLedger ledger = new StockLedger();
                ledger.setId(rs.getLong("id"));
                ledger.setOfferId(rs.getLong("offer_id"));
                ledger.setChangeType(StockChangeType.valueOf(rs.getString("change_type")));
                ledger.setQuantityChange(rs.getInt("quantity_change"));
                ledger.setReferenceId(rs.getString("reference_id"));
                ledger.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                if (rs.getObject("available_before") != null) {
                    ledger.setAvailableBefore(rs.getInt("available_before"));
                    ledger.setAvailableAfter(rs.getInt("available_after"));
                }
                if (rs.getObject("reserved_before") != null) {
                    ledger.setReservedBefore(rs.getInt("reserved_before"));
                    ledger.setReservedAfter(rs.getInt("reserved_after"));
                }
                return ledger;
            }, cutoff);
        } catch (Exception e) {
            log.error("Failed to query expired reservations", e);
            return List.of();
        }
    }
}
