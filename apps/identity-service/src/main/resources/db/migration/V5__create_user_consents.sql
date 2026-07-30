CREATE TABLE user_consents (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    consent_type    VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'NOT_SET',
    granted_at      TIMESTAMP    NULL,
    revoked_at      TIMESTAMP    NULL,
    last_updated    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_user_consent_type UNIQUE (user_id, consent_type),
    CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_consents_user ON user_consents (user_id);
CREATE INDEX idx_user_consents_type_status ON user_consents (consent_type, status);
