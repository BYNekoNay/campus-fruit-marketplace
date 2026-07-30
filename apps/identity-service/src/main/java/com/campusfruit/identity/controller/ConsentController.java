package com.campusfruit.identity.controller;

import com.campusfruit.identity.entity.User;
import com.campusfruit.identity.entity.UserConsent;
import com.campusfruit.identity.service.ConsentService;
import com.campusfruit.identity.service.UserService;
import com.campusfruit.observability.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/me")
public class ConsentController {

    private static final Logger log = LoggerFactory.getLogger(ConsentController.class);

    private final ConsentService consentService;
    private final UserService userService;

    public ConsentController(ConsentService consentService, UserService userService) {
        this.consentService = consentService;
        this.userService = userService;
    }

    /**
     * 查询指定类型的授权状态。
     * GET /api/me/consent/{type}
     */
    @GetMapping("/consent/{type}")
    public ResponseEntity<?> getConsentStatus(@PathVariable String type,
                                               Authentication authentication) {
        User user = getCurrentUser(authentication);
        String status = consentService.getStatus(user.getId(), type);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consentType", type);
        result.put("status", status);
        return ResponseEntity.ok(result);
    }

    /**
     * 授权指定类型。
     * PUT /api/me/consent/{type}/grant
     */
    @PutMapping("/consent/{type}/grant")
    public ResponseEntity<?> grantConsent(@PathVariable String type,
                                           Authentication authentication) {
        User user = getCurrentUser(authentication);

        if (!ConsentService.CONSENT_TYPE_LOCATION.equals(type)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.of("INVALID_CONSENT_TYPE", "不支持的授权类型: " + type));
        }

        String status = consentService.grant(user.getId(), type);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consentType", type);
        result.put("status", status);
        return ResponseEntity.ok(result);
    }

    /**
     * 撤销指定类型授权。
     * PUT /api/me/consent/{type}/revoke
     */
    @PutMapping("/consent/{type}/revoke")
    public ResponseEntity<?> revokeConsent(@PathVariable String type,
                                            Authentication authentication) {
        User user = getCurrentUser(authentication);

        if (!ConsentService.CONSENT_TYPE_LOCATION.equals(type)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiError.of("INVALID_CONSENT_TYPE", "不支持的授权类型: " + type));
        }

        String status = consentService.revoke(user.getId(), type);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consentType", type);
        result.put("status", status);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有已授权列表。
     * GET /api/me/consents
     */
    @GetMapping("/consents")
    public ResponseEntity<?> getAllConsents(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<UserConsent> consents = consentService.getGrantedConsents(user.getId());

        List<Map<String, Object>> results = consents.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("consentType", c.getConsentType());
            m.put("status", c.getStatus());
            m.put("grantedAt", c.getGrantedAt());
            m.put("revokedAt", c.getRevokedAt());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    /**
     * 删除所有存储的位置数据（软删除）。
     * DELETE /api/me/data/location
     */
    @DeleteMapping("/data/location")
    public ResponseEntity<?> deleteLocationData(Authentication authentication) {
        User user = getCurrentUser(authentication);

        consentService.softDeleteLocationData(user.getId());

        log.info("User {} requested location data deletion", user.getId());
        return ResponseEntity.noContent().build();
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
