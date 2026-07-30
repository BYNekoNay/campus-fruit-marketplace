package com.campusfruit.offer.service;

import com.campusfruit.offer.domain.inventory.InventoryService;
import com.campusfruit.offer.domain.inventory.ReservationResult;
import com.campusfruit.offer.domain.price.PriceNormalizer;
import com.campusfruit.offer.dto.*;
import com.campusfruit.offer.entity.*;
import com.campusfruit.offer.enums.OfferStatus;
import com.campusfruit.offer.enums.StockChangeType;
import com.campusfruit.offer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private static final Logger log = LoggerFactory.getLogger(OfferService.class);

    private final OfferRepository offerRepository;
    private final CanonicalFruitRepository canonicalFruitRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final OfferEventPublisher eventPublisher;
    private final PriceNormalizer priceNormalizer;
    private final InventoryService inventoryService;

    public OfferService(OfferRepository offerRepository,
                        CanonicalFruitRepository canonicalFruitRepository,
                        PriceHistoryRepository priceHistoryRepository,
                        StockLedgerRepository stockLedgerRepository,
                        OfferEventPublisher eventPublisher,
                        PriceNormalizer priceNormalizer,
                        InventoryService inventoryService) {
        this.offerRepository = offerRepository;
        this.canonicalFruitRepository = canonicalFruitRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.stockLedgerRepository = stockLedgerRepository;
        this.eventPublisher = eventPublisher;
        this.priceNormalizer = priceNormalizer;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public OfferResponse createOffer(CreateOfferRequest request) {
        CanonicalFruit fruit = canonicalFruitRepository.findById(request.getCanonicalFruitId())
                .orElseThrow(() -> new IllegalArgumentException("标准水果不存在: " + request.getCanonicalFruitId()));

        Offer offer = new Offer();
        offer.setStoreId(request.getStoreId());
        offer.setCanonicalFruit(fruit);
        offer.setSalesUnit(request.getSalesUnit());
        offer.setNetWeightGrams(request.getNetWeightGrams());
        offer.setUnitPrice(request.getUnitPrice());
        offer.setStockQuantity(request.getStockQuantity());
        offer.setAvailableQuantity(request.getStockQuantity());
        offer.setReservedQuantity(0);
        offer.setQualityDesc(request.getQualityDesc());
        offer.setStatus(OfferStatus.ACTIVE);

        Offer saved = offerRepository.save(offer);

        // 记录初始库存流水
        createStockLedger(saved, StockChangeType.INITIAL, request.getStockQuantity(),
                0, request.getStockQuantity(), 0, 0, null);

        // 发布事件
        eventPublisher.publishOfferCreated(saved);

        log.info("Created offer id={} for store={} fruit={}", saved.getId(), request.getStoreId(), request.getCanonicalFruitId());
        return toResponse(saved);
    }

    @Transactional
    public OfferResponse updateOffer(Long id, UpdateOfferRequest request) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报价不存在: " + id));

        // 记录旧价格用于事件
        Long oldPrice = offer.getUnitPrice();

        boolean priceChanged = false;
        if (request.getUnitPrice() != null && !request.getUnitPrice().equals(offer.getUnitPrice())) {
            priceChanged = true;
            // 价格变更时追加 PriceHistory，不可覆盖
            PriceHistory history = new PriceHistory();
            history.setOfferId(offer.getId());
            history.setUnitPrice(request.getUnitPrice());
            history.setNetWeightGrams(request.getNetWeightGrams() != null ? request.getNetWeightGrams() : offer.getNetWeightGrams());
            history.setSalesUnit(request.getSalesUnit() != null ? request.getSalesUnit() : offer.getSalesUnit());
            priceHistoryRepository.save(history);

            offer.setUnitPrice(request.getUnitPrice());
        }

        if (request.getSalesUnit() != null) {
            offer.setSalesUnit(request.getSalesUnit());
        }
        if (request.getNetWeightGrams() != null) {
            offer.setNetWeightGrams(request.getNetWeightGrams());
        }
        if (request.getStockQuantity() != null) {
            int oldStock = offer.getStockQuantity();
            int diff = request.getStockQuantity() - oldStock;
            offer.setStockQuantity(request.getStockQuantity());
            offer.setAvailableQuantity(offer.getAvailableQuantity() + diff);
            // 非负检查
            if (offer.getAvailableQuantity() < 0) {
                offer.setAvailableQuantity(0);
            }
        }
        if (request.getQualityDesc() != null) {
            offer.setQualityDesc(request.getQualityDesc());
        }

        Offer saved = offerRepository.save(offer);

        if (priceChanged) {
            eventPublisher.publishOfferPriceChanged(saved, oldPrice);
        }

        return toResponse(saved);
    }

    @Transactional
    public OfferResponse confirmPrice(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报价不存在: " + id));

        offer.setLastConfirmedAt(Instant.now());
        Offer saved = offerRepository.save(offer);

        return toResponse(saved);
    }

    public OfferResponse getOfferById(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报价不存在: " + id));
        return toResponse(offer);
    }

    public List<OfferResponse> getOffersByStore(Long storeId) {
        return offerRepository.findByStoreId(storeId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<OfferResponse> getOffersByFruit(Long fruitId) {
        return offerRepository.findByCanonicalFruitId(fruitId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OfferResponse pauseOffer(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报价不存在: " + id));
        offer.setStatus(OfferStatus.PAUSED);
        Offer saved = offerRepository.save(offer);

        eventPublisher.publishOfferStatusChanged(saved);
        return toResponse(saved);
    }

    @Transactional
    public OfferResponse activateOffer(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报价不存在: " + id));
        offer.setStatus(OfferStatus.ACTIVE);
        Offer saved = offerRepository.save(offer);

        eventPublisher.publishOfferStatusChanged(saved);
        return toResponse(saved);
    }

    public List<PriceHistory> getPriceHistory(Long offerId) {
        return priceHistoryRepository.findByOfferIdOrderByChangedAtDesc(offerId);
    }

    @Transactional
    public ReservationResponse reserve(ReservationRequest request) {
        ReservationResult result = inventoryService.reserve(
                request.getOfferId(), request.getQuantity(), request.getReservationId());

        if (result.isSuccess()) {
            return new ReservationResponse(true, request.getReservationId(),
                    result.getExpiresAt() != null ? result.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null);
        } else {
            return new ReservationResponse(false, request.getReservationId(), null);
        }
    }

    private void createStockLedger(Offer offer, StockChangeType changeType, int quantityChange,
                                   Integer availBefore, Integer availAfter,
                                   Integer reservedBefore, Integer reservedAfter,
                                   String referenceId) {
        StockLedger ledger = new StockLedger();
        ledger.setOfferId(offer.getId());
        ledger.setChangeType(changeType);
        ledger.setQuantityChange(quantityChange);
        ledger.setAvailableBefore(availBefore);
        ledger.setAvailableAfter(availAfter);
        ledger.setReservedBefore(reservedBefore);
        ledger.setReservedAfter(reservedAfter);
        ledger.setReferenceId(referenceId);
        stockLedgerRepository.save(ledger);
    }

    private OfferResponse toResponse(Offer offer) {
        OfferResponse resp = new OfferResponse();
        resp.setId(offer.getId());
        resp.setStoreId(offer.getStoreId());
        resp.setCanonicalFruitId(offer.getCanonicalFruit().getId());
        resp.setFruitCategory(offer.getCanonicalFruit().getCategory());
        resp.setFruitVariety(offer.getCanonicalFruit().getVariety());
        resp.setFruitGrade(offer.getCanonicalFruit().getGrade());
        resp.setSalesUnit(offer.getSalesUnit());
        resp.setNetWeightGrams(offer.getNetWeightGrams());
        resp.setUnitPrice(offer.getUnitPrice());

        // 使用 PriceNormalizer 计算标准价格（BigDecimal）
        priceNormalizer.normalize(
                Math.toIntExact(offer.getUnitPrice()),
                offer.getNetWeightGrams(),
                offer.getSalesUnit()
        ).ifPresent(sp -> {
            resp.setStandardPricePer500g(sp.getStandardPricePer500g());
            resp.setStandardPricePerKg(sp.getStandardPricePerKg());
        });

        resp.setStockQuantity(offer.getStockQuantity());
        resp.setAvailableQuantity(offer.getAvailableQuantity());
        resp.setReservedQuantity(offer.getReservedQuantity());
        resp.setStatus(offer.getStatus().name());
        resp.setQualityDesc(offer.getQualityDesc());
        resp.setLastConfirmedAt(offer.getLastConfirmedAt());

        // 使用实体上的 priceStale 标志
        resp.setPriceStale(offer.getPriceStale() != null ? offer.getPriceStale() : false);

        resp.setCreatedAt(offer.getCreatedAt());
        resp.setUpdatedAt(offer.getUpdatedAt());
        return resp;
    }
}
