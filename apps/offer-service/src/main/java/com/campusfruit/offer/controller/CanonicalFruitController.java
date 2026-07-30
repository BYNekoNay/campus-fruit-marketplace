package com.campusfruit.offer.controller;

import com.campusfruit.offer.dto.CanonicalFruitRequest;
import com.campusfruit.offer.dto.CanonicalFruitResponse;
import com.campusfruit.offer.service.CanonicalFruitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CanonicalFruitController {

    private final CanonicalFruitService canonicalFruitService;

    public CanonicalFruitController(CanonicalFruitService canonicalFruitService) {
        this.canonicalFruitService = canonicalFruitService;
    }

    /**
     * 创建标准水果（管理员）
     */
    @PostMapping("/admin/fruits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CanonicalFruitResponse> createFruit(@Valid @RequestBody CanonicalFruitRequest request) {
        CanonicalFruitResponse response = canonicalFruitService.createFruit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 更新标准水果（管理员）
     */
    @PutMapping("/admin/fruits/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CanonicalFruitResponse> updateFruit(@PathVariable Long id,
                                                              @Valid @RequestBody CanonicalFruitRequest request) {
        CanonicalFruitResponse response = canonicalFruitService.updateFruit(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 分页列表（管理员）
     */
    @GetMapping("/admin/fruits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CanonicalFruitResponse>> listFruits() {
        List<CanonicalFruitResponse> fruits = canonicalFruitService.getActiveFruits();
        return ResponseEntity.ok(fruits);
    }

    /**
     * 搜索水果（公开）
     */
    @GetMapping("/fruits/search")
    public ResponseEntity<List<CanonicalFruitResponse>> searchFruits(@RequestParam("keyword") String keyword) {
        List<CanonicalFruitResponse> fruits = canonicalFruitService.searchFruits(keyword);
        return ResponseEntity.ok(fruits);
    }

    /**
     * 按品类查询（公开）
     */
    @GetMapping("/fruits/category/{category}")
    public ResponseEntity<List<CanonicalFruitResponse>> getFruitsByCategory(@PathVariable String category) {
        List<CanonicalFruitResponse> fruits = canonicalFruitService.getFruitsByCategory(category);
        return ResponseEntity.ok(fruits);
    }
}
