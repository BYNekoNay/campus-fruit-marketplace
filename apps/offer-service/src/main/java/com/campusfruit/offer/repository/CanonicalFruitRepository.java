package com.campusfruit.offer.repository;

import com.campusfruit.offer.entity.CanonicalFruit;
import com.campusfruit.offer.enums.FruitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanonicalFruitRepository extends JpaRepository<CanonicalFruit, Long> {

    List<CanonicalFruit> findByCategory(String category);

    List<CanonicalFruit> findByComparisonGroupId(Long comparisonGroupId);

    List<CanonicalFruit> findByStatus(FruitStatus status);

    List<CanonicalFruit> findByVarietyContainingOrCategoryContainingOrOriginContaining(
            String variety, String category, String origin);
}
