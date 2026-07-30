package com.campusfruit.merchant;

import com.campusfruit.merchant.entity.Merchant;
import com.campusfruit.merchant.enums.MerchantStatus;
import com.campusfruit.merchant.repository.MerchantRepository;
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
 * 商家入驻-审核完整链路集成测试。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MerchantOnboardingIT {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private MerchantRepository merchantRepository;

    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_ADMIN_ID = 1L;

    private com.campusfruit.merchant.dto.CreateMerchantRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new com.campusfruit.merchant.dto.CreateMerchantRequest();
        createRequest.setName("校园鲜果铺");
        createRequest.setContactName("张三");
        createRequest.setContactPhone("13800138000");
        createRequest.setLicenseNumber("91110108MA01XXXXX");
    }

    @Test
    @DisplayName("商家入驻申请 -> 状态为 PENDING_REVIEW")
    void step1_applyMerchant_shouldBePendingReview() {
        var response = merchantService.createMerchant(createRequest, TEST_USER_ID);

        assertThat(response.getName()).isEqualTo("校园鲜果铺");
        assertThat(response.getStatus()).isEqualTo(MerchantStatus.PENDING_REVIEW);
        assertThat(response.getOwnerUserId()).isEqualTo(TEST_USER_ID);

        // 确认数据库中确实存入了
        Merchant saved = merchantRepository.findById(response.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(MerchantStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("审核通过 -> 状态变为 APPROVED 并发布事件")
    void step2_approveMerchant_shouldBeApproved() {
        var created = merchantService.createMerchant(createRequest, TEST_USER_ID);

        var reviewRequest = new com.campusfruit.merchant.dto.ReviewMerchantRequest();
        reviewRequest.setAction("APPROVE");

        var reviewed = merchantService.reviewMerchant(created.getId(), reviewRequest, TEST_ADMIN_ID);

        assertThat(reviewed.getStatus()).isEqualTo(MerchantStatus.APPROVED);
        assertThat(reviewed.getReviewedBy()).isEqualTo(TEST_ADMIN_ID);
        assertThat(reviewed.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("审核拒绝（无原因） -> 抛出异常")
    void step3_rejectWithoutReason_shouldThrowException() {
        var created = merchantService.createMerchant(createRequest, TEST_USER_ID);

        var reviewRequest = new com.campusfruit.merchant.dto.ReviewMerchantRequest();
        reviewRequest.setAction("REJECT");
        // 不设置 reason

        assertThrows(IllegalArgumentException.class, () ->
                merchantService.reviewMerchant(created.getId(), reviewRequest, TEST_ADMIN_ID));
    }

    @Test
    @DisplayName("审核拒绝（有原因） -> 状态变为 REJECTED")
    void step4_rejectWithReason_shouldBeRejected() {
        var created = merchantService.createMerchant(createRequest, TEST_USER_ID);

        var reviewRequest = new com.campusfruit.merchant.dto.ReviewMerchantRequest();
        reviewRequest.setAction("REJECT");
        reviewRequest.setReason("营业执照信息不符");

        var reviewed = merchantService.reviewMerchant(created.getId(), reviewRequest, TEST_ADMIN_ID);

        assertThat(reviewed.getStatus()).isEqualTo(MerchantStatus.REJECTED);
        assertThat(reviewed.getRejectReason()).isEqualTo("营业执照信息不符");
    }

    @Test
    @DisplayName("查询我的商家")
    void step5_myMerchant_shouldReturnOwnMerchant() {
        merchantService.createMerchant(createRequest, TEST_USER_ID);
        var myMerchant = merchantService.getMyMerchant(TEST_USER_ID);

        assertThat(myMerchant.getName()).isEqualTo("校园鲜果铺");
        assertThat(myMerchant.getOwnerUserId()).isEqualTo(TEST_USER_ID);
    }
}
