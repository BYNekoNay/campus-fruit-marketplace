package com.campusfruit.review.moderation;

import com.campusfruit.review.entity.ReviewReport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 举报审核控制器。
 */
@RestController
public class ModerationController {

    private final ReportService reportService;

    public ModerationController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * POST /api/reports — 用户举报评价
     */
    @PostMapping("/api/reports")
    public ResponseEntity<?> submitReport(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportRequest request) {
        Long userId = extractUserId(jwt);
        ReviewReport report = reportService.submitReport(userId, request.getReviewId(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "reportId", report.getId(),
                "status", report.getStatus().name(),
                "message", "举报已提交，等待审核"
        ));
    }

    /**
     * GET /api/admin/reports — 管理员查看待审核举报列表
     */
    @GetMapping("/api/admin/reports")
    public ResponseEntity<Page<ReviewReport>> listReports(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewReport> reports = reportService.listPendingReports(pageRequest);
        return ResponseEntity.ok(reports);
    }

    /**
     * PUT /api/admin/reports/{id}/review — 管理员审核举报
     */
    @PutMapping("/api/admin/reports/{id}/review")
    public ResponseEntity<?> reviewReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReviewActionRequest request) {
        Long adminId = extractUserId(jwt);
        reportService.reviewReport(id, adminId, request.getAction(), request.getComment());
        return ResponseEntity.ok(Map.of(
                "reportId", id,
                "action", request.getAction(),
                "message", "审核完成"
        ));
    }

    /**
     * PUT /api/admin/reviews/{id}/restore — 管理员恢复被隐藏评价
     */
    @PutMapping("/api/admin/reviews/{id}/restore")
    public ResponseEntity<?> restoreReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        Long adminId = extractUserId(jwt);
        reportService.restoreReview(adminId, id);
        return ResponseEntity.ok(Map.of(
                "reviewId", id,
                "message", "评价已恢复"
        ));
    }

    /**
     * PUT /api/admin/reviews/{id}/hide — 管理员直接隐藏评价
     */
    @PutMapping("/api/admin/reviews/{id}/hide")
    public ResponseEntity<?> hideReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody HideReviewRequest request) {
        Long adminId = extractUserId(jwt);
        reportService.hideReview(adminId, id, request.getReason());
        return ResponseEntity.ok(Map.of(
                "reviewId", id,
                "message", "评价已隐藏"
        ));
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    // --- 请求 DTO ---

    public static class ReportRequest {
        @NotNull(message = "评价ID不能为空")
        private Long reviewId;

        @NotBlank(message = "举报原因不能为空")
        private String reason;

        public Long getReviewId() { return reviewId; }
        public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ReviewActionRequest {
        @NotBlank(message = "审核动作不能为空")
        private String action;

        private String comment;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class HideReviewRequest {
        @NotBlank(message = "隐藏原因不能为空")
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
