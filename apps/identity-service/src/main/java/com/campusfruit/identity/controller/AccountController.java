package com.campusfruit.identity.controller;

import com.campusfruit.identity.dto.ChangePasswordRequest;
import com.campusfruit.identity.dto.UserInfoResponse;
import com.campusfruit.identity.entity.User;
import com.campusfruit.identity.service.AuditService;
import com.campusfruit.identity.service.UserService;
import com.campusfruit.observability.ApiError;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final UserService userService;
    private final AuditService auditService;

    public AccountController(UserService userService, AuditService auditService) {
        this.userService = userService;
        this.auditService = auditService;
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                             Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);

            userService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());

            // 审计日志：密码修改，不记录密码值
            auditService.log(user.getId(), "USER", "CHANGE_PASSWORD", "USER",
                    user.getId().toString(), null, null, null);

            log.info("Password changed for user: id={}", user.getId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.of("PASSWORD_CHANGE_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        User user = getCurrentUser(authentication);
        UserInfoResponse response = userService.toUserInfoResponse(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body,
                                            Authentication authentication) {
        User user = getCurrentUser(authentication);

        String oldNickname = user.getNickname();
        String newNickname = body.get("nickname");

        User updated = userService.updateProfile(user.getId(), newNickname);

        auditService.log(user.getId(), "USER", "UPDATE_PROFILE", "USER",
                user.getId().toString(), "nickname=" + oldNickname, "nickname=" + newNickname, null);

        return ResponseEntity.ok(userService.toUserInfoResponse(updated));
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
}
