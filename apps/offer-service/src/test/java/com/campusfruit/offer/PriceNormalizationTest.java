package com.campusfruit.offer;

import com.campusfruit.offer.domain.price.PriceNormalizer;
import com.campusfruit.offer.domain.price.StandardPrice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 价格标准化测试：验证 PriceNormalizer 的标准价格（每500g价格）计算逻辑。
 * 使用 BigDecimal 避免浮点精度问题。
 */
class PriceNormalizationTest {

    private final PriceNormalizer normalizer = new PriceNormalizer();

    @ParameterizedTest
    @CsvSource({
            // unitPrice(分), netWeightGrams, expectedStandardPricePer500g
            "999, 333, 15.00",
            "999, 500, 9.99",
            "500, 250, 10.00",
            "2000, 1000, 10.00",
            "1500, 500, 15.00",
            "300, 300, 5.00",
            "0, 500, 0.00",
            "999, 600, 8.33",
    })
    void shouldNormalizeTo500gStandardPrice(int unitPrice, int netWeightGrams, String expectedPrice) {
        Optional<StandardPrice> result = normalizer.normalize(unitPrice, netWeightGrams, "盒装");
        assertThat(result).isPresent();
        BigDecimal expected = new BigDecimal(expectedPrice);
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(expected);
    }

    @Test
    void shouldConvert333gPackageCorrectly() {
        // 333g 包装售价 999分 → 每500g = 999 * 500 / 333 / 100 = 15.00元
        Optional<StandardPrice> result = normalizer.normalize(999, 333, "333g盒装");
        assertThat(result).isPresent();
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void shouldReturnDirectPriceFor500gPackage() {
        // 500g 包装 → 每500g 直接等于单价
        Optional<StandardPrice> result = normalizer.normalize(999, 500, "500g盒装");
        assertThat(result).isPresent();
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("9.99"));
    }

    @Test
    void shouldCalculateFor1kgPackage() {
        // 1kg 包装 → 每500g = 单价 / 2
        Optional<StandardPrice> result = normalizer.normalize(2000, 1000, "1kg装");
        assertThat(result).isPresent();
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldReturnEmptyForZeroWeight() {
        Optional<StandardPrice> result = normalizer.normalize(999, 0, "个");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForNullWeight() {
        Optional<StandardPrice> result = normalizer.normalize(999, null, "个");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldPreserveBigDecimalPrecision() {
        // 验证 BigDecimal 精度：无浮点数误差
        Optional<StandardPrice> result = normalizer.normalize(333, 300, "300g装");
        assertThat(result).isPresent();
        // 333 * 500 / 300 = 555分 = 5.55元
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("5.55"));
    }

    @Test
    void shouldHandleMinimumWeight() {
        // 1g 边界值
        Optional<StandardPrice> result = normalizer.normalize(1, 1, "1g");
        assertThat(result).isPresent();
        // 1 * 500 / 1 = 500分 = 5.00元
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void shouldHandleMaximumWeight() {
        // 100000g (100kg) 边界值
        Optional<StandardPrice> result = normalizer.normalize(100000, 100000, "100kg装");
        assertThat(result).isPresent();
        // 100000 * 500 / 100000 = 500分 = 5.00元
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void shouldSetComparableForValidWeight() {
        Optional<StandardPrice> result = normalizer.normalize(999, 500, "500g");
        assertThat(result).isPresent();
        assertThat(result.get().isComparable()).isTrue();
    }

    @Test
    void shouldSetNotComparableForInvalidWeight() {
        Optional<StandardPrice> result = normalizer.normalize(999, 0, "个");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCalculateStandardPricePerKg() {
        Optional<StandardPrice> result = normalizer.normalize(999, 500, "500g");
        assertThat(result).isPresent();
        // 每500g = 9.99元， 每kg = 19.98元
        assertThat(result.get().getStandardPricePerKg()).isEqualByComparingTo(new BigDecimal("19.98"));
    }

    @Test
    void shouldHandleLargeValues() {
        // 999.99元/kg → 每500g = 499.995元 ≈ 500.00元
        Optional<StandardPrice> result = normalizer.normalize(99999, 1000, "1kg装");
        assertThat(result).isPresent();
        // 99999 * 500 / 1000 = 49999.5分 → 四舍五入 = 50000分 = 500.00元
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldHandleWholeNumberStandardPrice() {
        // 1kg售价2000分，每500g = 1000分 = 10元
        Optional<StandardPrice> result = normalizer.normalize(2000, 1000, "1kg装");
        assertThat(result).isPresent();
        assertThat(result.get().getStandardPricePer500g()).isEqualByComparingTo(new BigDecimal("10.00"));
    }
}
