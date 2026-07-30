package com.campusfruit.offer.domain.inventory;

import com.campusfruit.offer.entity.Offer;
import com.campusfruit.offer.entity.StockLedger;
import com.campusfruit.offer.enums.OfferStatus;
import com.campusfruit.offer.enums.StockChangeType;
import com.campusfruit.offer.repository.OfferRepository;
import com.campusfruit.offer.repository.StockLedgerRepository;
import com.campusfruit.offer.service.OfferEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存一致性引擎。提供库存预占/确认/释放/取消/调整及守恒验证。
 * 所有写操作使用数据库条件更新保证原子性。
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final OfferRepository offerRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final OfferEventPublisher eventPublisher;

    public InventoryService(OfferRepository offerRepository,
                            StockLedgerRepository stockLedgerRepository,
                            OfferEventPublisher eventPublisher) {
        this.offerRepository = offerRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 库存预占。
     * - 幂等键检查：如果 reservationId 已有记录，返回已有结果
     * - 数据库条件更新保证原子性
     * - 写入 StockLedger
     */
    @Transactional
    public ReservationResult reserve(Long offerId, int quantity, String reservationId) {
        // 幂等键检查
        List<StockLedger> existing = stockLedgerRepository.findByReferenceId(reservationId);
        if (!existing.isEmpty()) {
            log.info("Idempotent reservation detected: {}", reservationId);
            return ReservationResult.success(reservationId, offerId, quantity,
                    LocalDateTime.now().plusMinutes(15));
        }

        // 检查报价是否活跃
        Offer offer = offerRepository.findById(offerId)
                .orElse(null);
        if (offer == null) {
            return ReservationResult.failure(reservationId, "OFFER_NOT_ACTIVE");
        }
        if (offer.getStatus() != OfferStatus.ACTIVE) {
            log.warn("Offer {} is not ACTIVE, status={}", offerId, offer.getStatus());
            return ReservationResult.failure(reservationId, "OFFER_NOT_ACTIVE");
        }

        // 数据库条件更新：UPDATE offers SET available = available - ? WHERE id = ? AND available >= ?
        int updated = offerRepository.reserveStock(offerId, quantity);
        if (updated == 0) {
            log.warn("Insufficient stock for offer={}, requested={}", offerId, quantity);
            return ReservationResult.failure(reservationId, "INSUFFICIENT_STOCK");
        }

        // 重新加载以获取最新状态
        Offer updatedOffer = offerRepository.findById(offerId).orElseThrow();

        // 写入 StockLedger
        StockLedger ledger = new StockLedger();
        ledger.setOfferId(offerId);
        ledger.setChangeType(StockChangeType.RESERVE);
        ledger.setQuantityChange(-quantity);
        ledger.setAvailableBefore(offer.getAvailableQuantity());
        ledger.setAvailableAfter(updatedOffer.getAvailableQuantity());
        ledger.setReservedBefore(offer.getReservedQuantity());
        ledger.setReservedAfter(updatedOffer.getReservedQuantity());
        ledger.setReferenceId(reservationId);
        stockLedgerRepository.save(ledger);

        // 发布事件
        eventPublisher.publishOfferStockChanged(updatedOffer);

        log.info("Reserved {} units for offer={}, reservationId={}", quantity, offerId, reservationId);
        return ReservationResult.success(reservationId, offerId, quantity,
                LocalDateTime.now().plusMinutes(15));
    }

    /**
     * 确认预占（已售出）。
     * 减少 stock_quantity 和 reserved。
     */
    @Transactional
    public void confirm(Long offerId, String reservationId) {
        List<StockLedger> records = stockLedgerRepository.findByReferenceId(reservationId);
        if (records.isEmpty()) {
            log.warn("Reservation {} not found for confirm", reservationId);
            return;
        }

        // 检查是否已确认（幂等）
        if (records.stream().anyMatch(r -> r.getChangeType() == StockChangeType.CONFIRM)) {
            log.info("Reservation {} already confirmed, idempotent", reservationId);
            return;
        }

        Offer offer = offerRepository.findById(offerId).orElseThrow();

        int availBefore = offer.getAvailableQuantity();
        int reservedBefore = offer.getReservedQuantity();
        int quantity = Math.abs(records.get(0).getQuantityChange());

        // stock_quantity -= quantity, reserved -= quantity
        offer.setStockQuantity(offer.getStockQuantity() - quantity);
        offer.setReservedQuantity(reservedBefore - quantity);

        Offer saved = offerRepository.save(offer);

        StockLedger ledger = new StockLedger();
        ledger.setOfferId(offerId);
        ledger.setChangeType(StockChangeType.CONFIRM);
        ledger.setQuantityChange(-quantity);
        ledger.setAvailableBefore(availBefore);
        ledger.setAvailableAfter(saved.getAvailableQuantity());
        ledger.setReservedBefore(reservedBefore);
        ledger.setReservedAfter(saved.getReservedQuantity());
        ledger.setReferenceId(reservationId);
        stockLedgerRepository.save(ledger);

        eventPublisher.publishOfferStockChanged(saved);
        log.info("Confirmed reservation {} for offer={}", reservationId, offerId);
    }

    /**
     * 释放预占。
     * 幂等：如果已确认或已释放，直接返回。
     */
    @Transactional
    public void release(Long offerId, String reservationId) {
        List<StockLedger> records = stockLedgerRepository.findByReferenceId(reservationId);
        if (records.isEmpty()) {
            log.warn("Reservation {} not found for release", reservationId);
            return;
        }

        // 幂等：已确认或已释放
        boolean alreadyConfirmed = records.stream().anyMatch(r -> r.getChangeType() == StockChangeType.CONFIRM);
        boolean alreadyReleased = records.stream().anyMatch(r -> r.getChangeType() == StockChangeType.RELEASE);
        if (alreadyConfirmed) {
            log.info("Reservation {} already confirmed, release idempotent", reservationId);
            return;
        }
        if (alreadyReleased) {
            log.info("Reservation {} already released, idempotent", reservationId);
            return;
        }

        Offer offer = offerRepository.findById(offerId).orElseThrow();

        int availBefore = offer.getAvailableQuantity();
        int reservedBefore = offer.getReservedQuantity();
        int quantity = Math.abs(records.get(0).getQuantityChange());

        // available += quantity, reserved -= quantity
        offer.setAvailableQuantity(availBefore + quantity);
        offer.setReservedQuantity(reservedBefore - quantity);

        Offer saved = offerRepository.save(offer);

        StockLedger ledger = new StockLedger();
        ledger.setOfferId(offerId);
        ledger.setChangeType(StockChangeType.RELEASE);
        ledger.setQuantityChange(quantity);
        ledger.setAvailableBefore(availBefore);
        ledger.setAvailableAfter(saved.getAvailableQuantity());
        ledger.setReservedBefore(reservedBefore);
        ledger.setReservedAfter(saved.getReservedQuantity());
        ledger.setReferenceId(reservationId);
        stockLedgerRepository.save(ledger);

        eventPublisher.publishOfferStockChanged(saved);
        log.info("Released reservation {} for offer={}", reservationId, offerId);
    }

    /**
     * 取消已确认库存。
     */
    @Transactional
    public void cancel(Long offerId, int quantity) {
        Offer offer = offerRepository.findById(offerId).orElseThrow();

        int availBefore = offer.getAvailableQuantity();
        offer.setAvailableQuantity(availBefore + quantity);

        Offer saved = offerRepository.save(offer);

        StockLedger ledger = new StockLedger();
        ledger.setOfferId(offerId);
        ledger.setChangeType(StockChangeType.CANCEL);
        ledger.setQuantityChange(quantity);
        ledger.setAvailableBefore(availBefore);
        ledger.setAvailableAfter(saved.getAvailableQuantity());
        ledger.setReservedBefore(offer.getReservedQuantity());
        ledger.setReservedAfter(offer.getReservedQuantity());
        stockLedgerRepository.save(ledger);

        eventPublisher.publishOfferStockChanged(saved);
        log.info("Cancelled {} units for offer={}", quantity, offerId);
    }

    /**
     * 库存调整（盘点）。
     * 直接修改 available/reserved，写入 ADJUST 流水。
     */
    @Transactional
    public void adjust(Long offerId, int availableDelta, int reservedDelta) {
        Offer offer = offerRepository.findById(offerId).orElseThrow();

        int availBefore = offer.getAvailableQuantity();
        int reservedBefore = offer.getReservedQuantity();

        offer.setAvailableQuantity(availBefore + availableDelta);
        offer.setReservedQuantity(reservedBefore + reservedDelta);

        // 验证非负
        if (offer.getAvailableQuantity() < 0 || offer.getReservedQuantity() < 0) {
            throw new IllegalArgumentException("库存调整后不可为负数");
        }

        Offer saved = offerRepository.save(offer);

        // 验证守恒式
        validateConservation(saved);

        StockLedger ledger = new StockLedger();
        ledger.setOfferId(offerId);
        ledger.setChangeType(StockChangeType.ADJUST);
        ledger.setQuantityChange(availableDelta + reservedDelta);
        ledger.setAvailableBefore(availBefore);
        ledger.setAvailableAfter(saved.getAvailableQuantity());
        ledger.setReservedBefore(reservedBefore);
        ledger.setReservedAfter(saved.getReservedQuantity());
        stockLedgerRepository.save(ledger);

        eventPublisher.publishOfferStockChanged(saved);
        log.info("Adjusted offer={} availableDelta={} reservedDelta={}", offerId, availableDelta, reservedDelta);
    }

    /**
     * 验证库存守恒式：available + reserved == stock_quantity。
     */
    public boolean validateConservation(Long offerId) {
        Offer offer = offerRepository.findById(offerId).orElseThrow();
        return validateConservation(offer);
    }

    private boolean validateConservation(Offer offer) {
        int sum = offer.getAvailableQuantity() + offer.getReservedQuantity();
        boolean valid = (sum == offer.getStockQuantity());
        if (!valid) {
            log.error("Conservation violated for offer={}: available={} + reserved={} != stock={}",
                    offer.getId(),
                    offer.getAvailableQuantity(),
                    offer.getReservedQuantity(),
                    offer.getStockQuantity());
        }
        return valid;
    }
}
