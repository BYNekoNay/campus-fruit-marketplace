package com.campusfruit.identity.service;

import com.campusfruit.identity.entity.UserAppeal;
import com.campusfruit.identity.enums.UserStatus;
import com.campusfruit.identity.repository.UserAppealRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppealService {

    private static final Logger log = LoggerFactory.getLogger(AppealService.class);

    private final UserAppealRepository appealRepository;
    private final UserService userService;

    public AppealService(UserAppealRepository appealRepository, UserService userService) {
        this.appealRepository = appealRepository;
        this.userService = userService;
    }

    @Transactional
    public UserAppeal submit(Long userId, String reason, String evidence) {
        userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        UserAppeal appeal = new UserAppeal();
        appeal.setUserId(userId);
        appeal.setReason(reason);
        appeal.setEvidence(evidence);
        appeal.setStatus("PENDING");

        UserAppeal saved = appealRepository.save(appeal);
        log.info("Appeal submitted: id={}, userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional
    public UserAppeal review(Long appealId, Long reviewerId, String decision, String comment) {
        UserAppeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在: " + appealId));

        if (!"PENDING".equals(appeal.getStatus())) {
            throw new IllegalArgumentException("申诉已处理");
        }

        appeal.setStatus(decision);
        appeal.setReviewerId(reviewerId);
        appeal.setReviewComment(comment);

        // 如果申诉通过，恢复用户状态
        if ("APPROVED".equals(decision)) {
            userService.updateStatus(appeal.getUserId(), UserStatus.ACTIVE, reviewerId, "申诉通过恢复");
        }

        UserAppeal saved = appealRepository.save(appeal);
        log.info("Appeal reviewed: id={}, decision={}, reviewerId={}", appealId, decision, reviewerId);
        return saved;
    }

    public UserAppeal findById(Long id) {
        return appealRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在: " + id));
    }

    public List<UserAppeal> findByUserId(Long userId) {
        return appealRepository.findByUserId(userId);
    }

    public List<UserAppeal> findPending() {
        return appealRepository.findByStatus("PENDING");
    }

    public List<UserAppeal> findByStatus(String status) {
        return appealRepository.findByStatus(status);
    }

    @Transactional
    public UserAppeal save(UserAppeal appeal) {
        return appealRepository.save(appeal);
    }
}
