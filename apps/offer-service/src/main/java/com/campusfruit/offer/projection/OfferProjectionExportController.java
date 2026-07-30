package com.campusfruit.offer.projection;

import com.campusfruit.offer.domain.price.PriceNormalizer;
import com.campusfruit.offer.entity.Offer;
import com.campusfruit.offer.repository.OfferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal/offer")
public class OfferProjectionExportController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final OfferRepository offerRepository;
    private final PriceNormalizer priceNormalizer;

    public OfferProjectionExportController(OfferRepository offerRepository,
                                           PriceNormalizer priceNormalizer) {
        this.offerRepository = offerRepository;
        this.priceNormalizer = priceNormalizer;
    }

    /**
     * 导出报价投影数据，分页返回。
     *
     * @param pageSize  每页大小（默认 20，最大 100）
     * @param nextToken 分页标记（Base64 编码的上一页最后 offerId）
     */
    @GetMapping("/projection/export")
    public ResponseEntity<ProjectionExportResponse> export(
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "nextToken", required = false) String nextToken) {

        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(0, pageSize);
        Page<Offer> offerPage;

        if (nextToken != null && !nextToken.isBlank()) {
            try {
                Long cursorId = Long.parseLong(new String(Base64.getDecoder().decode(nextToken)));
                offerPage = offerRepository.findByIdGreaterThan(cursorId, pageable);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            offerPage = offerRepository.findAllByOrderByIdAsc(pageable);
        }

        List<OfferProjection> data = offerPage.getContent().stream()
                .map(this::toProjection)
                .collect(Collectors.toList());

        boolean hasMore = offerPage.getNumberOfElements() >= pageSize;
        String newNextToken = null;
        if (hasMore && !offerPage.getContent().isEmpty()) {
            Long lastId = offerPage.getContent().get(offerPage.getContent().size() - 1).getId();
            newNextToken = Base64.getEncoder().encodeToString(String.valueOf(lastId).getBytes());
        }

        return ResponseEntity.ok(new ProjectionExportResponse(data, newNextToken, hasMore));
    }

    private OfferProjection toProjection(Offer offer) {
        OfferProjection proj = new OfferProjection();
        proj.setOfferId(offer.getId());
        proj.setStoreId(offer.getStoreId());
        if (offer.getCanonicalFruit() != null) {
            proj.setCanonicalFruitId(offer.getCanonicalFruit().getId());
            proj.setFruitCategory(offer.getCanonicalFruit().getCategory());
            proj.setFruitVariety(offer.getCanonicalFruit().getVariety());
            proj.setFruitGrade(offer.getCanonicalFruit().getGrade());
            proj.setFruitOrigin(offer.getCanonicalFruit().getOrigin());
        }
        proj.setSalesUnit(offer.getSalesUnit());
        proj.setNetWeightGrams(offer.getNetWeightGrams());
        proj.setUnitPrice(offer.getUnitPrice());

        // 使用 PriceNormalizer 计算标准价格
        priceNormalizer.normalize(
                Math.toIntExact(offer.getUnitPrice()),
                offer.getNetWeightGrams(),
                offer.getSalesUnit()
        ).ifPresent(sp -> proj.setStandardPricePer500g(sp.getStandardPricePer500g()));

        proj.setStockQuantity(offer.getStockQuantity());
        proj.setAvailableQuantity(offer.getAvailableQuantity());
        proj.setReservedQuantity(offer.getReservedQuantity());
        proj.setStatus(offer.getStatus().name());
        proj.setQualityDesc(offer.getQualityDesc());
        proj.setLastConfirmedAt(offer.getLastConfirmedAt());
        proj.setPriceStale(offer.getPriceStale() != null ? offer.getPriceStale() : false);
        proj.setCreatedAt(offer.getCreatedAt());
        proj.setUpdatedAt(offer.getUpdatedAt());
        return proj;
    }
}
