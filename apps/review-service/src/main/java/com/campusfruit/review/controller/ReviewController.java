package com.campusfruit.review.controller;

import com.campusfruit.review.dto.*;
import com.campusfruit.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 提交评价。
     */
    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewResponse> submitReview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmitReviewRequest request) {
        Long userId = extractUserId(jwt);
        ReviewResponse response = reviewService.submitReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 修改评价。
     */
    @PutMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        Long userId = extractUserId(jwt);
        ReviewResponse response = reviewService.updateReview(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 我的评价列表。
     */
    @GetMapping("/api/reviews/my")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = extractUserId(jwt);
        Page<ReviewResponse> reviews = reviewService.getMyReviews(userId, page, size);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 删除评价。
     */
    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        Long userId = extractUserId(jwt);
        reviewService.deleteReview(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
