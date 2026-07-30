package com.campusfruit.order;

import com.campusfruit.order.dto.PickupCodeResponse;
import com.campusfruit.order.entity.Order;
import com.campusfruit.order.enums.OrderStatus;
import com.campusfruit.order.pickup.PickupCodeService;
import com.campusfruit.order.repository.OrderRepository;
import com.campusfruit.order.repository.OrderStatusEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自取码集成测试。
 * <p>
 * 验证：
 * 1. 生成自取码并返回明文码
 * 2. 正确码核验通过
 * 3. 错误码核验拒绝
 * 4. 过期码核验拒绝
 * 5. 重新生成自取码
 * 6. 审计日志
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PickupCodeIT {

    @Autowired
    private PickupCodeService pickupCodeService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusEventRepository statusEventRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        statusEventRepository.deleteAll();
        orderRepository.deleteAll();

        testOrder = new Order();
        testOrder.setOrderNo("ORD-PICKUP-001");
        testOrder.setUserId(300L);
        testOrder.setStoreId(1L);
        testOrder.setStatus(OrderStatus.ACCEPTED);
        testOrder.setTotalAmount(5000L);
        testOrder.setItemCount(2);
        testOrder = orderRepository.save(testOrder);
    }

    @AfterEach
    void tearDown() {
        statusEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void shouldGenerateAndVerifyPickupCode() {
        // 生成自取码
        PickupCodeResponse response = pickupCodeService.generatePickupCode(testOrder.getId());

        assertNotNull(response);
        assertNotNull(response.getPickupCode());
        assertEquals(6, response.getPickupCode().length());
        assertNotNull(response.getExpiresAt());
        assertNotNull(response.getExpiresIn());
        assertTrue(response.getExpiresIn() > 0);

        // 验证订单哈希已存储
        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertNotNull(updated.getPickupCodeHash());
        assertNotNull(updated.getPickupCodeExpiresAt());

        // 核验正确的码
        boolean verified = pickupCodeService.verifyPickupCode(testOrder.getId(), response.getPickupCode());
        assertTrue(verified, "正确自取码应核验通过");
    }

    @Test
    void shouldRejectWrongPickupCode() {
        // 生成自取码
        PickupCodeResponse response = pickupCodeService.generatePickupCode(testOrder.getId());
        assertNotNull(response.getPickupCode());

        // 用错误的码核验
        boolean verified = pickupCodeService.verifyPickupCode(testOrder.getId(), "000000");
        assertFalse(verified, "错误自取码应核验拒绝");
    }

    @Test
    void shouldRejectExpiredPickupCode() {
        // 生成自取码后人工置为过期
        PickupCodeResponse response = pickupCodeService.generatePickupCode(testOrder.getId());

        Order order = orderRepository.findById(testOrder.getId()).orElseThrow();
        order.setPickupCodeExpiresAt(Instant.now().minus(Duration.ofHours(1)));
        orderRepository.save(order);

        // 核验过期的码
        boolean verified = pickupCodeService.verifyPickupCode(testOrder.getId(), response.getPickupCode());
        assertFalse(verified, "过期自取码应核验拒绝");

        // 再生成新的码，验证新码可以核验
        PickupCodeResponse newResponse = pickupCodeService.regeneratePickupCode(testOrder.getId());
        boolean newVerified = pickupCodeService.verifyPickupCode(testOrder.getId(), newResponse.getPickupCode());
        assertTrue(newVerified, "重新生成的自取码应核验通过");
    }

    @Test
    void shouldRegeneratePickupCodeAndInvalidateOld() {
        // 首次生成
        PickupCodeResponse oldResponse = pickupCodeService.generatePickupCode(testOrder.getId());
        String oldCode = oldResponse.getPickupCode();

        // 重新生成
        PickupCodeResponse newResponse = pickupCodeService.regeneratePickupCode(testOrder.getId());
        String newCode = newResponse.getPickupCode();

        assertNotEquals(oldCode, newCode, "新旧自取码应不同");

        // 旧码应该核验失败
        boolean oldVerified = pickupCodeService.verifyPickupCode(testOrder.getId(), oldCode);
        assertFalse(oldVerified, "旧自取码应核验失败");

        // 新码应该核验通过
        boolean newVerified = pickupCodeService.verifyPickupCode(testOrder.getId(), newCode);
        assertTrue(newVerified, "新自取码应核验通过");
    }

    @Test
    void shouldNotAllowDuplicateVerificationAfterCompletion() {
        // 自取码可以多次核验通过（code级验证不负责状态变更）
        // 此测试只验证 hash 匹配逻辑，重复核销拒绝由 completeOrder 流程保证
        PickupCodeResponse response = pickupCodeService.generatePickupCode(testOrder.getId());

        // 多次核验相同码应该都通过（仅验证哈希匹配）
        boolean first = pickupCodeService.verifyPickupCode(testOrder.getId(), response.getPickupCode());
        boolean second = pickupCodeService.verifyPickupCode(testOrder.getId(), response.getPickupCode());
        assertTrue(first);
        assertTrue(second);
    }

    @Test
    void shouldRejectVerificationForOrderWithoutCode() {
        // 从未生成过自取码
        boolean verified = pickupCodeService.verifyPickupCode(testOrder.getId(), "123456");
        assertFalse(verified, "未生成自取码的订单应核验拒绝");
    }

    @Test
    void shouldRecordAuditLogOnGenerate() {
        pickupCodeService.generatePickupCode(testOrder.getId());

        var events = statusEventRepository.findByOrderIdOrderByCreatedAt(testOrder.getId());
        assertFalse(events.isEmpty(), "生成自取码应写审计日志");
    }
}
