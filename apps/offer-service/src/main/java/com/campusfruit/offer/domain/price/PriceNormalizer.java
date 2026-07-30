package com.campusfruit.offer.domain.price;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 价格标准化引擎。将报价统一换算为每500克价格。
 * 使用 BigDecimal 避免浮点精度问题。
 */
@Component
public class PriceNormalizer {

    private static final BigDecimal FIVE_HUNDRED = BigDecimal.valueOf(500);

    /**
     * 将报价统一换算为"每500克价格"。
     *
     * @param unitPrice      单位价格（分）
     * @param netWeightGrams 净重（克）
     * @param salesUnit      销售单位
     * @return 标准价格，不可比时返回空 Optional
     */
    public Optional<StandardPrice> normalize(int unitPrice, Integer netWeightGrams, String salesUnit) {
        if (netWeightGrams == null || netWeightGrams <= 0) {
            return Optional.empty();
        }

        // 公式：standardPrice = (unitPrice * 500) / netWeightGrams
        // 使用 BigDecimal 避浮点精度问题，四舍五入到分（2位小数）
        BigDecimal priceInFen = BigDecimal.valueOf(unitPrice);
        BigDecimal weight = BigDecimal.valueOf(netWeightGrams);

        // 每500g价格（元）= unitPrice(分) / 100 * 500 / netWeightGrams = unitPrice * 5 / netWeightGrams
        // 或理解为：每500g价格（分）= unitPrice * 500 / netWeightGrams, 然后转元
        BigDecimal standardPricePer500gInFen = priceInFen.multiply(FIVE_HUNDRED)
                .divide(weight, 0, RoundingMode.HALF_UP);

        // 转换为元（分 -> 元, 除以100, 保留2位小数）
        BigDecimal standardPricePer500g = standardPricePer500gInFen
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // KTD8: 持久化有理数 {numerator = priceFen*500, denominator = netWeightGram}
        long numerator = unitPrice * 500L;
        int denominator = netWeightGrams;

        return Optional.of(new StandardPrice(standardPricePer500g, unitPrice, netWeightGrams, numerator, denominator));
    }
}
