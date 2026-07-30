package com.campusfruit.offer;

import com.campusfruit.offer.dto.CreateOfferRequest;
import com.campusfruit.offer.dto.OfferResponse;
import com.campusfruit.offer.dto.UpdateOfferRequest;
import com.campusfruit.offer.entity.CanonicalFruit;
import com.campusfruit.offer.entity.Offer;
import com.campusfruit.offer.entity.PriceHistory;
import com.campusfruit.offer.enums.FruitStatus;
import com.campusfruit.offer.repository.*;
import com.campusfruit.offer.service.OfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OfferLifecycleIT {

    @Autowired
    private OfferService offerService;

    @Autowired
    private CanonicalFruitRepository canonicalFruitRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    private Long fruitId;
    private Long storeId;

    @BeforeEach
    void setUp() {
        CanonicalFruit fruit = new CanonicalFruit();
        fruit.setCategory("柑橘类");
        fruit.setVariety("赣南脐橙");
        fruit.setGrade("一级");
        fruit.setOrigin("江西赣州");
        fruit.setStatus(FruitStatus.ACTIVE);
        CanonicalFruit saved = canonicalFruitRepository.save(fruit);
        fruitId = saved.getId();
        storeId = 1L;
    }

    @Test
    void shouldCreateOfferSuccessfully() {
        CreateOfferRequest request = new CreateOfferRequest();
        request.setStoreId(storeId);
        request.setCanonicalFruitId(fruitId);
        request.setSalesUnit("500g盒装");
        request.setNetWeightGrams(500);
        request.setUnitPrice(999L); // 9.99元
        request.setStockQuantity(100);
        request.setQualityDesc("新鲜采摘，口感香甜");

        OfferResponse response = offerService.createOffer(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStoreId()).isEqualTo(storeId);
        assertThat(response.getUnitPrice()).isEqualTo(999L);
        assertThat(response.getStockQuantity()).isEqualTo(100);
        assertThat(response.getAvailableQuantity()).isEqualTo(100);
        assertThat(response.getReservedQuantity()).isEqualTo(0);
        assertThat(response.getNetWeightGrams()).isEqualTo(500);
        assertThat(response.getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("9.99"));

        // 验证标准价格计算：单位价 / 净重 * 500 = 999/500*500 = 999
        // 验证库存流水
        List<Offer> offers = offerRepository.findByStoreId(storeId);
        assertThat(offers).hasSize(1);
    }

    @Test
    void shouldUpdateOfferAndRecordPriceHistory() {
        // 创建报价
        CreateOfferRequest createReq = new CreateOfferRequest();
        createReq.setStoreId(storeId);
        createReq.setCanonicalFruitId(fruitId);
        createReq.setSalesUnit("500g盒装");
        createReq.setNetWeightGrams(500);
        createReq.setUnitPrice(999L);
        createReq.setStockQuantity(100);
        OfferResponse created = offerService.createOffer(createReq);

        // 更新价格
        UpdateOfferRequest updateReq = new UpdateOfferRequest();
        updateReq.setUnitPrice(1299L);
        OfferResponse updated = offerService.updateOffer(created.getId(), updateReq);

        assertThat(updated.getUnitPrice()).isEqualTo(1299L);

        // 验证价格历史
        List<PriceHistory> history = priceHistoryRepository.findByOfferIdOrderByChangedAtDesc(created.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getUnitPrice()).isEqualTo(1299L);
    }

    @Test
    void shouldConfirmPriceWithoutCreatingHistory() {
        CreateOfferRequest createReq = new CreateOfferRequest();
        createReq.setStoreId(storeId);
        createReq.setCanonicalFruitId(fruitId);
        createReq.setSalesUnit("500g盒装");
        createReq.setNetWeightGrams(500);
        createReq.setUnitPrice(999L);
        createReq.setStockQuantity(100);
        OfferResponse created = offerService.createOffer(createReq);

        OfferResponse confirmed = offerService.confirmPrice(created.getId());

        assertThat(confirmed.getLastConfirmedAt()).isNotNull();

        // 验证没有新增价格历史
        List<PriceHistory> history = priceHistoryRepository.findByOfferIdOrderByChangedAtDesc(created.getId());
        assertThat(history).isEmpty();
    }

    @Test
    void shouldPauseAndActivateOffer() {
        CreateOfferRequest createReq = new CreateOfferRequest();
        createReq.setStoreId(storeId);
        createReq.setCanonicalFruitId(fruitId);
        createReq.setSalesUnit("500g盒装");
        createReq.setNetWeightGrams(500);
        createReq.setUnitPrice(999L);
        createReq.setStockQuantity(100);
        OfferResponse created = offerService.createOffer(createReq);

        // 暂停
        OfferResponse paused = offerService.pauseOffer(created.getId());
        assertThat(paused.getStatus()).isEqualTo("PAUSED");

        // 激活
        OfferResponse activated = offerService.activateOffer(created.getId());
        assertThat(activated.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldUpdateStockQuantityCorrectly() {
        CreateOfferRequest createReq = new CreateOfferRequest();
        createReq.setStoreId(storeId);
        createReq.setCanonicalFruitId(fruitId);
        createReq.setSalesUnit("500g盒装");
        createReq.setNetWeightGrams(500);
        createReq.setUnitPrice(999L);
        createReq.setStockQuantity(100);
        OfferResponse created = offerService.createOffer(createReq);

        // 增加库存
        UpdateOfferRequest updateReq = new UpdateOfferRequest();
        updateReq.setStockQuantity(150);
        OfferResponse updated = offerService.updateOffer(created.getId(), updateReq);

        assertThat(updated.getStockQuantity()).isEqualTo(150);
        assertThat(updated.getAvailableQuantity()).isEqualTo(150);
    }

}
