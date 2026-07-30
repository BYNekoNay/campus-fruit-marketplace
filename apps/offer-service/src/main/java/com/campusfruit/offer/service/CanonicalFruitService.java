package com.campusfruit.offer.service;

import com.campusfruit.offer.dto.CanonicalFruitRequest;
import com.campusfruit.offer.dto.CanonicalFruitResponse;
import com.campusfruit.offer.entity.CanonicalFruit;
import com.campusfruit.offer.enums.FruitStatus;
import com.campusfruit.offer.repository.CanonicalFruitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CanonicalFruitService {

    private final CanonicalFruitRepository repository;

    public CanonicalFruitService(CanonicalFruitRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CanonicalFruitResponse createFruit(CanonicalFruitRequest request) {
        CanonicalFruit fruit = new CanonicalFruit();
        fruit.setCategory(request.getCategory());
        fruit.setVariety(request.getVariety());
        fruit.setGrade(request.getGrade());
        fruit.setOrigin(request.getOrigin());
        fruit.setDefaultUnit(request.getDefaultUnit() != null ? request.getDefaultUnit() : "g");
        fruit.setComparisonGroupId(request.getComparisonGroupId());
        fruit.setStatus(FruitStatus.ACTIVE);

        CanonicalFruit saved = repository.save(fruit);
        return toResponse(saved);
    }

    @Transactional
    public CanonicalFruitResponse updateFruit(Long id, CanonicalFruitRequest request) {
        CanonicalFruit fruit = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标准水果不存在: " + id));

        fruit.setCategory(request.getCategory());
        fruit.setVariety(request.getVariety());
        fruit.setGrade(request.getGrade());
        fruit.setOrigin(request.getOrigin());
        if (request.getDefaultUnit() != null) {
            fruit.setDefaultUnit(request.getDefaultUnit());
        }
        fruit.setComparisonGroupId(request.getComparisonGroupId());
        // 更新版本号
        fruit.setVersion(fruit.getVersion() + 1);

        CanonicalFruit saved = repository.save(fruit);
        return toResponse(saved);
    }

    public List<CanonicalFruitResponse> getFruitsByCategory(String category) {
        return repository.findByCategory(category).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CanonicalFruitResponse> searchFruits(String keyword) {
        return repository.findByVarietyContainingOrCategoryContainingOrOriginContaining(
                        keyword, keyword, keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CanonicalFruitResponse> getActiveFruits() {
        return repository.findByStatus(FruitStatus.ACTIVE).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateFruit(Long id) {
        CanonicalFruit fruit = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标准水果不存在: " + id));
        fruit.setStatus(FruitStatus.INACTIVE);
        repository.save(fruit);
    }

    /**
     * 更新标准水果的比较分组。
     * 比较分组用于将同类型水果的价格进行对比统计。
     */
    @Transactional
    public void updateComparisonGroup(Long fruitId, Long groupId) {
        CanonicalFruit fruit = repository.findById(fruitId)
                .orElseThrow(() -> new IllegalArgumentException("标准水果不存在: " + fruitId));
        fruit.setComparisonGroupId(groupId);
        fruit.setVersion(fruit.getVersion() + 1);
        repository.save(fruit);
    }

    private CanonicalFruitResponse toResponse(CanonicalFruit fruit) {
        CanonicalFruitResponse resp = new CanonicalFruitResponse();
        resp.setId(fruit.getId());
        resp.setCategory(fruit.getCategory());
        resp.setVariety(fruit.getVariety());
        resp.setGrade(fruit.getGrade());
        resp.setOrigin(fruit.getOrigin());
        resp.setDefaultUnit(fruit.getDefaultUnit());
        resp.setComparisonGroupId(fruit.getComparisonGroupId());
        resp.setVersion(fruit.getVersion());
        resp.setStatus(fruit.getStatus().name());
        resp.setCreatedAt(fruit.getCreatedAt());
        resp.setUpdatedAt(fruit.getUpdatedAt());
        return resp;
    }
}
