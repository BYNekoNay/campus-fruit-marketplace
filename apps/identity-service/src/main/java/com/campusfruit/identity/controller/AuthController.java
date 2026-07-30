package com.campusfruit.identity.controller;

import com.campusfruit.identity.dto.LoginRequest;
import com.campusfruit.identity.dto.LoginResponse;
import com.campusfruit.identity.dto.RegisterRequest;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuditService auditService;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    public AuthController(UserService userService, AuditService auditService,
                          AuthenticationManager authenticationManager,
                          JwtEncoder jwtEncoder) {
        this.userService = userService;
        this.auditService = auditService;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            UserInfoResponse response = userService.toUserInfoResponse(user);
            log.info("New user registered: email={}", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of("REGISTER_CONFLICT", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 尝试认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String email = authentication.getName();
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("认证成功但用户不存在"));

            // 生成 JWT token
            List<String> roles = userService.parseRoles(user.getRoles());
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(3600); // 1 小时

            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("http://localhost:8080")
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .subject(user.getId().toString())
                    .claim("email", user.getEmail())
                    .claim("roles", roles)
                    .build();

            String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                    user.getId(), user.getEmail(), user.getNickname(), roles);

            LoginResponse response = new LoginResponse(
                    accessToken, "refresh-" + user.getId(), 3600L, userInfo);

            auditService.log(user.getId(), "USER", "LOGIN", "USER",
                    user.getId().toString(), null, null, null);

            log.info("User logged in: email={}", user.getEmail());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.of("AUTH_FAILED", "邮箱或密码不正确"));
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.of("ACCOUNT_FROZEN", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.of("UNAUTHORIZED", "未登录"));
        }

        // 从 JWT 或用户名中提取用户信息
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String email = jwt.getClaimAsString("email");
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiError.of("UNAUTHORIZED", "无效的令牌"));
            }
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("用户不存在"));
            return ResponseEntity.ok(userService.toUserInfoResponse(user));
        }

        // 回退：从认证名查找
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("认证用户不存在"));
        return ResponseEntity.ok(userService.toUserInfoResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // 当前为 Stub 实现，实际可从 Redis 中移除 refresh_token 或加入黑名单
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }
}
