package com.campusfruit.offer.controller;

import com.campusfruit.offer.dto.*;
import com.campusfruit.offer.entity.PriceHistory;
import com.campusfruit.offer.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * 创建报价（需商家/员工）
     */
    @PostMapping("/stores/{storeId}/offers")
    @PreAuthorize("hasAnyRole('MERCHANT', 'STORE_STAFF')")
    public ResponseEntity<OfferResponse> createOffer(@PathVariable Long storeId,
                                                     @Valid @RequestBody CreateOfferRequest request) {
        request.setStoreId(storeId);
        OfferResponse response = offerService.createOffer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 更新报价
     */
    @PutMapping("/offers/{id}")
    @PreAuthorize("hasAnyRole('MERCHANT', 'STORE_STAFF')")
    public ResponseEntity<OfferResponse> updateOffer(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateOfferRequest request) {
        OfferResponse response = offerService.updateOffer(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 确认价格未变
     */
    @PostMapping("/offers/{id}/confirm")
    @PreAuthorize("hasAnyRole('MERCHANT', 'STORE_STAFF')")
    public ResponseEntity<OfferResponse> confirmPrice(@PathVariable Long id) {
        OfferResponse response = offerService.confirmPrice(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 查看报价详情
     */
    @GetMapping("/offers/{id}")
    public ResponseEntity<OfferResponse> getOfferById(@PathVariable Long id) {
        OfferResponse response = offerService.getOfferById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 门店报价列表
     */
    @GetMapping("/stores/{storeId}/offers")
    public ResponseEntity<List<OfferResponse>> getOffersByStore(@PathVariable Long storeId) {
        List<OfferResponse> offers = offerService.getOffersByStore(storeId);
        return ResponseEntity.ok(offers);
    }

    /**
     * 按水果查报价
     */
    @GetMapping("/fruits/{fruitId}/offers")
    public ResponseEntity<List<OfferResponse>> getOffersByFruit(@PathVariable Long fruitId) {
        List<OfferResponse> offers = offerService.getOffersByFruit(fruitId);
        return ResponseEntity.ok(offers);
    }

    /**
     * 价格历史
     */
    @GetMapping("/offers/{id}/price-history")
    public ResponseEntity<List<PriceHistory>> getPriceHistory(@PathVariable Long id) {
        List<PriceHistory> history = offerService.getPriceHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * 暂停报价
     */
    @PostMapping("/offers/{id}/pause")
    @PreAuthorize("hasAnyRole('MERCHANT', 'STORE_STAFF')")
    public ResponseEntity<OfferResponse> pauseOffer(@PathVariable Long id) {
        OfferResponse response = offerService.pauseOffer(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 激活报价
     */
    @PostMapping("/offers/{id}/activate")
    @PreAuthorize("hasAnyRole('MERCHANT', 'STORE_STAFF')")
    public ResponseEntity<OfferResponse> activateOffer(@PathVariable Long id) {
        OfferResponse response = offerService.activateOffer(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 库存预占（内部调用）
     */
    @PostMapping("/internal/offers/reserve")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = offerService.reserve(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
