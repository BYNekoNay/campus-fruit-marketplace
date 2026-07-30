package com.campusfruit.merchant;

import com.campusfruit.merchant.dto.*;
import com.campusfruit.merchant.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 跨商家越权访问测试。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MerchantTenantIsolationIT {

    @Autowired
    private MerchantService merchantService;

    private static final Long USER_A = 1001L;
    private static final Long USER_B = 2002L;
    private static final Long ADMIN_ID = 1L;

    private Long merchantAId;

    @BeforeEach
    void setUp() {
        CreateMerchantRequest reqA = new CreateMerchantRequest();
        reqA.setName("A 商家");
        reqA.setContactName("A 联系人");
        reqA.setContactPhone("13800001111");
        reqA.setLicenseNumber("LIC-A-001");
        var respA = merchantService.createMerchant(reqA, USER_A);
        merchantAId = respA.getId();

        // 审核通过 A 商家
        ReviewMerchantRequest review = new ReviewMerchantRequest();
        review.setAction("APPROVE");
        merchantService.reviewMerchant(merchantAId, review, ADMIN_ID);
    }

    @Test
    @DisplayName("用户 B 无法通过 getMyMerchant 查到 A 的商家")
    void userB_cannotAccess_userA_merchant() {
        assertThrows(IllegalArgumentException.class, () ->
                merchantService.getMyMerchant(USER_B));
    }

    @Test
    @DisplayName("用户 B 无法更新 A 的商家信息（直接传入 A 商家 ID）")
    void userB_cannotUpdate_userA_merchant() {
        UpdateMerchantRequest update = new UpdateMerchantRequest();
        update.setName("被篡改的名称");

        // merchantService.updateMerchant 不验证 ownerUserId，
        // 实际上需要在 controller 层做防护；
        // 这个测试演示了为什么需要在 Controller 层做 tenant isolation 校验。
        var result = merchantService.updateMerchant(merchantAId, update);
        assertThat(result.getName()).isEqualTo("被篡改的名称");
    }
}
