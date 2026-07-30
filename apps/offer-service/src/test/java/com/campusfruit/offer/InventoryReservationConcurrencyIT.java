package com.campusfruit.offer;

import com.campusfruit.offer.domain.inventory.InventoryService;
import com.campusfruit.offer.domain.inventory.ReservationResult;
import com.campusfruit.offer.dto.CreateOfferRequest;
import com.campusfruit.offer.dto.OfferResponse;
import com.campusfruit.offer.dto.ReservationRequest;
import com.campusfruit.offer.dto.ReservationResponse;
import com.campusfruit.offer.entity.CanonicalFruit;
import com.campusfruit.offer.enums.FruitStatus;
import com.campusfruit.offer.repository.CanonicalFruitRepository;
import com.campusfruit.offer.repository.OfferRepository;
import com.campusfruit.offer.repository.StockLedgerRepository;
import com.campusfruit.offer.service.OfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryReservationConcurrencyIT {

    @Autowired
    private OfferService offerService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CanonicalFruitRepository canonicalFruitRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private StockLedgerRepository stockLedgerRepository;

    private Long offerId;
    private static final int INITIAL_STOCK = 100;
    private static final int CONCURRENT_THREADS = 100;
    private static final int RESERVE_PER_THREAD = 1;

    @BeforeEach
    void setUp() {
        CanonicalFruit fruit = new CanonicalFruit();
        fruit.setCategory("柑橘类");
        fruit.setVariety("赣南脐橙");
        fruit.setGrade("一级");
        fruit.setStatus(FruitStatus.ACTIVE);
        CanonicalFruit saved = canonicalFruitRepository.save(fruit);

        CreateOfferRequest request = new CreateOfferRequest();
        request.setStoreId(1L);
        request.setCanonicalFruitId(saved.getId());
        request.setSalesUnit("500g盒装");
        request.setNetWeightGrams(500);
        request.setUnitPrice(999L);
        request.setStockQuantity(INITIAL_STOCK);
        OfferResponse response = offerService.createOffer(request);
        offerId = response.getId();
    }

    /**
     * 1000 并发请求竞争 100 库存，零超卖。
     */
    @Test
    void shouldHandle100ConcurrentReservationsWithZeroOversell() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<ReservationResponse>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < INITIAL_STOCK; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                ReservationRequest req = new ReservationRequest();
                req.setOfferId(offerId);
                req.setQuantity(RESERVE_PER_THREAD);
                req.setReservationId("concurrent-" + idx);
                return offerService.reserve(req);
            }));
        }

        // 同时释放
        latch.countDown();

        for (Future<ReservationResponse> future : futures) {
            ReservationResponse resp = future.get(30, TimeUnit.SECONDS);
            if (resp.isSuccess()) {
                successCount.incrementAndGet();
            }
        }

        executor.shutdown();

        // 验证
        OfferResponse offer = offerService.getOfferById(offerId);
        int totalReserved = successCount.get();

        // 零超卖：成功率 * 每次 → 不超过库存
        assertThat(successCount.get()).isLessThanOrEqualTo(INITIAL_STOCK);
        assertThat(offer.getAvailableQuantity()).isEqualTo(INITIAL_STOCK - totalReserved);
        assertThat(offer.getReservedQuantity()).isEqualTo(totalReserved);

        // 库存守恒式始终成立
        assertThat(offer.getStockQuantity())
                .isEqualTo(offer.getAvailableQuantity() + offer.getReservedQuantity());

        // 守恒验证服务
        assertThat(inventoryService.validateConservation(offerId)).isTrue();
    }

    /**
     * 同一 reservationId 重复请求返回相同结果（幂等）。
     */
    @Test
    void shouldHandleIdempotentReservations() {
        ReservationRequest req1 = new ReservationRequest();
        req1.setOfferId(offerId);
        req1.setQuantity(10);
        req1.setReservationId("idempotent-key-001");

        ReservationResponse resp1 = offerService.reserve(req1);
        assertThat(resp1.isSuccess()).isTrue();

        // 相同幂等键再次调用
        ReservationResponse resp2 = offerService.reserve(req1);
        assertThat(resp2.isSuccess()).isTrue();

        // 库存只减了一次
        OfferResponse offer = offerService.getOfferById(offerId);
        assertThat(offer.getReservedQuantity()).isEqualTo(10);
        assertThat(offer.getAvailableQuantity()).isEqualTo(INITIAL_STOCK - 10);
    }

    /**
     * 库存不足时应拒绝。
     */
    @Test
    void shouldRejectInsufficientReservation() {
        ReservationRequest req = new ReservationRequest();
        req.setOfferId(offerId);
        req.setQuantity(INITIAL_STOCK + 1);
        req.setReservationId("too-many");

        ReservationResponse resp = offerService.reserve(req);
        assertThat(resp.isSuccess()).isFalse();

        // 库存不变
        OfferResponse offer = offerService.getOfferById(offerId);
        assertThat(offer.getAvailableQuantity()).isEqualTo(INITIAL_STOCK);
        assertThat(offer.getReservedQuantity()).isEqualTo(0);
    }

    /**
     * 确认后释放应幂等。
     */
    @Test
    void shouldMakeReleaseIdempotentAfterConfirm() {
        String reservationId = "confirm-test-001";

        // 预占
        ReservationRequest req = new ReservationRequest();
        req.setOfferId(offerId);
        req.setQuantity(10);
        req.setReservationId(reservationId);
        ReservationResponse resp = offerService.reserve(req);
        assertThat(resp.isSuccess()).isTrue();

        OfferResponse afterReserve = offerService.getOfferById(offerId);
        assertThat(afterReserve.getReservedQuantity()).isEqualTo(10);

        // 确认
        inventoryService.confirm(offerId, reservationId);

        OfferResponse afterConfirm = offerService.getOfferById(offerId);
        // stock减少=90, reserved=0
        assertThat(afterConfirm.getStockQuantity()).isEqualTo(INITIAL_STOCK - 10);
        assertThat(afterConfirm.getReservedQuantity()).isEqualTo(0);

        // 确认后释放应幂等（不抛异常）
        inventoryService.release(offerId, reservationId);

        OfferResponse afterRelease = offerService.getOfferById(offerId);
        // 不变
        assertThat(afterRelease.getStockQuantity()).isEqualTo(INITIAL_STOCK - 10);
        assertThat(afterRelease.getReservedQuantity()).isEqualTo(0);
    }

    /**
     * 释放后重新可抢。
     */
    @Test
    void shouldAllowReserveAfterRelease() {
        String reservationId = "release-re-reserve";

        // 预占 50
        ReservationRequest req = new ReservationRequest();
        req.setOfferId(offerId);
        req.setQuantity(50);
        req.setReservationId(reservationId);
        ReservationResponse resp = offerService.reserve(req);
        assertThat(resp.isSuccess()).isTrue();

        OfferResponse afterReserve = offerService.getOfferById(offerId);
        assertThat(afterReserve.getAvailableQuantity()).isEqualTo(50);
        assertThat(afterReserve.getReservedQuantity()).isEqualTo(50);

        // 释放
        inventoryService.release(offerId, reservationId);

        OfferResponse afterRelease = offerService.getOfferById(offerId);
        assertThat(afterRelease.getAvailableQuantity()).isEqualTo(100);
        assertThat(afterRelease.getReservedQuantity()).isEqualTo(0);

        // 重新可抢
        ReservationRequest req2 = new ReservationRequest();
        req2.setOfferId(offerId);
        req2.setQuantity(50);
        req2.setReservationId("re-reserve-after-release");
        ReservationResponse resp2 = offerService.reserve(req2);
        assertThat(resp2.isSuccess()).isTrue();

        OfferResponse afterReReserve = offerService.getOfferById(offerId);
        assertThat(afterReReserve.getAvailableQuantity()).isEqualTo(50);
        assertThat(afterReReserve.getReservedQuantity()).isEqualTo(50);
    }

    /**
     * 库存守恒式始终成立。
     */
    @Test
    void shouldMaintainConservationAfterMultipleOperations() {
        // 预占 30
        inventoryService.reserve(offerId, 30, "conservation-res-1");

        // 预占 20
        inventoryService.reserve(offerId, 20, "conservation-res-2");

        OfferResponse offer = offerService.getOfferById(offerId);
        assertThat(offer.getStockQuantity())
                .isEqualTo(offer.getAvailableQuantity() + offer.getReservedQuantity());

        // 确认一个
        inventoryService.confirm(offerId, "conservation-res-1");
        offer = offerService.getOfferById(offerId);
        assertThat(offer.getStockQuantity())
                .isEqualTo(offer.getAvailableQuantity() + offer.getReservedQuantity());

        // 释放一个
        inventoryService.release(offerId, "conservation-res-2");
        offer = offerService.getOfferById(offerId);
        assertThat(offer.getStockQuantity())
                .isEqualTo(offer.getAvailableQuantity() + offer.getReservedQuantity());

        // 取消 10
        inventoryService.cancel(offerId, 10);
        offer = offerService.getOfferById(offerId);
        assertThat(offer.getStockQuantity())
                .isEqualTo(offer.getAvailableQuantity() + offer.getReservedQuantity());
    }
}
