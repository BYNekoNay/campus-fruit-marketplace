package com.campusfruit.review.controller;

import com.campusfruit.review.dto.MerchantReplyRequest;
import com.campusfruit.review.entity.MerchantReply;
import com.campusfruit.review.service.MerchantReplyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class MerchantReplyController {

    private final MerchantReplyService replyService;

    public MerchantReplyController(MerchantReplyService replyService) {
        this.replyService = replyService;
    }

    /**
     * 商家回复评价。
     */
    @PostMapping("/api/reviews/{reviewId}/reply")
    public ResponseEntity<MerchantReply> addReply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId,
            @Valid @RequestBody MerchantReplyRequest request) {
        Long merchantId = extractMerchantId(jwt);
        Long storeId = extractStoreId(jwt);
        MerchantReply reply = replyService.addReply(merchantId, storeId, reviewId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    /**
     * 修改回复。
     */
    @PutMapping("/api/reviews/{reviewId}/reply")
    public ResponseEntity<MerchantReply> updateReply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId,
            @Valid @RequestBody MerchantReplyRequest request) {
        Long merchantId = extractMerchantId(jwt);
        Long storeId = extractStoreId(jwt);
        MerchantReply reply = replyService.updateReply(merchantId, storeId, reviewId, request.getContent());
        return ResponseEntity.ok(reply);
    }

    /**
     * 删除回复。
     */
    @DeleteMapping("/api/reviews/{reviewId}/reply")
    public ResponseEntity<Void> deleteReply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId) {
        Long merchantId = extractMerchantId(jwt);
        Long storeId = extractStoreId(jwt);
        replyService.deleteReply(merchantId, storeId, reviewId);
        return ResponseEntity.noContent().build();
    }

    private Long extractMerchantId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private Long extractStoreId(Jwt jwt) {
        String storeIdStr = jwt.getClaimAsString("store_id");
        return storeIdStr != null ? Long.parseLong(storeIdStr) : 0L;
    }
}
