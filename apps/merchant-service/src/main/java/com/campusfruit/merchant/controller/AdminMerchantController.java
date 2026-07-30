package com.campusfruit.merchant.controller;

import com.campusfruit.merchant.dto.*;
import com.campusfruit.merchant.service.MerchantService;
import com.campusfruit.merchant.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminMerchantController {

    private final MerchantService merchantService;
    private final StoreService storeService;

    public AdminMerchantController(MerchantService merchantService, StoreService storeService) {
        this.merchantService = merchantService;
        this.storeService = storeService;
    }

    /**
     * 分页查询所有商家。
     */
    @GetMapping("/merchants")
    public ResponseEntity<Page<MerchantResponse>> listMerchants(Pageable pageable) {
        Page<MerchantResponse> merchants = merchantService.listMerchants(pageable);
        return ResponseEntity.ok(merchants);
    }

    /**
     * 待审核商家列表。
     */
    @GetMapping("/merchants/pending")
    public ResponseEntity<Page<MerchantResponse>> listPendingMerchants(Pageable pageable) {
        Page<MerchantResponse> merchants = merchantService.listPendingReviewMerchants(pageable);
        return ResponseEntity.ok(merchants);
    }

    /**
     * 审核商家（APPROVE / REJECT）。
     */
    @PutMapping("/merchants/{id}/review")
    public ResponseEntity<MerchantResponse> reviewMerchant(
            @PathVariable Long id,
            @Valid @RequestBody ReviewMerchantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long adminId = Long.parseLong(jwt.getSubject());
        MerchantResponse response = merchantService.reviewMerchant(id, request, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * 管理员审核通过门店。
     */
    @PutMapping("/stores/{id}/approve")
    public ResponseEntity<StoreResponse> approveStore(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long adminId = Long.parseLong(jwt.getSubject());
        StoreResponse response = storeService.approveStore(id, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * 管理员暂停门店。
     */
    @PutMapping("/stores/{id}/suspend")
    public ResponseEntity<StoreResponse> suspendStore(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long adminId = Long.parseLong(jwt.getSubject());
        StoreResponse response = storeService.suspendStore(id, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * 管理员激活门店。
     */
    @PutMapping("/stores/{id}/activate")
    public ResponseEntity<StoreResponse> activateStore(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long adminId = Long.parseLong(jwt.getSubject());
        StoreResponse response = storeService.activateStore(id, adminId);
        return ResponseEntity.ok(response);
    }
}
