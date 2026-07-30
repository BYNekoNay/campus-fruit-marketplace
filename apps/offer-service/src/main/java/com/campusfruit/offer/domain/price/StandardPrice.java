package com.campusfruit.offer.domain.price;

import java.math.BigDecimal;

/**
 * 标准价格值对象 — 有理数表示（KTD8）。
 * <ul>
 *   <li>normalizedNumerator = unitPrice(分) × 500</li>
 *   <li>normalizedDenominator = netWeightGram</li>
 *   <li>比较时交叉相乘避免浮点误差</li>
 * </ul>
 */
public class StandardPrice {

    /** 每500克标准价（元，仅供 UI 展示） */
    private BigDecimal standardPricePer500g;

    /** 每千克标准价（元，仅供 UI 展示） */
    private BigDecimal standardPricePerKg;

    /** 归一化分子：unitPrice(分) × 500 */
    private long normalizedNumerator;

    /** 归一化分母：netWeightGram(克) */
    private int normalizedDenominator;

    /** 是否可比（净重有效则为可比） */
    private boolean isComparable;

    /** 原始价格（分） */
    private long originalPrice;

    /** 原始净重（克） */
    private int originalNetWeight;

    public StandardPrice() {}

    public StandardPrice(BigDecimal standardPricePer500g, long originalPrice, int originalNetWeight,
                          long normalizedNumerator, int normalizedDenominator) {
        this.standardPricePer500g = standardPricePer500g;
        this.standardPricePerKg = standardPricePer500g.multiply(BigDecimal.valueOf(2));
        this.isComparable = true;
        this.originalPrice = originalPrice;
        this.originalNetWeight = originalNetWeight;
        this.normalizedNumerator = normalizedNumerator;
        this.normalizedDenominator = normalizedDenominator;
    }

    public BigDecimal getStandardPricePer500g() { return standardPricePer500g; }
    public void setStandardPricePer500g(BigDecimal standardPricePer500g) { this.standardPricePer500g = standardPricePer500g; }

    public BigDecimal getStandardPricePerKg() { return standardPricePerKg; }
    public void setStandardPricePerKg(BigDecimal standardPricePerKg) { this.standardPricePerKg = standardPricePerKg; }

    public boolean isComparable() { return isComparable; }
    public void setComparable(boolean isComparable) { this.isComparable = isComparable; }

    public long getNormalizedNumerator() { return normalizedNumerator; }
    public void setNormalizedNumerator(long normalizedNumerator) { this.normalizedNumerator = normalizedNumerator; }

    public int getNormalizedDenominator() { return normalizedDenominator; }
    public void setNormalizedDenominator(int normalizedDenominator) { this.normalizedDenominator = normalizedDenominator; }

    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }

    public int getOriginalNetWeight() { return originalNetWeight; }
    public void setOriginalNetWeight(int originalNetWeight) { this.originalNetWeight = originalNetWeight; }
}
