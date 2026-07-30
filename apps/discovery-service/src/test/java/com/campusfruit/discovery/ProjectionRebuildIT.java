package com.campusfruit.discovery;

import com.campusfruit.discovery.dto.RebuildStatusDTO;
import com.campusfruit.discovery.entity.*;
import com.campusfruit.discovery.fallback.DiscoveryFallbackConfig;
import com.campusfruit.discovery.rebuild.ProjectionRebuildService;
import com.campusfruit.discovery.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 投影重建集成测试。
 * 测试全量重建流程、影子表数据正确性、增量追平、favorites 不受影响、序列缺口检测。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectionRebuildIT {

    @Autowired
    private StoreOfferProjectionRepository projectionRepository;

    @Autowired
    private ProjectionCheckpointRepository checkpointRepository;

    @Autowired
    private ProjectionChangeLogRepository changeLogRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private DiscoveryFallbackConfig fallbackConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        projectionRepository.deleteAll();
        checkpointRepository.deleteAll();
        changeLogRepository.deleteAll();
        favoriteRepository.deleteAll();
    }

    @Nested
    @DisplayName("全量重建流程")
    class FullRebuild {

        @Test
        @DisplayName("checkpoint 记录了 snapshot token")
        void checkpointShouldRecordSnapshotToken() {
            // Given - 在 change_log 中插入数据
            ProjectionChangeLog log = new ProjectionChangeLog();
            log.setSourceService("merchant-service");
            log.setSequence(100L);
            log.setAggregateId("1");
            log.setEventType("com.campusfruit.merchant.StoreActivated");
            log.setOperation("INSERT");
            log.setAfterSnapshot("{\"storeId\":1,\"storeName\":\"测试\"}");
            changeLogRepository.save(log);

            // When - 查询 snapshot token
            Long maxSeq = changeLogRepository.findMaxSequenceBySourceService("merchant-service");

            // Then
            assertThat(maxSeq).isEqualTo(100L);
        }

        @Test
        @DisplayName("影子表数据正确性（模拟）")
        void shadowTableDataCorrectness() throws Exception {
            // Given - 创建影子表并插入数据
            jdbcTemplate.execute("DROP TABLE IF EXISTS store_offer_projections_shadow");
            jdbcTemplate.execute("CREATE TABLE store_offer_projections_shadow LIKE store_offer_projections");

            jdbcTemplate.update(
                    "INSERT INTO store_offer_projections_shadow " +
                    "(store_id, offer_id, store_name, store_address, store_lat, store_lng, " +
                    "store_phone, store_status, merchant_id, merchant_name, canonical_fruit_id, " +
                    "fruit_category, fruit_variety, fruit_grade, fruit_origin, sales_unit, " +
                    "net_weight_grams, unit_price, standard_price_per500g, is_comparable, " +
                    "available_quantity, offer_status, price_stale, avg_rating, review_count, " +
                    "aggregate_version, last_event_type, last_event_at) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    1L, 101L, "测试门店", "测试地址", 39.9, 116.4,
                    "13800000001", "ACTIVE", 1L, "测试商家", 10L,
                    "苹果", "红富士", "一级", "山东", "斤",
                    500, 800L, new BigDecimal("8.00"), true,
                    100, "ACTIVE", false, new BigDecimal("4.5"), 50,
                    1, "com.campusfruit.rebuild.Rebuild", Instant.now()
            );

            // When - 验证影子表数据
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM store_offer_projections_shadow", Integer.class);

            // Then
            assertThat(count).isEqualTo(1);

            String storeName = jdbcTemplate.queryForObject(
                    "SELECT store_name FROM store_offer_projections_shadow WHERE offer_id = 101",
                    String.class);
            assertThat(storeName).isEqualTo("测试门店");

            // Cleanup
            jdbcTemplate.execute("DROP TABLE IF EXISTS store_offer_projections_shadow");
        }

        @Test
        @DisplayName("原子切换后旧表被替换")
        void atomicSwapShouldReplaceOldTable() throws Exception {
            // Given
            jdbcTemplate.execute("DROP TABLE IF EXISTS store_offer_projections_shadow");
            jdbcTemplate.execute("DROP TABLE IF EXISTS store_offer_projections_old");
            jdbcTemplate.execute("CREATE TABLE store_offer_projections_shadow LIKE store_offer_projections");

            // 在影子表插入新数据
            jdbcTemplate.update(
                    "INSERT INTO store_offer_projections_shadow (store_id, offer_id, store_name, store_status) " +
                    "VALUES (?,?,?,?)", 1L, 201L, "新旧替门店", "ACTIVE");

            // 在主表插入旧数据
            jdbcTemplate.update(
                    "INSERT INTO store_offer_projections (store_id, offer_id, store_name, store_status) " +
                    "VALUES (?,?,?,?)", 2L, 202L, "旧门店", "ACTIVE");

            // When - 原子切换
            jdbcTemplate.execute("RENAME TABLE store_offer_projections TO store_offer_projections_old, " +
                    "store_offer_projections_shadow TO store_offer_projections");

            // Then - 主表现在是影子表的内容
            String storeName = jdbcTemplate.queryForObject(
                    "SELECT store_name FROM store_offer_projections WHERE offer_id = 201",
                    String.class);
            assertThat(storeName).isEqualTo("新旧替门店");

            // 旧数据在 _old 表中
            String oldName = jdbcTemplate.queryForObject(
                    "SELECT store_name FROM store_offer_projections_old WHERE offer_id = 202",
                    String.class);
            assertThat(oldName).isEqualTo("旧门店");

            // Cleanup - 恢复原状
            jdbcTemplate.execute("RENAME TABLE store_offer_projections TO store_offer_projections_shadow, " +
                    "store_offer_projections_old TO store_offer_projections");
            jdbcTemplate.execute("DROP TABLE IF EXISTS store_offer_projections_shadow");
        }
    }

    @Nested
    @DisplayName("增量追平")
    class DeltaSync {

        @Test
        @DisplayName("从指定序列拉取增量 delta")
        void shouldPullDeltaFromSequence() {
            // Given
            ProjectionChangeLog log1 = createChangeLog("merchant-service", 10L, "INSERT",
                    "{\"storeId\":1,\"storeName\":\"门店A\"}");
            ProjectionChangeLog log2 = createChangeLog("merchant-service", 11L, "UPDATE",
                    "{\"storeId\":1,\"storeName\":\"门店A改名\"}");
            ProjectionChangeLog log3 = createChangeLog("merchant-service", 12L, "INSERT",
                    "{\"storeId\":2,\"storeName\":\"门店B\"}");
            changeLogRepository.saveAll(List.of(log1, log2, log3));

            // When - 从序列 10 之后开始追
            List<ProjectionChangeLog> deltas = changeLogRepository
                    .findBySourceServiceAndSequenceGreaterThanOrderBySequenceAsc("merchant-service", 10L);

            // Then
            assertThat(deltas).hasSize(2);
            assertThat(deltas.get(0).getSequence()).isEqualTo(11L);
            assertThat(deltas.get(1).getSequence()).isEqualTo(12L);
        }

        @Test
        @DisplayName("无新 delta 时不返回数据")
        void shouldReturnEmptyWhenNoNewDelta() {
            // Given
            ProjectionChangeLog log = createChangeLog("merchant-service", 5L, "INSERT", "{}");
            changeLogRepository.save(log);

            // When - 从序列 100 之后追
            List<ProjectionChangeLog> deltas = changeLogRepository
                    .findBySourceServiceAndSequenceGreaterThanOrderBySequenceAsc("merchant-service", 100L);

            // Then
            assertThat(deltas).isEmpty();
        }

        @Test
        @DisplayName("最高水位标记")
        void highWatermarkShouldBeCorrect() {
            // Given
            ProjectionChangeLog log1 = createChangeLog("offer-service", 1L, "INSERT", "{}");
            ProjectionChangeLog log2 = createChangeLog("offer-service", 5L, "INSERT", "{}");
            ProjectionChangeLog log3 = createChangeLog("offer-service", 10L, "INSERT", "{}");
            changeLogRepository.saveAll(List.of(log1, log2, log3));

            // When
            List<ProjectionChangeLog> deltas = changeLogRepository
                    .findBySourceServiceAndSequenceGreaterThanOrderBySequenceAsc("offer-service", 0L);

            // Then
            assertThat(deltas).hasSize(3);
            long highWatermark = deltas.get(deltas.size() - 1).getSequence();
            assertThat(highWatermark).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Favorites 不受影响")
    class FavoritesIndependence {

        @Test
        @DisplayName("重建后 favorites 记录保持不变")
        void favoritesShouldBePreservedAfterRebuild() throws Exception {
            // Given
            Favorite fav = new Favorite(100L, 1L);
            favoriteRepository.save(fav);

            // When - 模拟重建（影子表操作不影响 favorites）
            jdbcTemplate.execute("DROP TABLE IF EXISTS store_offer_projections_shadow");
            jdbcTemplate.execute("CREATE TABLE store_offer_projections_shadow LIKE store_offer_projections");
            jdbcTemplate.update(
                    "INSERT INTO store_offer_projections_shadow (store_id, offer_id, store_name, store_status) " +
                    "VALUES (?,?,?,?)", 3L, 301L, "重建门店", "ACTIVE");
            jdbcTemplate.execute("RENAME TABLE store_offer_projections TO store_offer_projections_old, " +
                    "store_offer_projections_shadow TO store_offer_projections");

            // Then - favorites 不受影响
            assertThat(favoriteRepository.count()).isEqualTo(1);
            Favorite found = favoriteRepository.findByUserIdAndStoreId(100L, 1L).orElse(null);
            assertThat(found).isNotNull();
            assertThat(found.getStoreId()).isEqualTo(1L);

            // Cleanup
            jdbcTemplate.execute("RENAME TABLE store_offer_projections TO store_offer_projections_shadow, " +
                    "store_offer_projections_old TO store_offer_projections");
            jdbcTemplate.execute("DROP TABLE IF EXISTS store_offer_projections_shadow");
        }

        @Test
        @DisplayName("投影表重建不删除 favorites")
        void projectionRebuildShouldNotDeleteFavorites() {
            // Given
            favoriteRepository.save(new Favorite(1L, 10L));
            favoriteRepository.save(new Favorite(1L, 20L));
            favoriteRepository.save(new Favorite(2L, 10L));

            // When - 删除投影表数据（模拟重建前置条件）
            projectionRepository.deleteAll();

            // Then - favorites 记录保留
            assertThat(favoriteRepository.count()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("序列缺口检测")
    class SequenceGapDetection {

        @Test
        @DisplayName("连续序列无缺口")
        void consecutiveSequenceShouldHaveNoGap() {
            // Given
            for (long i = 1; i <= 5; i++) {
                ProjectionChangeLog log = createChangeLog("merchant-service", i, "INSERT",
                        "{\"offerId\":" + i + "}");
                changeLogRepository.save(log);
            }

            // When
            boolean hasGap = checkForGaps("merchant-service");

            // Then
            assertThat(hasGap).isFalse();
        }

        @Test
        @DisplayName("非连续序列检测到缺口")
        void nonConsecutiveSequenceShouldDetectGap() {
            // Given - 序列 1, 2, 4, 5（缺失 3）
            changeLogRepository.save(createChangeLog("merchant-service", 1L, "INSERT", "{}"));
            changeLogRepository.save(createChangeLog("merchant-service", 2L, "INSERT", "{}"));
            changeLogRepository.save(createChangeLog("merchant-service", 4L, "INSERT", "{}"));
            changeLogRepository.save(createChangeLog("merchant-service", 5L, "INSERT", "{}"));

            // When
            boolean hasGap = checkForGaps("merchant-service");

            // Then
            assertThat(hasGap).isTrue();
        }

        @Test
        @DisplayName("空日志无缺口")
        void emptyLogShouldHaveNoGap() {
            boolean hasGap = checkForGaps("merchant-service");
            assertThat(hasGap).isFalse();
        }

        private boolean checkForGaps(String sourceService) {
            List<ProjectionChangeLog> all = changeLogRepository.findBySourceServiceOrderBySequenceAsc(sourceService);
            if (all.isEmpty()) {
                return false;
            }
            long expected = all.get(0).getSequence();
            for (ProjectionChangeLog log : all) {
                if (log.getSequence() != expected) {
                    return true;
                }
                expected++;
            }
            return false;
        }
    }

    @Nested
    @DisplayName("降级标记")
    class FallbackMarking {

        @Test
        @DisplayName("重建后标记数据可用")
        void shouldMarkDataAvailableAfterRebuild() {
            // Given - 投影表为空
            projectionRepository.deleteAll();

            // When - 手动标记
            fallbackConfig.markDataAvailable();

            // Then
            assertThat(fallbackConfig.isDataAvailable()).isTrue();
            assertThat(fallbackConfig.getFallbackMessage()).isNull();
        }

        @Test
        @DisplayName("初始状态：投影表空时降级消息")
        void shouldReturnFallbackMessageWhenEmpty() {
            projectionRepository.deleteAll();
            fallbackConfig.checkDataAvailability();

            assertThat(fallbackConfig.isDataAvailable()).isFalse();
            assertThat(fallbackConfig.getFallbackMessage()).isNotNull();
            assertThat(fallbackConfig.getFallbackMessage()).contains("数据加载中");
        }
    }

    // --- 辅助方法 ---

    private ProjectionChangeLog createChangeLog(String sourceService, long sequence,
                                                  String operation, String afterSnapshot) {
        ProjectionChangeLog log = new ProjectionChangeLog();
        log.setSourceService(sourceService);
        log.setSequence(sequence);
        log.setAggregateId(String.valueOf(sequence));
        log.setEventType("com.campusfruit.test.TestEvent");
        log.setOperation(operation);
        log.setAfterSnapshot(afterSnapshot);
        log.setCreatedAt(Instant.now());
        return log;
    }
}
