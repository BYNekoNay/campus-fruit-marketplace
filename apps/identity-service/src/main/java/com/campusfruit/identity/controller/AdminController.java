package com.campusfruit.identity.controller;

import com.campusfruit.identity.dto.UserInfoResponse;
import com.campusfruit.identity.entity.User;
import com.campusfruit.identity.enums.UserStatus;
import com.campusfruit.identity.repository.UserRepository;
import com.campusfruit.identity.service.AuditService;
import com.campusfruit.identity.service.UserService;
import com.campusfruit.observability.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminController(UserService userService, UserRepository userRepository,
                           AuditService auditService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               Authentication authentication) {
        try {
            String statusStr = body.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiError.of("INVALID_REQUEST", "status 不能为空"));
            }

            UserStatus newStatus;
            try {
                newStatus = UserStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiError.of("INVALID_STATUS", "无效的状态: " + statusStr));
            }

            User operator = getCurrentUser(authentication);
            String oldStatus = userService.findById(id)
                    .map(u -> u.getStatus().name())
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            String reason = body.getOrDefault("reason", "管理员操作");
            userService.updateStatus(id, newStatus, operator.getId(), reason);

            // 审计日志
            auditService.log(operator.getId(), "ADMIN", "UPDATE_USER_STATUS", "USER",
                    id.toString(), oldStatus, newStatus.name(), reason);

            log.info("Admin {} updated user {} status: {} -> {}",
                    operator.getId(), id, oldStatus, newStatus);

            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of("USER_NOT_FOUND", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        Page<UserInfoResponse> dtoPage = userPage.map(userService::toUserInfoResponse);
        return ResponseEntity.ok(dtoPage);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email != null) {
                return userService.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException("管理员用户不存在"));
            }
        }

        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("管理员用户不存在"));
    }
}
