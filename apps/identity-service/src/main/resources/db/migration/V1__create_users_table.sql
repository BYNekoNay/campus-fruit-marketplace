CREATE TABLE users (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    nickname    VARCHAR(100),
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    roles       VARCHAR(500)    NOT NULL DEFAULT 'ROLE_USER',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_status ON users (status);
