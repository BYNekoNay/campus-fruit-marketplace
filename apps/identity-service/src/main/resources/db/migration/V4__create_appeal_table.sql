CREATE TABLE user_appeals (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    reason          TEXT,
    evidence        TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewer_id     BIGINT,
    review_comment  TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_appeal_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_appeals_user ON user_appeals (user_id);
CREATE INDEX idx_user_appeals_status ON user_appeals (status);
