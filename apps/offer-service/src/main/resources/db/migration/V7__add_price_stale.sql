ALTER TABLE offers
    ADD COLUMN price_stale BOOLEAN NOT NULL DEFAULT FALSE COMMENT '价格是否已过时(24h未确认)' AFTER last_confirmed_at;

CREATE INDEX idx_offers_price_stale ON offers (price_stale);
CREATE INDEX idx_offers_last_confirmed ON offers (last_confirmed_at);
