CREATE TABLE audit_logs (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    actor_id    BIGINT,
    actor_type  VARCHAR(50),
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(100),
    target_id   VARCHAR(255),
    old_value   TEXT,
    new_value   TEXT,
    reason      TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_id, actor_type);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at);
