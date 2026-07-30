package com.campusfruit.merchant.controller;

import com.campusfruit.merchant.dto.*;
import com.campusfruit.merchant.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * 创建门店（需商家所有者）。
     */
    @PostMapping("/api/merchant/{merchantId}/stores")
    public ResponseEntity<StoreResponse> createStore(
            @PathVariable Long merchantId,
            @Valid @RequestBody CreateStoreRequest request) {
        StoreResponse response = storeService.createStore(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 查询商家的所有门店。
     */
    @GetMapping("/api/merchant/{merchantId}/stores")
    public ResponseEntity<List<StoreResponse>> getMerchantStores(
            @PathVariable Long merchantId) {
        List<StoreResponse> stores = storeService.getStoresByMerchant(merchantId);
        return ResponseEntity.ok(stores);
    }

    /**
     * 查询门店详情。
     */
    @GetMapping("/api/stores/{id}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long id) {
        StoreResponse response = storeService.getStoreById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新门店信息。
     */
    @PutMapping("/api/stores/{id}")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStoreRequest request) {
        StoreResponse response = storeService.updateStore(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 添加员工。
     */
    @PostMapping("/api/stores/{id}/staff")
    public ResponseEntity<StaffResponse> addStaff(
            @PathVariable("id") Long storeId,
            @Valid @RequestBody AddStaffRequest request) {
        String role = request.getRole() != null ? request.getRole() : "STAFF";
        StaffResponse response = storeService.addStaff(storeId, request.getUserId(), role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 移除员工。
     */
    @DeleteMapping("/api/stores/{id}/staff/{userId}")
    public ResponseEntity<Void> removeStaff(
            @PathVariable("id") Long storeId,
            @PathVariable Long userId) {
        storeService.removeStaff(storeId, userId);
        return ResponseEntity.noContent().build();
    }
}
