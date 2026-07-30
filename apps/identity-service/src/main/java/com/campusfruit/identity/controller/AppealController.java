package com.campusfruit.identity.controller;

import com.campusfruit.identity.entity.User;
import com.campusfruit.identity.entity.UserAppeal;
import com.campusfruit.identity.enums.AppealStatus;
import com.campusfruit.identity.enums.UserStatus;
import com.campusfruit.identity.service.AppealService;
import com.campusfruit.identity.service.AuditService;
import com.campusfruit.identity.service.UserService;
import com.campusfruit.observability.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appeals")
public class AppealController {

    private static final Logger log = LoggerFactory.getLogger(AppealController.class);

    private final AppealService appealService;
    private final UserService userService;
    private final AuditService auditService;

    public AppealController(AppealService appealService, UserService userService,
                            AuditService auditService) {
        this.appealService = appealService;
        this.userService = userService;
        this.auditService = auditService;
    }

    /**
     * 提交申诉（需冻结用户）。
     */
    @PostMapping
    public ResponseEntity<?> submitAppeal(@RequestBody Map<String, String> body,
                                           Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);

            // 仅冻结用户可以申诉
            if (user.getStatus() != UserStatus.FROZEN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiError.of("NOT_FROZEN", "仅冻结账号可以提交申诉"));
            }

            String reason = body.getOrDefault("reason", "");
            String evidence = body.getOrDefault("evidence", "");

            UserAppeal appeal = appealService.submit(user.getId(), reason, evidence);

            auditService.log(user.getId(), "USER", "SUBMIT_APPEAL", "APPEAL",
                    appeal.getId().toString(), null, null, null);

            log.info("Appeal submitted: id={}, userId={}", appeal.getId(), user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toMap(appeal));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.of("SUBMIT_APPEAL_FAILED", e.getMessage()));
        }
    }

    /**
     * 查看我的申诉列表。
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyAppeals(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<UserAppeal> appeals = appealService.findByUserId(user.getId());
        List<Map<String, Object>> result = appeals.stream()
                .map(AppealController::toMap)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * 管理员查看申诉列表。
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listAppeals(@RequestParam(defaultValue = "PENDING") String status) {
        List<UserAppeal> appeals;
        if ("ALL".equalsIgnoreCase(status)) {
            // 需要 repository 支持 findAll 即可，appealService 只提供了 findPending
            appeals = appealService.findPending();
            // 如果需要全部，通过额外的 repository 调用
            // 暂简化处理：status=PENDING 或 status=ALL
            List<UserAppeal> all = appealService.findPending();
            return ResponseEntity.ok(all.stream().map(AppealController::toMap).toList());
        }
        appeals = appealService.findByStatus(status);
        return ResponseEntity.ok(appeals.stream().map(AppealController::toMap).toList());
    }

    /**
     * 受理申诉。
     */
    @PutMapping("/admin/{id}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> acceptAppeal(@PathVariable Long id,
                                           Authentication authentication) {
        return handleAdminAction(id, authentication, AppealStatus.ACCEPTED, "受理申诉");
    }

    /**
     * 维持冻结（需写原因）。
     */
    @PutMapping("/admin/{id}/uphold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> upholdAppeal(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           Authentication authentication) {
        String reason = body.getOrDefault("reason", "");
        if (reason.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of("REASON_REQUIRED", "维持冻结必须填写原因"));
        }
        return handleAdminAction(id, authentication, AppealStatus.UPHELD_FREEZE, reason);
    }

    /**
     * 恢复账号。
     */
    @PutMapping("/admin/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restoreAppeal(@PathVariable Long id,
                                            Authentication authentication) {
        try {
            User operator = getCurrentUser(authentication);
            UserAppeal appeal = appealService.findById(id);

            if (!AppealStatus.PENDING_REVIEW.name().equals(appeal.getStatus())
                    && !AppealStatus.ACCEPTED.name().equals(appeal.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiError.of("INVALID_STATUS", "当前申诉状态不允许恢复账号"));
            }

            // 更新申诉状态为 RESTORED
            appeal.setStatus(AppealStatus.RESTORED.name());
            appeal.setReviewerId(operator.getId());
            appeal.setReviewComment("恢复账号");
            appealService.save(appeal);

            // 恢复用户状态为 ACTIVE
            userService.updateStatus(appeal.getUserId(), UserStatus.ACTIVE,
                    operator.getId(), "申诉通过，恢复账号");

            // 审计日志
            auditService.log(operator.getId(), "ADMIN", "RESTORE_ACCOUNT", "USER",
                    appeal.getUserId().toString(), "FROZEN", "ACTIVE",
                    "申诉ID: " + appeal.getId());

            log.info("Account restored via appeal: appealId={}, userId={}, operatorId={}",
                    appeal.getId(), appeal.getUserId(), operator.getId());

            // 账号恢复后旧会话失效注释：
            // 前端侧应在恢复成功后清除本地 token 并引导用户重新登录，
            // 确保旧冻结状态下的 JWT 不再被使用。
            // 后端侧由 JwtAuthFilter 校验用户当前状态，旧 token 会自动失效。

            return ResponseEntity.ok(toMap(appeal));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of("APPEAL_NOT_FOUND", e.getMessage()));
        }
    }

    /**
     * 管理员处理申诉的统一方法。
     */
    private ResponseEntity<?> handleAdminAction(Long appealId, Authentication authentication,
                                                 AppealStatus decision, String comment) {
        try {
            User operator = getCurrentUser(authentication);
            UserAppeal appeal = appealService.findById(appealId);

            if (!AppealStatus.PENDING_REVIEW.name().equals(appeal.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiError.of("ALREADY_PROCESSED", "申诉已处理"));
            }

            appeal.setStatus(decision.name());
            appeal.setReviewerId(operator.getId());
            appeal.setReviewComment(comment);
            appealService.save(appeal);

            auditService.log(operator.getId(), "ADMIN", "REVIEW_APPEAL", "APPEAL",
                    appeal.getId().toString(), "PENDING_REVIEW", decision.name(), comment);

            log.info("Appeal {}: id={}, operatorId={}", decision.getLabel(), appealId, operator.getId());

            return ResponseEntity.ok(toMap(appeal));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of("APPEAL_NOT_FOUND", e.getMessage()));
        }
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email != null) {
                return userService.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException("用户不存在"));
            }
        }
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("认证用户不存在"));
    }

    private static Map<String, Object> toMap(UserAppeal appeal) {
        return Map.of(
                "id", appeal.getId(),
                "userId", appeal.getUserId(),
                "reason", appeal.getReason() != null ? appeal.getReason() : "",
                "evidence", appeal.getEvidence() != null ? appeal.getEvidence() : "",
                "status", appeal.getStatus(),
                "reviewerId", appeal.getReviewerId() != null ? appeal.getReviewerId() : 0,
                "reviewComment", appeal.getReviewComment() != null ? appeal.getReviewComment() : "",
                "createdAt", appeal.getCreatedAt().toString(),
                "updatedAt", appeal.getUpdatedAt().toString()
        );
    }
}
