package com.campusfruit.discovery.ranking;

import com.campusfruit.discovery.dto.StoreOfferProjectionDTO;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 预计提货时间计算。
 * 基于门店营业时段、备货时长和当前时间估算。
 */
public class PickupTimeEstimator {

    /** 默认备货时长 15 分钟 */
    private static final int DEFAULT_LEAD_MINUTES = 15;

    /**
     * 计算预计可提货时间。
     * @return 如 "预计 14:30 可提货" 或 "明天营业后"
     */
    public static String estimate(StoreOfferProjectionDTO offer) {
        LocalTime now = LocalTime.now();
        int leadMinutes = DEFAULT_LEAD_MINUTES;

        // 简化：当前时间 + 备货时长
        LocalTime estimated = now.plusMinutes(leadMinutes);

        // 检查是否在营业时间内
        LocalTime openTime = LocalTime.of(8, 0);
        LocalTime closeTime = LocalTime.of(22, 0);

        if (estimated.isAfter(closeTime)) {
            return "明天 " + openTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " 后";
        }
        if (now.isBefore(openTime)) {
            return "预计 " + openTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " 后可提货";
        }

        return "预计 " + estimated.format(DateTimeFormatter.ofPattern("HH:mm")) + " 可提货";
    }
}
