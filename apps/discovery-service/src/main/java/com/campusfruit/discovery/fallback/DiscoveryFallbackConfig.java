package com.campusfruit.discovery.fallback;

import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 降级策略配置。
 * <p>
 * 当投影表为空时（重启后首次加载 / 数据同步延迟），搜索返回空结果并提示"数据加载中"，不实时调用外部 Merchant/Offer API。
 */
@Component
public class DiscoveryFallbackConfig {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryFallbackConfig.class);
    static final String DATA_LOADING_MESSAGE = "数据加载中，请稍后再试";

    private final StoreOfferProjectionRepository projectionRepository;

    private volatile boolean dataAvailable = false;

    public DiscoveryFallbackConfig(StoreOfferProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @PostConstruct
    public void init() {
        checkDataAvailability();
        log.info("DiscoveryFallback initialized: dataAvailable={}", dataAvailable);
    }

    /**
     * 检查投影表是否有数据。
     */
    public void checkDataAvailability() {
        long count = projectionRepository.count();
        dataAvailable = count > 0;
        if (!dataAvailable) {
            log.warn("Projection table is empty, search will return empty results with loading prompt");
        }
    }

    /**
     * 当前数据是否可用（投影表非空）。
     */
    public boolean isDataAvailable() {
        return dataAvailable;
    }

    /**
     * 获取降级提示信息。
     */
    public String getFallbackMessage() {
        return dataAvailable ? null : DATA_LOADING_MESSAGE;
    }

    /**
     * 标记数据变为可用（例如投影重建完成后调用）。
     */
    public void markDataAvailable() {
        this.dataAvailable = true;
        log.info("Projection data marked as available");
    }
}
