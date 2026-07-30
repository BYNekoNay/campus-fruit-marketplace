package com.campusfruit.merchant.controller;

import com.campusfruit.merchant.dto.*;
import com.campusfruit.merchant.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 商家入驻申请。
     */
    @PostMapping("/apply")
    public ResponseEntity<MerchantResponse> apply(
            @Valid @RequestBody CreateMerchantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        MerchantResponse response = merchantService.createMerchant(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 查看我的商家。
     */
    @GetMapping("/my")
    public ResponseEntity<MerchantResponse> myMerchant(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        MerchantResponse response = merchantService.getMyMerchant(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 查看商家详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<MerchantResponse> getMerchant(@PathVariable Long id) {
        MerchantResponse response = merchantService.getMerchantById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 更新商家信息。
     */
    @PutMapping("/{id}")
    public ResponseEntity<MerchantResponse> updateMerchant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMerchantRequest request) {
        MerchantResponse response = merchantService.updateMerchant(id, request);
        return ResponseEntity.ok(response);
    }

    private Long getUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
