package com.campusfruit.offer.controller;

import com.campusfruit.offer.dto.CanonicalFruitRequest;
import com.campusfruit.offer.service.CanonicalFruitService;
import com.campusfruit.observability.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 标准水果目录候选映射。
 * 商家找不到标准水果时提交候选映射申请；管理员可审核创建/驳回。
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogMappingController {

    private final CanonicalFruitService canonicalFruitService;

    public CatalogMappingController(CanonicalFruitService canonicalFruitService) {
        this.canonicalFruitService = canonicalFruitService;
    }

    @PostMapping("/mapping-requests")
    public ResponseEntity<?> submitMappingRequest(@RequestBody Map<String, String> body) {
        String variety = body.get("variety");
        if (variety == null || variety.isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiError.of("VARIETY_REQUIRED", "请填写新品种名称"));
        }

        var request = new CanonicalFruitRequest();
        request.setCategory(body.getOrDefault("category", "其他"));
        request.setVariety(variety);
        request.setGrade(body.getOrDefault("suggestGrade", "待定"));
        request.setOrigin(body.getOrDefault("suggestOrigin", "待确认"));
        request.setDefaultUnit("g");

        var fruit = canonicalFruitService.createFruit(request);
        canonicalFruitService.deactivateFruit(fruit.getId());

        return ResponseEntity.accepted().body(Map.of(
            "message", "候选映射已提交，管理员审核通过后可公开报价",
            "fruitId", fruit.getId()
        ));
    }
}
