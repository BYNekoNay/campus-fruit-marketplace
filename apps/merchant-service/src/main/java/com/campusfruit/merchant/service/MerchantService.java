package com.campusfruit.merchant.service;

import com.campusfruit.merchant.dto.*;
import com.campusfruit.merchant.entity.Merchant;
import com.campusfruit.merchant.entity.Store;
import com.campusfruit.merchant.entity.StoreStaff;
import com.campusfruit.merchant.enums.MerchantStatus;
import com.campusfruit.merchant.enums.StoreStatus;
import com.campusfruit.merchant.repository.MerchantRepository;
import com.campusfruit.merchant.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);

    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;
    private final AuditService auditService;
    private final MerchantEventPublisher eventPublisher;

    public MerchantService(MerchantRepository merchantRepository,
                           StoreRepository storeRepository,
                           AuditService auditService,
                           MerchantEventPublisher eventPublisher) {
        this.merchantRepository = merchantRepository;
        this.storeRepository = storeRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建商家入驻申请（PENDING_REVIEW 状态）。
     */
    @Transactional
    public MerchantResponse createMerchant(CreateMerchantRequest dto, Long ownerUserId) {
        Merchant merchant = new Merchant();
        merchant.setOwnerUserId(ownerUserId);
        merchant.setName(dto.getName());
        merchant.setContactName(dto.getContactName());
        merchant.setContactPhone(dto.getContactPhone());
        merchant.setLicenseNumber(dto.getLicenseNumber());
        merchant.setStatus(MerchantStatus.PENDING_REVIEW);

        merchant = merchantRepository.save(merchant);

        auditService.log(ownerUserId, "USER", "MERCHANT_APPLY",
                "Merchant", String.valueOf(merchant.getId()),
                null, merchant.getName(), null);

        log.info("Merchant application created: id={}, name={}, ownerUserId={}",
                merchant.getId(), merchant.getName(), ownerUserId);

        return toMerchantResponse(merchant, Collections.emptyList());
    }

    /**
     * 审核商家（管理员操作）。
     */
    @Transactional
    public MerchantResponse reviewMerchant(Long merchantId, ReviewMerchantRequest dto, Long reviewerId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("商家不存在: " + merchantId));

        MerchantStatus oldStatus = merchant.getStatus();

        if ("APPROVE".equalsIgnoreCase(dto.getAction())) {
            merchant.setStatus(MerchantStatus.APPROVED);
            merchant.setReviewedAt(Instant.now());
        } else if ("REJECT".equalsIgnoreCase(dto.getAction())) {
            if (dto.getReason() == null || dto.getReason().isBlank()) {
                throw new IllegalArgumentException("拒绝时必须提供原因");
            }
            merchant.setStatus(MerchantStatus.REJECTED);
            merchant.setRejectReason(dto.getReason());
            merchant.setReviewedAt(Instant.now());
        } else {
            throw new IllegalArgumentException("无效的操作: " + dto.getAction());
        }

        merchant.setReviewedBy(reviewerId);
        merchant = merchantRepository.save(merchant);

        auditService.log(reviewerId, "ADMIN", "MERCHANT_" + dto.getAction().toUpperCase(),
                "Merchant", String.valueOf(merchant.getId()),
                oldStatus.name(), merchant.getStatus().name(), dto.getReason());

        // 审核通过时发布事件
        if (merchant.getStatus() == MerchantStatus.APPROVED) {
            eventPublisher.publishMerchantApproved(merchant);
        }

        log.info("Merchant reviewed: id={}, action={}, reviewerId={}",
                merchant.getId(), dto.getAction(), reviewerId);

        List<Store> stores = storeRepository.findByMerchantId(merchant.getId());
        return toMerchantResponse(merchant, stores);
    }

    /**
     * 根据 ID 查询商家详情（只返回已审核通过的）。
     */
    @Transactional(readOnly = true)
    public MerchantResponse getMerchantById(Long id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商家不存在: " + id));

        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            throw new IllegalArgumentException("商家未通过审核");
        }

        List<Store> stores = storeRepository.findByMerchantId(merchant.getId());
        return toMerchantResponse(merchant, stores);
    }

    /**
     * 查询当前用户的商家。
     */
    @Transactional(readOnly = true)
    public MerchantResponse getMyMerchant(Long ownerUserId) {
        Merchant merchant = merchantRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("您尚未入驻商家"));

        List<Store> stores = storeRepository.findByMerchantId(merchant.getId());
        return toMerchantResponse(merchant, stores);
    }

    /**
     * 分页查询商家列表（管理员）。
     */
    @Transactional(readOnly = true)
    public Page<MerchantResponse> listMerchants(Pageable pageable) {
        return merchantRepository.findAll(pageable).map(m -> {
            List<Store> stores = storeRepository.findByMerchantId(m.getId());
            return toMerchantResponse(m, stores);
        });
    }

    /**
     * 分页查询待审核商家列表（管理员）。
     */
    @Transactional(readOnly = true)
    public Page<MerchantResponse> listPendingReviewMerchants(Pageable pageable) {
        return merchantRepository.findByStatus(MerchantStatus.PENDING_REVIEW, pageable)
                .map(m -> toMerchantResponse(m, Collections.emptyList()));
    }

    /**
     * 更新商家基本信息（仅商家所有者可操作）。
     */
    @Transactional
    public MerchantResponse updateMerchant(Long merchantId, UpdateMerchantRequest dto) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("商家不存在: " + merchantId));

        String oldName = merchant.getName();

        if (dto.getName() != null) {
            merchant.setName(dto.getName());
        }
        if (dto.getContactName() != null) {
            merchant.setContactName(dto.getContactName());
        }
        if (dto.getContactPhone() != null) {
            merchant.setContactPhone(dto.getContactPhone());
        }

        merchant = merchantRepository.save(merchant);

        auditService.log(merchant.getOwnerUserId(), "MERCHANT_OWNER", "MERCHANT_UPDATE",
                "Merchant", String.valueOf(merchant.getId()),
                oldName, merchant.getName(), null);

        List<Store> stores = storeRepository.findByMerchantId(merchant.getId());
        return toMerchantResponse(merchant, stores);
    }

    // --- 转换方法 ---

    private MerchantResponse toMerchantResponse(Merchant merchant, List<Store> stores) {
        MerchantResponse resp = new MerchantResponse();
        resp.setId(merchant.getId());
        resp.setOwnerUserId(merchant.getOwnerUserId());
        resp.setName(merchant.getName());
        resp.setContactName(merchant.getContactName());
        resp.setContactPhone(merchant.getContactPhone());
        resp.setLicenseNumber(merchant.getLicenseNumber());
        resp.setStatus(merchant.getStatus());
        resp.setStatusText(getStatusText(merchant.getStatus()));
        resp.setRejectReason(merchant.getRejectReason());
        resp.setReviewedBy(merchant.getReviewedBy());
        resp.setReviewedAt(merchant.getReviewedAt());
        resp.setCreatedAt(merchant.getCreatedAt());
        resp.setUpdatedAt(merchant.getUpdatedAt());
        resp.setStores(stores.stream()
                .map(this::toStoreResponse)
                .collect(Collectors.toList()));
        return resp;
    }

    private StoreResponse toStoreResponse(Store store) {
        StoreResponse resp = new StoreResponse();
        resp.setId(store.getId());
        resp.setMerchantId(store.getMerchant().getId());
        resp.setName(store.getName());
        resp.setAddress(store.getAddress());
        resp.setLatitude(store.getLatitude());
        resp.setLongitude(store.getLongitude());
        resp.setCoordType(store.getCoordType());
        resp.setPhone(store.getPhone());
        resp.setBusinessHours(store.getBusinessHours());
        resp.setStatus(store.getStatus());
        resp.setStatusText(getStoreStatusText(store.getStatus()));
        resp.setPickupLeadMinutes(store.getPickupLeadMinutes());
        resp.setCreatedAt(store.getCreatedAt());
        resp.setUpdatedAt(store.getUpdatedAt());
        resp.setStaff(Collections.emptyList());
        return resp;
    }

    private String getStatusText(MerchantStatus status) {
        return switch (status) {
            case PENDING_REVIEW -> "待审核";
            case APPROVED -> "已通过";
            case REJECTED -> "已拒绝";
            case SUSPENDED -> "已暂停";
        };
    }

    private String getStoreStatusText(StoreStatus status) {
        return switch (status) {
            case PENDING_APPROVAL -> "待审核";
            case ACTIVE -> "营业中";
            case CLOSED -> "已关闭";
            case SUSPENDED -> "已暂停";
        };
    }
}
