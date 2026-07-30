package com.campusfruit.discovery.repository;

import com.campusfruit.discovery.entity.StoreOfferProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreOfferProjectionRepository extends JpaRepository<StoreOfferProjection, Long> {

    Optional<StoreOfferProjection> findByOfferId(Long offerId);

    List<StoreOfferProjection> findByStoreId(Long storeId);

    List<StoreOfferProjection> findByCanonicalFruitId(Long canonicalFruitId);

    /**
     * 关键词搜索：模糊匹配 fruit_variety。
     */
    @Query("SELECT s FROM StoreOfferProjection s WHERE s.merchantStatus = 'APPROVED' AND s.fruitVariety LIKE %:keyword%")
    Page<StoreOfferProjection> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 关键词 + 分类搜索。
     */
    @Query("SELECT s FROM StoreOfferProjection s WHERE s.merchantStatus = 'APPROVED' AND s.fruitVariety LIKE %:keyword% AND s.fruitCategory = :category")
    Page<StoreOfferProjection> searchByKeywordAndCategory(@Param("keyword") String keyword,
                                                          @Param("category") String category,
                                                          Pageable pageable);

    /**
     * 范围框地理查询（近似距离过滤）。
     */
    @Query("SELECT s FROM StoreOfferProjection s WHERE s.merchantStatus = 'APPROVED' AND s.storeLat BETWEEN :latMin AND :latMax AND s.storeLng BETWEEN :lngMin AND :lngMax")
    Page<StoreOfferProjection> findByBoundingBox(@Param("latMin") Double latMin,
                                                  @Param("latMax") Double latMax,
                                                  @Param("lngMin") Double lngMin,
                                                  @Param("lngMax") Double lngMax,
                                                  Pageable pageable);

    /**
     * 范围框 + 关键词搜索。
     */
    @Query("SELECT s FROM StoreOfferProjection s WHERE s.merchantStatus = 'APPROVED' AND s.storeLat BETWEEN :latMin AND :latMax AND s.storeLng BETWEEN :lngMin AND :lngMax AND s.fruitVariety LIKE %:keyword%")
    Page<StoreOfferProjection> findByBoundingBoxAndKeyword(@Param("latMin") Double latMin,
                                                            @Param("latMax") Double latMax,
                                                            @Param("lngMin") Double lngMin,
                                                            @Param("lngMax") Double lngMax,
                                                            @Param("keyword") String keyword,
                                                            Pageable pageable);

    /**
     * 价格区间过滤。
     */
    @Query("SELECT s FROM StoreOfferProjection s WHERE s.merchantStatus = 'APPROVED' AND s.standardPricePer500g BETWEEN :minPrice AND :maxPrice")
    Page<StoreOfferProjection> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                                 @Param("maxPrice") BigDecimal maxPrice,
                                                 Pageable pageable);

    /**
     * 按品类查询所有品类（去重）。
     */
    @Query("SELECT DISTINCT s.fruitCategory FROM StoreOfferProjection s WHERE s.merchantStatus = 'APPROVED' AND s.fruitCategory IS NOT NULL ORDER BY s.fruitCategory")
    List<String> findDistinctCategories();

    /**
     * 查询最小评分以上的报价。
     */
    @Query("SELECT s FROM StoreOfferProjection s WHERE s.merchantStatus = 'APPROVED' AND s.avgRating >= :minRating")
    Page<StoreOfferProjection> findByMinRating(@Param("minRating") BigDecimal minRating, Pageable pageable);

    /**
     * 按 offerIds 批量查询。
     */
    List<StoreOfferProjection> findByOfferIdIn(List<Long> offerIds);
}
