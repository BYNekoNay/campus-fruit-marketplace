package com.campusfruit.review.controller;

import com.campusfruit.review.entity.RatingAggregate;
import com.campusfruit.review.entity.Review;
import com.campusfruit.review.repository.RatingAggregateRepository;
import com.campusfruit.review.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价数据投影导出控制器。供内部服务调用（如数据分析、推荐系统）。
 */
@RestController
@RequestMapping("/api/internal/review")
public class ReviewProjectionExportController {

    private final ReviewRepository reviewRepository;
    private final RatingAggregateRepository ratingAggregateRepository;

    public ReviewProjectionExportController(ReviewRepository reviewRepository,
                                             RatingAggregateRepository ratingAggregateRepository) {
        this.reviewRepository = reviewRepository;
        this.ratingAggregateRepository = ratingAggregateRepository;
    }

    /**
     * 导出评价数据投影。
     * 供数据分析/推荐系统批量获取评价数据。
     */
    @GetMapping("/projection/export")
    public ResponseEntity<List<ReviewExportDto>> exportProjections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Review> reviewPage = reviewRepository.findAll(pageRequest);

        List<ReviewExportDto> exports = reviewPage.getContent().stream()
                .map(r -> {
                    ReviewExportDto dto = new ReviewExportDto();
                    dto.setId(r.getId());
                    dto.setUserId(r.getUserId());
                    dto.setStoreId(r.getStoreId());
                    dto.setOrderId(r.getOrderId());
                    dto.setRating(r.getRating());
                    dto.setStatus(r.getStatus().name());
                    dto.setCreatedAt(r.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(exports);
    }

    /**
     * 分页导出评分聚合数据。
     * 供数据分析/推荐系统使用。
     */
    @GetMapping("/rating-aggregates")
    public ResponseEntity<RatingAggregatePageDto> exportRatingAggregates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("storeId").ascending());
        Page<RatingAggregate> aggPage = ratingAggregateRepository.findAll(pageRequest);

        List<RatingAggregateDto> items = aggPage.getContent().stream()
                .map(a -> {
                    RatingAggregateDto dto = new RatingAggregateDto();
                    dto.setStoreId(a.getStoreId());
                    dto.setAvgRating(a.getAvgRating());
                    dto.setBayesianRating(a.getBayesianRating());
                    dto.setTotalRatings(a.getTotalRatings());
                    dto.setRatingDistribution(a.getRatingDistribution());
                    dto.setVersion(a.getVersion());
                    dto.setCalculatedAt(a.getCalculatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        RatingAggregatePageDto result = new RatingAggregatePageDto();
        result.setItems(items);
        result.setPage(page);
        result.setSize(size);
        result.setTotalElements(aggPage.getTotalElements());
        result.setTotalPages(aggPage.getTotalPages());

        return ResponseEntity.ok(result);
    }

    /**
     * 内部投影 DTO。
     */
    public static class ReviewExportDto {
        private Long id;
        private Long userId;
        private Long storeId;
        private Long orderId;
        private Integer rating;
        private String status;
        private java.time.Instant createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    }

    /**
     * 评分聚合导出 DTO。
     */
    public static class RatingAggregateDto {
        private Long storeId;
        private BigDecimal avgRating;
        private BigDecimal bayesianRating;
        private Integer totalRatings;
        private String ratingDistribution;
        private Integer version;
        private java.time.Instant calculatedAt;

        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }

        public BigDecimal getAvgRating() { return avgRating; }
        public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

        public BigDecimal getBayesianRating() { return bayesianRating; }
        public void setBayesianRating(BigDecimal bayesianRating) { this.bayesianRating = bayesianRating; }

        public Integer getTotalRatings() { return totalRatings; }
        public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }

        public String getRatingDistribution() { return ratingDistribution; }
        public void setRatingDistribution(String ratingDistribution) { this.ratingDistribution = ratingDistribution; }

        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }

        public java.time.Instant getCalculatedAt() { return calculatedAt; }
        public void setCalculatedAt(java.time.Instant calculatedAt) { this.calculatedAt = calculatedAt; }
    }

    /**
     * 分页结果包装 DTO。
     */
    public static class RatingAggregatePageDto {
        private List<RatingAggregateDto> items;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public List<RatingAggregateDto> getItems() { return items; }
        public void setItems(List<RatingAggregateDto> items) { this.items = items; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
}
