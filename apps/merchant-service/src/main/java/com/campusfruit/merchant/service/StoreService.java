package com.campusfruit.merchant.service;

import com.campusfruit.merchant.dto.*;
import com.campusfruit.merchant.entity.Store;
import com.campusfruit.merchant.entity.StoreStaff;
import com.campusfruit.merchant.entity.Merchant;
import com.campusfruit.merchant.enums.StoreStatus;
import com.campusfruit.merchant.repository.MerchantRepository;
import com.campusfruit.merchant.repository.StoreRepository;
import com.campusfruit.merchant.repository.StoreStaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);

    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final MerchantRepository merchantRepository;
    private final AuditService auditService;
    private final MerchantEventPublisher eventPublisher;

    public StoreService(StoreRepository storeRepository,
                        StoreStaffRepository storeStaffRepository,
                        MerchantRepository merchantRepository,
                        AuditService auditService,
                        MerchantEventPublisher eventPublisher) {
        this.storeRepository = storeRepository;
        this.storeStaffRepository = storeStaffRepository;
        this.merchantRepository = merchantRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建门店（PENDING_APPROVAL 状态，需管理员审核）。
     */
    @Transactional
    public StoreResponse createStore(Long merchantId, CreateStoreRequest dto) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("商家不存在: " + merchantId));

        Store store = new Store();
        store.setMerchant(merchant);
        store.setName(dto.getName());
        store.setAddress(dto.getAddress());
        store.setLatitude(dto.getLatitude());
        store.setLongitude(dto.getLongitude());
        if (dto.getCoordType() != null) {
            store.setCoordType(dto.getCoordType());
        }
        store.setPhone(dto.getPhone());
        store.setBusinessHours(dto.getBusinessHours());
        if (dto.getPickupLeadMinutes() != null) {
            store.setPickupLeadMinutes(dto.getPickupLeadMinutes());
        }
        store.setStatus(StoreStatus.PENDING_APPROVAL);

        store = storeRepository.save(store);

        auditService.log(merchant.getOwnerUserId(), "MERCHANT_OWNER", "STORE_CREATE",
                "Store", String.valueOf(store.getId()),
                null, store.getName(), null);

        log.info("Store created: id={}, name={}, merchantId={}", store.getId(), store.getName(), merchantId);

        return toStoreResponse(store, Collections.emptyList());
    }

    /**
     * 更新门店信息（变更地址需要重新审核）。
     */
    @Transactional
    public StoreResponse updateStore(Long storeId, UpdateStoreRequest dto) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        String oldAddress = store.getAddress();
        boolean addressChanged = false;

        if (dto.getName() != null) {
            store.setName(dto.getName());
        }
        if (dto.getAddress() != null && !dto.getAddress().equals(store.getAddress())) {
            store.setAddress(dto.getAddress());
            addressChanged = true;
        }
        if (dto.getLatitude() != null) {
            store.setLatitude(dto.getLatitude());
        }
        if (dto.getLongitude() != null) {
            store.setLongitude(dto.getLongitude());
        }
        if (dto.getCoordType() != null) {
            store.setCoordType(dto.getCoordType());
        }
        if (dto.getPhone() != null) {
            store.setPhone(dto.getPhone());
        }
        if (dto.getBusinessHours() != null) {
            store.setBusinessHours(dto.getBusinessHours());
        }
        if (dto.getPickupLeadMinutes() != null) {
            store.setPickupLeadMinutes(dto.getPickupLeadMinutes());
        }

        // 地址变更触发重新审核
        if (addressChanged) {
            store.setStatus(StoreStatus.PENDING_APPROVAL);
            eventPublisher.publishStoreLocationChanged(store);
        }

        store = storeRepository.save(store);

        auditService.log(store.getMerchant().getOwnerUserId(), "MERCHANT_OWNER", "STORE_UPDATE",
                "Store", String.valueOf(store.getId()),
                oldAddress, store.getAddress(), null);

        List<StoreStaff> staffList = storeStaffRepository.findByStoreId(store.getId());
        return toStoreResponse(store, staffList);
    }

    /**
     * 管理员审核通过门店。
     */
    @Transactional
    public StoreResponse approveStore(Long storeId, Long adminId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        StoreStatus oldStatus = store.getStatus();
        store.setStatus(StoreStatus.ACTIVE);
        store = storeRepository.save(store);

        auditService.log(adminId, "ADMIN", "STORE_APPROVE",
                "Store", String.valueOf(store.getId()),
                oldStatus.name(), store.getStatus().name(), null);

        eventPublisher.publishStoreActivated(store);

        log.info("Store approved: id={}, adminId={}", store.getId(), adminId);

        List<StoreStaff> staffList = storeStaffRepository.findByStoreId(store.getId());
        return toStoreResponse(store, staffList);
    }

    /**
     * 管理员暂停门店。
     */
    @Transactional
    public StoreResponse suspendStore(Long storeId, Long adminId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        StoreStatus oldStatus = store.getStatus();
        store.setStatus(StoreStatus.SUSPENDED);
        store = storeRepository.save(store);

        auditService.log(adminId, "ADMIN", "STORE_SUSPEND",
                "Store", String.valueOf(store.getId()),
                oldStatus.name(), store.getStatus().name(), null);

        eventPublisher.publishStoreSuspended(store);

        log.info("Store suspended: id={}, adminId={}", store.getId(), adminId);

        List<StoreStaff> staffList = storeStaffRepository.findByStoreId(store.getId());
        return toStoreResponse(store, staffList);
    }

    /**
     * 管理员激活门店。
     */
    @Transactional
    public StoreResponse activateStore(Long storeId, Long adminId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        StoreStatus oldStatus = store.getStatus();
        store.setStatus(StoreStatus.ACTIVE);
        store = storeRepository.save(store);

        auditService.log(adminId, "ADMIN", "STORE_ACTIVATE",
                "Store", String.valueOf(store.getId()),
                oldStatus.name(), store.getStatus().name(), null);

        eventPublisher.publishStoreActivated(store);

        log.info("Store activated: id={}, adminId={}", store.getId(), adminId);

        List<StoreStaff> staffList = storeStaffRepository.findByStoreId(store.getId());
        return toStoreResponse(store, staffList);
    }

    /**
     * 关闭门店。
     */
    @Transactional
    public StoreResponse closeStore(Long storeId, Long operatorId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        StoreStatus oldStatus = store.getStatus();
        store.setStatus(StoreStatus.CLOSED);
        store = storeRepository.save(store);

        auditService.log(operatorId, "MERCHANT_OWNER", "STORE_CLOSE",
                "Store", String.valueOf(store.getId()),
                oldStatus.name(), store.getStatus().name(), null);

        List<StoreStaff> staffList = storeStaffRepository.findByStoreId(store.getId());
        return toStoreResponse(store, staffList);
    }

    /**
     * 添加门店员工。
     */
    @Transactional
    public StaffResponse addStaff(Long storeId, Long userId, String role) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        if (storeStaffRepository.findByStoreIdAndUserId(storeId, userId).isPresent()) {
            throw new IllegalArgumentException("该用户已是门店员工");
        }

        StoreStaff staff = new StoreStaff();
        staff.setStore(store);
        staff.setUserId(userId);
        staff.setRole(role != null ? role : "STAFF");
        staff = storeStaffRepository.save(staff);

        auditService.log(store.getMerchant().getOwnerUserId(), "MERCHANT_OWNER", "STAFF_ADD",
                "StoreStaff", String.valueOf(staff.getId()),
                null, String.format("store=%d, user=%d, role=%s", storeId, userId, staff.getRole()), null);

        return new StaffResponse(staff.getUserId(), staff.getRole(), store.getName());
    }

    /**
     * 移除门店员工。
     */
    @Transactional
    public void removeStaff(Long storeId, Long userId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        StoreStaff staff = storeStaffRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("员工不存在"));

        storeStaffRepository.delete(staff);

        auditService.log(store.getMerchant().getOwnerUserId(), "MERCHANT_OWNER", "STAFF_REMOVE",
                "StoreStaff", String.valueOf(staff.getId()),
                String.format("role=%s", staff.getRole()), null, null);
    }

    /**
     * 查询门店详情。
     */
    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        List<StoreStaff> staffList = storeStaffRepository.findByStoreId(storeId);
        return toStoreResponse(store, staffList);
    }

    /**
     * 查询商家的所有门店。
     */
    @Transactional(readOnly = true)
    public List<StoreResponse> getStoresByMerchant(Long merchantId) {
        List<Store> stores = storeRepository.findByMerchantId(merchantId);
        return stores.stream().map(store -> {
            List<StoreStaff> staffList = storeStaffRepository.findByStoreId(store.getId());
            return toStoreResponse(store, staffList);
        }).collect(Collectors.toList());
    }

    // --- 转换方法 ---

    private StoreResponse toStoreResponse(Store store, List<StoreStaff> staffList) {
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
        resp.setStatusText(getStatusText(store.getStatus()));
        resp.setPickupLeadMinutes(store.getPickupLeadMinutes());
        resp.setCreatedAt(store.getCreatedAt());
        resp.setUpdatedAt(store.getUpdatedAt());
        resp.setStaff(staffList.stream()
                .map(s -> new StaffResponse(s.getUserId(), s.getRole(), store.getName()))
                .collect(Collectors.toList()));
        return resp;
    }

    private String getStatusText(StoreStatus status) {
        return switch (status) {
            case PENDING_APPROVAL -> "待审核";
            case ACTIVE -> "营业中";
            case CLOSED -> "已关闭";
            case SUSPENDED -> "已暂停";
        };
    }
}
