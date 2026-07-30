package com.campusfruit.identity.service;

import com.campusfruit.identity.dto.RegisterRequest;
import com.campusfruit.identity.dto.UserInfoResponse;
import com.campusfruit.identity.entity.User;
import com.campusfruit.identity.enums.UserStatus;
import com.campusfruit.identity.repository.UserRepository;
import com.campusfruit.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        // 使用 BCryptPasswordEncoder（后续可切换为 Argon2id）
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname().trim() : extractNicknameFromEmail(request.getEmail()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(SecurityConstants.ROLE_USER);

        User saved = userRepository.save(user);
        log.info("User registered: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void updateStatus(Long userId, UserStatus newStatus, Long operatorId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new IllegalArgumentException("已注销用户无法修改状态");
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        userRepository.save(user);
        log.info("User status updated: id={}, {} -> {}, operator={}", userId, oldStatus, newStatus, operatorId);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码不正确");
        }

        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("User password changed: id={}", userId);
    }

    @Transactional
    public User updateProfile(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname.trim());
        }
        User saved = userRepository.save(user);
        log.info("User profile updated: id={}", userId);
        return saved;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        log.info("User soft-deleted: id={}", userId);
    }

    public UserInfoResponse toUserInfoResponse(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setStatus(user.getStatus().name());
        response.setRoles(parseRoles(user.getRoles()));
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public List<String> parseRoles(String rolesStr) {
        if (rolesStr == null || rolesStr.isBlank()) {
            return List.of();
        }
        return Arrays.asList(rolesStr.split(","));
    }

    private String extractNicknameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "用户";
        }
        return email.substring(0, email.indexOf('@'));
    }
}
