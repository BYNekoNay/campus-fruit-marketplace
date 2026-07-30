package com.campusfruit.offer.domain.price;

import com.campusfruit.offer.entity.Offer;
import com.campusfruit.offer.repository.OfferRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 价格比较服务。计算同一标准水果下所有可比报价的统计值。
 */
@Service
public class PriceComparisonService {

    private final OfferRepository offerRepository;
    private final PriceNormalizer priceNormalizer;

    public PriceComparisonService(OfferRepository offerRepository, PriceNormalizer priceNormalizer) {
        this.offerRepository = offerRepository;
        this.priceNormalizer = priceNormalizer;
    }

    /**
     * 计算同一标准水果下所有可比报价的统计值。
     */
    public PriceComparisonResult compareByFruit(Long canonicalFruitId) {
        List<Offer> offers = offerRepository.findByCanonicalFruitId(canonicalFruitId);

        // 收集所有可比标准价格
        List<BigDecimal> comparablePrices = new ArrayList<>();
        for (Offer offer : offers) {
            priceNormalizer.normalize(
                    Math.toIntExact(offer.getUnitPrice()),
                    offer.getNetWeightGrams(),
                    offer.getSalesUnit()
            ).ifPresent(sp -> comparablePrices.add(sp.getStandardPricePer500g()));
        }

        // 排序用于计算中位数
        Collections.sort(comparablePrices);

        PriceComparisonResult result = new PriceComparisonResult();
        result.setParticipatingStores(comparablePrices.size());
        result.setSampleTime(Instant.now());

        if (comparablePrices.size() < 3) {
            result.setSampleInsufficient(true);
            return result;
        }

        result.setMinPrice(comparablePrices.get(0));
        result.setMaxPrice(comparablePrices.get(comparablePrices.size() - 1));
        result.setMedianPrice(calculateMedian(comparablePrices));
        result.setAvgPrice(calculateAverage(comparablePrices));
        result.setSampleInsufficient(false);

        return result;
    }

    private BigDecimal calculateMedian(List<BigDecimal> sortedPrices) {
        int size = sortedPrices.size();
        if (size % 2 == 1) {
            return sortedPrices.get(size / 2);
        } else {
            return sortedPrices.get(size / 2 - 1)
                    .add(sortedPrices.get(size / 2))
                    .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    private BigDecimal calculateAverage(List<BigDecimal> prices) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal price : prices) {
            sum = sum.add(price);
        }
        return sum.divide(BigDecimal.valueOf(prices.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 价格比较结果 VO
     */
    public static class PriceComparisonResult {
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private BigDecimal medianPrice;
        private BigDecimal avgPrice;
        private int participatingStores;
        private Instant sampleTime;
        private boolean sampleInsufficient;

        public BigDecimal getMinPrice() { return minPrice; }
        public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

        public BigDecimal getMaxPrice() { return maxPrice; }
        public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

        public BigDecimal getMedianPrice() { return medianPrice; }
        public void setMedianPrice(BigDecimal medianPrice) { this.medianPrice = medianPrice; }

        public BigDecimal getAvgPrice() { return avgPrice; }
        public void setAvgPrice(BigDecimal avgPrice) { this.avgPrice = avgPrice; }

        public int getParticipatingStores() { return participatingStores; }
        public void setParticipatingStores(int participatingStores) { this.participatingStores = participatingStores; }

        public Instant getSampleTime() { return sampleTime; }
        public void setSampleTime(Instant sampleTime) { this.sampleTime = sampleTime; }

        public boolean isSampleInsufficient() { return sampleInsufficient; }
        public void setSampleInsufficient(boolean sampleInsufficient) { this.sampleInsufficient = sampleInsufficient; }
    }
}
