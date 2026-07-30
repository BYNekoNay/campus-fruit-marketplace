package com.campusfruit.identity.service;

import com.campusfruit.identity.entity.UserConsent;
import com.campusfruit.identity.repository.UserConsentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 用户授权管理服务。
 * <p>
 * 定位 consent 独立于账号 consent。
 * 撤销 consent 不清除已授权期间的合法审计日志。
 */
@Service
public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    public static final String CONSENT_TYPE_LOCATION = "LOCATION";
    public static final String STATUS_GRANTED = "GRANTED";
    public static final String STATUS_REVOKED = "REVOKED";
    public static final String STATUS_NOT_SET = "NOT_SET";

    private final UserConsentRepository consentRepository;
    private final AuditService auditService;

    public ConsentService(UserConsentRepository consentRepository, AuditService auditService) {
        this.consentRepository = consentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public String grant(Long userId, String consentType) {
        UserConsent consent = consentRepository.findByUserIdAndConsentType(userId, consentType)
                .orElseGet(() -> {
                    UserConsent newConsent = new UserConsent();
                    newConsent.setUserId(userId);
                    newConsent.setConsentType(consentType);
                    newConsent.setStatus(STATUS_NOT_SET);
                    return newConsent;
                });

        // 如果已软删除，恢复
        if (Boolean.TRUE.equals(consent.getDeleted())) {
            consent.setDeleted(false);
        }

        String oldStatus = consent.getStatus();
        consent.setStatus(STATUS_GRANTED);
        consent.setGrantedAt(Instant.now());
        consent.setRevokedAt(null);

        UserConsent saved = consentRepository.save(consent);

        auditService.log(userId, "USER", "CONSENT_GRANT", "CONSENT",
                saved.getId().toString(),
                "type=" + consentType + ", old=" + oldStatus,
                "type=" + consentType + ", new=" + STATUS_GRANTED,
                null);

        log.info("Consent granted: userId={}, type={}", userId, consentType);
        return STATUS_GRANTED;
    }

    @Transactional
    public String revoke(Long userId, String consentType) {
        UserConsent consent = consentRepository.findByUserIdAndConsentType(userId, consentType)
                .orElseGet(() -> {
                    UserConsent newConsent = new UserConsent();
                    newConsent.setUserId(userId);
                    newConsent.setConsentType(consentType);
                    newConsent.setStatus(STATUS_NOT_SET);
                    return newConsent;
                });

        String oldStatus = consent.getStatus();
        consent.setStatus(STATUS_REVOKED);
        consent.setRevokedAt(Instant.now());

        UserConsent saved = consentRepository.save(consent);

        auditService.log(userId, "USER", "CONSENT_REVOKE", "CONSENT",
                saved.getId().toString(),
                "type=" + consentType + ", old=" + oldStatus,
                "type=" + consentType + ", new=" + STATUS_REVOKED,
                null);

        log.info("Consent revoked: userId={}, type={}", userId, consentType);
        return STATUS_REVOKED;
    }

    public String getStatus(Long userId, String consentType) {
        return consentRepository.findByUserIdAndConsentType(userId, consentType)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .map(UserConsent::getStatus)
                .orElse(STATUS_NOT_SET);
    }

    public List<UserConsent> getGrantedConsents(Long userId) {
        List<UserConsent> consents = consentRepository.findByUserIdAndDeletedFalse(userId);
        if (consents == null || consents.isEmpty()) {
            return Collections.emptyList();
        }
        return consents;
    }

    /**
     * 软删除用户所有 location 数据（用户请求删除定位数据入口）。
     * 不清除审计日志。
     */
    @Transactional
    public void softDeleteLocationData(Long userId) {
        consentRepository.findByUserIdAndConsentType(userId, CONSENT_TYPE_LOCATION)
                .ifPresent(consent -> {
                    consent.setDeleted(true);
                    consent.setStatus(STATUS_REVOKED);
                    consent.setRevokedAt(Instant.now());
                    consentRepository.save(consent);

                    auditService.log(userId, "USER", "DATA_DELETE_LOCATION", "CONSENT",
                            consent.getId().toString(), null, null, "User requested location data deletion");
                });

        log.info("Location data soft-deleted for userId={}", userId);
    }
}
