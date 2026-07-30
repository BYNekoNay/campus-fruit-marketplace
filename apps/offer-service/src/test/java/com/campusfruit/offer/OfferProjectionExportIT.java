package com.campusfruit.offer;

import com.campusfruit.offer.dto.CreateOfferRequest;
import com.campusfruit.offer.dto.OfferResponse;
import com.campusfruit.offer.entity.CanonicalFruit;
import com.campusfruit.offer.entity.Offer;
import com.campusfruit.offer.enums.FruitStatus;
import com.campusfruit.offer.repository.CanonicalFruitRepository;
import com.campusfruit.offer.repository.OfferRepository;
import com.campusfruit.offer.service.OfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OfferProjectionExportIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OfferService offerService;

    @Autowired
    private CanonicalFruitRepository canonicalFruitRepository;

    @Autowired
    private OfferRepository offerRepository;

    @BeforeEach
    void setUp() {
        CanonicalFruit fruit = new CanonicalFruit();
        fruit.setCategory("柑橘类");
        fruit.setVariety("赣南脐橙");
        fruit.setGrade("一级");
        fruit.setStatus(FruitStatus.ACTIVE);
        CanonicalFruit saved = canonicalFruitRepository.save(fruit);

        // Create multiple offers for pagination testing
        for (int i = 0; i < 25; i++) {
            CreateOfferRequest request = new CreateOfferRequest();
            request.setStoreId((long) (i % 5 + 1));
            request.setCanonicalFruitId(saved.getId());
            request.setSalesUnit("500g盒装");
            request.setNetWeightGrams(500);
            request.setUnitPrice(999L + i * 100);
            request.setStockQuantity(100);
            offerService.createOffer(request);
        }
    }

    @Test
    void shouldExportOffersWithCursorPagination() throws Exception {
        // 第一页
        mockMvc.perform(get("/api/internal/offer/projection/export")
                        .header("X-Internal-API-Key", "test-api-key")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextToken").isNotEmpty());
    }

    @Test
    void shouldExportOffersWithoutToken() throws Exception {
        mockMvc.perform(get("/api/internal/offer/projection/export")
                        .header("X-Internal-API-Key", "test-api-key")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.hasMore").isBoolean())
                .andExpect(jsonPath("$.data[0].offerId").isNumber())
                .andExpect(jsonPath("$.data[0].standardPricePer500g").isNumber());
    }

    @Test
    void shouldRejectInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/internal/offer/projection/export")
                        .header("X-Internal-API-Key", "wrong-key")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/internal/offer/projection/export")
                        .header("X-Internal-API-Key", "test-api-key")
                        .param("nextToken", "invalid-base64===")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest());
    }
}
