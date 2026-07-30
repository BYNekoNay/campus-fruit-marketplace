package com.campusfruit.order.pickup;

import com.campusfruit.order.dto.PickupCodeResponse;
import com.campusfruit.order.entity.Order;
import com.campusfruit.order.entity.OrderStatusEvent;
import com.campusfruit.order.enums.OperatorType;
import com.campusfruit.order.repository.OrderRepository;
import com.campusfruit.order.repository.OrderStatusEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * 自取码服务。
 * <p>
 * 生成6位数字随机自取码，使用 SHA-256 哈希存储到数据库，
 * 明文码仅在生成时返回一次。
 */
@Service
public class PickupCodeService {

    private static final Logger log = LoggerFactory.getLogger(PickupCodeService.class);

    private final OrderRepository orderRepository;
    private final OrderStatusEventRepository statusEventRepository;
    private final Random secureRandom = new SecureRandom();

    @Value("${app.order.pickup.code-length:6}")
    private int codeLength;

    @Value("${app.order.pickup.code-expiry-hours:24}")
    private int codeExpiryHours;

    public PickupCodeService(OrderRepository orderRepository,
                              OrderStatusEventRepository statusEventRepository) {
        this.orderRepository = orderRepository;
        this.statusEventRepository = statusEventRepository;
    }

    /**
     * 生成自取码。
     * <ol>
     *   <li>生成6位数字随机码</li>
     *   <li>SHA-256 哈希存储到 order.pickupCodeHash</li>
     *   <li>设置 pickupCodeExpiresAt = now + code-expiry-hours</li>
     *   <li>返回明文码（仅这一次）</li>
     * </ol>
     *
     * @param orderId 订单ID
     * @return 自取码响应（含明文码）
     */
    @Transactional
    public PickupCodeResponse generatePickupCode(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));

        // 生成随机数字码
        String plainCode = generateRandomNumericCode();
        String codeHash = sha256(plainCode);

        Instant expiresAt = Instant.now().plus(Duration.ofHours(codeExpiryHours));

        // 存储哈希
        order.setPickupCodeHash(codeHash);
        order.setPickupCodeExpiresAt(expiresAt);
        orderRepository.save(order);

        // 记录审计日志
        OrderStatusEvent auditEvent = OrderStatusEvent.of(
                order.getId(),
                null,
                "PICKUP_CODE_GENERATED",
                OperatorType.SYSTEM.name(),
                null,
                "生成自取码"
        );
        statusEventRepository.save(auditEvent);

        log.info("生成自取码: orderNo={}, expiresAt={}", order.getOrderNo(), expiresAt);

        // 构建响应
        PickupCodeResponse response = new PickupCodeResponse();
        response.setPickupCode(plainCode);
        response.setExpiresAt(expiresAt);
        response.setExpiresIn(Duration.between(Instant.now(), expiresAt).getSeconds());

        return response;
    }

    /**
     * 核验自取码。
     * <ol>
     *   <li>对输入 code 做 SHA-256</li>
     *   <li>比对 order.pickupCodeHash</li>
     *   <li>校验未过期</li>
     * </ol>
     *
     * @param orderId 订单ID
     * @param code    待核验的明文自取码
     * @return true 如果核验成功
     */
    @Transactional(readOnly = true)
    public boolean verifyPickupCode(Long orderId, String code) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));

        if (order.getPickupCodeHash() == null) {
            log.warn("自取码核验失败（未生成自取码）: orderId={}", orderId);
            return false;
        }

        // 校验过期
        if (order.getPickupCodeExpiresAt() != null
                && order.getPickupCodeExpiresAt().isBefore(Instant.now())) {
            log.warn("自取码核验失败（已过期）: orderId={}", orderId);
            return false;
        }

        // 比对哈希
        String inputHash = sha256(code);
        boolean matches = inputHash.equals(order.getPickupCodeHash());

        if (matches) {
            log.info("自取码核验成功: orderId={}", orderId);
        } else {
            log.warn("自取码核验失败（不匹配）: orderId={}", orderId);
        }

        return matches;
    }

    /**
     * 重新生成自取码。
     * <ol>
     *   <li>作废旧码</li>
     *   <li>生成新码</li>
     *   <li>记录审计日志</li>
     * </ol>
     *
     * @param orderId 订单ID
     * @return 新自取码响应（含明文码）
     */
    @Transactional
    public PickupCodeResponse regeneratePickupCode(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在: " + orderId));

        String oldHash = order.getPickupCodeHash();
        Instant oldExpiry = order.getPickupCodeExpiresAt();

        // 作废旧码
        order.setPickupCodeHash(null);
        order.setPickupCodeExpiresAt(null);

        // 记录审计日志（旧码作废）
        OrderStatusEvent invalidateEvent = OrderStatusEvent.of(
                order.getId(),
                null,
                "PICKUP_CODE_INVALIDATED",
                OperatorType.SYSTEM.name(),
                null,
                "旧自取码作废，重新生成"
        );
        statusEventRepository.save(invalidateEvent);

        log.info("旧自取码作废: orderNo={}", order.getOrderNo());

        // 生成新码
        String plainCode = generateRandomNumericCode();
        String codeHash = sha256(plainCode);
        Instant expiresAt = Instant.now().plus(Duration.ofHours(codeExpiryHours));

        order.setPickupCodeHash(codeHash);
        order.setPickupCodeExpiresAt(expiresAt);
        orderRepository.save(order);

        // 记录审计日志（新码生成）
        OrderStatusEvent generatedEvent = OrderStatusEvent.of(
                order.getId(),
                null,
                "PICKUP_CODE_REGENERATED",
                OperatorType.SYSTEM.name(),
                null,
                "重新生成自取码"
        );
        statusEventRepository.save(generatedEvent);

        log.info("重新生成自取码: orderNo={}, oldExpiry={}, newExpiry={}", order.getOrderNo(), oldExpiry, expiresAt);

        // 构建响应
        PickupCodeResponse response = new PickupCodeResponse();
        response.setPickupCode(plainCode);
        response.setExpiresAt(expiresAt);
        response.setExpiresIn(Duration.between(Instant.now(), expiresAt).getSeconds());

        return response;
    }

    /**
     * 生成指定位数的随机数字码。
     */
    private String generateRandomNumericCode() {
        int upperBound = (int) Math.pow(10, codeLength);
        int code = secureRandom.nextInt(upperBound);
        return String.format("%0" + codeLength + "d", code);
    }

    /**
     * SHA-256 哈希。
     */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
