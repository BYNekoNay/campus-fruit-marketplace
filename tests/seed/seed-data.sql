-- ============================================================
-- 校园水果商城 - 集成测试种子数据
-- 使用方式: docker compose up mysql 后逐库执行
-- 
-- 数据约定:
--   用户 U1: 普通用户 alice@test.com (ROLE_USER)
--   用户 U2: 普通用户 bob@test.com (ROLE_USER)
--   用户 U3: 商家主 carol@fruit.com (ROLE_USER,ROLE_MERCHANT)
--   用户 U4: 管理员 admin@fruit.com (ROLE_ADMIN)
--   商家 M1: 鲜果多 (carol 经营)
--   门店 S1: 鲜果多-东门店 (ACTIVE)
--   门店 S2: 鲜果多-西门店 (ACTIVE)
--   标准水果 F1-F5: 赣南脐橙/红富士苹果/巨峰葡萄/海南香蕉/徐闻菠萝
--   订单 O1: alice 在 S1 已完成
--   订单 O2: bob 在 S1 待确认
--   评价 R1: alice 对 S1 的评价
-- ============================================================

-- =================== identity_service ===================
-- 密码"password123"的 BCrypt 哈希值
INSERT INTO users (id, email, password_hash, nickname, status, roles) VALUES
(1, 'alice@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice', 'ACTIVE', 'ROLE_USER'),
(2, 'bob@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bob', 'ACTIVE', 'ROLE_USER'),
(3, 'carol@fruit.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Caroli鲜果多', 'ACTIVE', 'ROLE_USER,ROLE_MERCHANT'),
(4, 'admin@fruit.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '平台管理员', 'ACTIVE', 'ROLE_ADMIN');

-- =================== merchant_service ===================
INSERT INTO merchants (id, owner_user_id, name, contact_name, contact_phone, license_number, status, created_at) VALUES
(1, 3, '鲜果多商贸有限公司', '李老板', '13800138001', 'SH20260001', 'APPROVED', '2026-07-01 08:00:00');

INSERT INTO stores (id, merchant_id, name, address, latitude, longitude, coord_type, phone, business_hours, status, pickup_lead_minutes, created_at) VALUES
(1, 1, '鲜果多-东门旗舰店', 'XX大学东门向东100米', 39.9042, 116.4074, 'BD09LL', '010-12345601', '{"open":"08:00","close":"22:00","days":[1,2,3,4,5,6,7]}', 'ACTIVE', 15, '2026-07-01 08:00:00'),
(2, 1, '鲜果多-西门店', 'XX大学西门商业街B-12', 39.9030, 116.3990, 'BD09LL', '010-12345602', '{"open":"09:00","close":"21:00","days":[1,2,3,4,5,6,7]}', 'ACTIVE', 20, '2026-07-01 08:00:00');

INSERT INTO store_staff (store_id, user_id, role) VALUES
(1, 3, 'OWNER');

-- =================== offer_service ===================
INSERT INTO canonical_fruits (id, category, variety, grade, origin, default_unit, comparison_group_id, version, status, created_at) VALUES
(1, '柑橘类', '赣南脐橙', '一级', '江西赣州', 'g', 1001, 1, 'ACTIVE', '2026-07-01 08:00:00'),
(2, '仁果类', '红富士苹果', '一级', '山东烟台', 'g', 1002, 1, 'ACTIVE', '2026-07-01 08:00:00'),
(3, '浆果类', '巨峰葡萄', '一级', '辽宁营口', 'g', 1003, 1, 'ACTIVE', '2026-07-01 08:00:00'),
(4, '热带水果', '海南香蕉', '一级', '海南海口', 'g', 1004, 1, 'ACTIVE', '2026-07-01 08:00:00'),
(5, '热带水果', '徐闻菠萝', '一级', '广东徐闻', 'g', 1005, 1, 'ACTIVE', '2026-07-01 08:00:00');

-- 门店报价 (价格单位: 分)
INSERT INTO offers (id, store_id, canonical_fruit_id, sales_unit, net_weight_grams, unit_price, stock_quantity, available_quantity, reserved_quantity, status, last_confirmed_at, created_at) VALUES
-- S1 东门店报价
(1, 1, 1, '500g盒装', 500, 1290, 100, 100, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00'),
(2, 1, 1, '1kg散装', 1000, 2380, 50, 50, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00'),
(3, 1, 2, '500g盒装', 500, 1590, 80, 80, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00'),
(4, 1, 3, '500g盒装', 500, 1890, 60, 60, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00'),
(5, 1, 4, '1kg把', 1000, 790, 120, 120, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00'),
-- S2 西门店报价
(6, 2, 1, '500g盒装', 500, 1390, 80, 80, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00'),
(7, 2, 2, '500g盒装', 500, 1490, 70, 70, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00'),
(8, 2, 5, '个(约1.5kg)', 1500, 2990, 30, 30, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00'),
-- S1 不可比报价 (按个销售,无净重)
(9, 1, 5, '个', NULL, 3500, 40, 40, 0, 'ACTIVE', '2026-07-29 08:00:00', '2026-07-01 08:00:00');

-- 价格历史 (模拟价格变动)
INSERT INTO price_histories (offer_id, unit_price, net_weight_grams, sales_unit, changed_at) VALUES
(1, 1390, 500, '500g盒装', '2026-07-15 08:00:00'),
(1, 1290, 500, '500g盒装', '2026-07-22 08:00:00');

-- 库存初始流水
INSERT INTO stock_ledger (offer_id, change_type, quantity_change, available_before, available_after, reserved_before, reserved_after, reference_id, created_at) VALUES
(1, 'INITIAL', 100, 0, 100, 0, 0, 'seed-init', '2026-07-01 08:00:00'),
(2, 'INITIAL', 50, 0, 50, 0, 0, 'seed-init', '2026-07-01 08:00:00'),
(3, 'INITIAL', 80, 0, 80, 0, 0, 'seed-init', '2026-07-01 08:00:00'),
(4, 'INITIAL', 60, 0, 60, 0, 0, 'seed-init', '2026-07-01 08:00:00'),
(5, 'INITIAL', 120, 0, 120, 0, 0, 'seed-init', '2026-07-01 08:00:00'),
(6, 'INITIAL', 80, 0, 80, 0, 0, 'seed-init', '2026-07-01 08:00:00'),
(7, 'INITIAL', 70, 0, 70, 0, 0, 'seed-init', '2026-07-01 08:00:00'),
(8, 'INITIAL', 30, 0, 30, 0, 0, 'seed-init', '2026-07-01 08:00:00'),
(9, 'INITIAL', 40, 0, 40, 0, 0, 'seed-init', '2026-07-01 08:00:00');

-- =================== order_service ===================
-- 已完成订单 O1: alice 在 S1 购买
INSERT INTO orders (id, order_no, user_id, store_id, status, total_amount, item_count, idempotency_key, payment_status, created_at) VALUES
(1, '20260729000001', 1, 1, 'COMPLETED', 2180, 1, 'it-seed-order-1', 'PAID_AT_PICKUP', '2026-07-29 10:00:00');
INSERT INTO order_items (id, order_id, offer_id, fruit_variety, sales_unit, unit_price, quantity) VALUES
(1, 1, 3, '红富士苹果', '500g盒装', 1590, 1);

-- 待确认订单 O2: bob 在 S1 购买
INSERT INTO orders (id, order_no, user_id, store_id, status, total_amount, item_count, idempotency_key, payment_status, created_at) VALUES
(2, '20260729000002', 2, 1, 'PENDING_STORE_CONFIRMATION', 1290, 1, 'it-seed-order-2', 'UNPAID', '2026-07-29 14:00:00');
INSERT INTO order_items (id, order_id, offer_id, fruit_variety, sales_unit, unit_price, quantity) VALUES
(2, 2, 1, '赣南脐橙', '500g盒装', 1290, 1);

-- 订单状态事件
INSERT INTO order_status_events (order_id, from_status, to_status, operator_type, created_at) VALUES
(1, NULL, 'PENDING_RESERVATION', 'SYSTEM', '2026-07-29 10:00:00'),
(1, 'PENDING_RESERVATION', 'PENDING_STORE_CONFIRMATION', 'SYSTEM', '2026-07-29 10:00:01'),
(1, 'PENDING_STORE_CONFIRMATION', 'ACCEPTED', 'STORE_STAFF', '2026-07-29 10:05:00'),
(1, 'ACCEPTED', 'READY_FOR_PICKUP', 'STORE_STAFF', '2026-07-29 10:20:00'),
(1, 'READY_FOR_PICKUP', 'COMPLETED', 'STORE_STAFF', '2026-07-29 11:00:00');

-- =================== review_service ===================
-- alice 的已完成订单评价资格
INSERT INTO review_eligibilities (user_id, store_id, order_id, order_completed_at, used, tombstone) VALUES
(1, 1, 1, '2026-07-29 11:00:00', TRUE, FALSE);

-- alice 对 S1 的评价
INSERT INTO reviews (id, user_id, store_id, order_id, rating, content, tags, status, current_version, visible, created_at) VALUES
(1, 1, 1, 1, 5, '水果非常新鲜，包装也很用心，到店自取很方便！', '新鲜,包装好,服务好', 'ACTIVE', 1, TRUE, '2026-07-29 12:00:00');

INSERT INTO review_versions (review_id, version, rating, content, tags, changed_at) VALUES
(1, 1, 5, '水果非常新鲜，包装也很用心，到店自取很方便！', '新鲜,包装好,服务好', '2026-07-29 12:00:00');

-- 商家回复
INSERT INTO merchant_replies (review_id, merchant_id, store_id, content, status, created_at) VALUES
(1, 1, 1, '感谢您的支持！我们会继续保持品质~', 'ACTIVE', '2026-07-29 13:00:00');

-- 评分聚合
INSERT INTO rating_aggregates (store_id, avg_rating, bayesian_rating, total_ratings, rating_distribution, version, calculated_at) VALUES
(1, 5.00, 3.75, 1, '{"1":0,"2":0,"3":0,"4":0,"5":1}', 1, '2026-07-29 12:00:01');

-- =================== discovery_service ===================
-- 投影数据由事件消费者自动填充,此处预填测试数据
INSERT INTO store_offer_projections (store_id, offer_id, store_name, store_address, store_lat, store_lng, store_status, merchant_id, merchant_name, canonical_fruit_id, fruit_category, fruit_variety, fruit_grade, fruit_origin, sales_unit, net_weight_grams, unit_price, standard_price_per500g, is_comparable, available_quantity, offer_status, avg_rating, review_count, aggregate_version) VALUES
(1, 1, '鲜果多-东门旗舰店', 'XX大学东门向东100米', 39.9042, 116.4074, 'ACTIVE', 1, '鲜果多', 1, '柑橘类', '赣南脐橙', '一级', '江西赣州', '500g盒装', 500, 1290, 12.90, TRUE, 100, 'ACTIVE', 3.75, 1, 1),
(1, 2, '鲜果多-东门旗舰店', 'XX大学东门向东100米', 39.9042, 116.4074, 'ACTIVE', 1, '鲜果多', 1, '柑橘类', '赣南脐橙', '一级', '江西赣州', '1kg散装', 1000, 2380, 11.90, TRUE, 50, 'ACTIVE', 3.75, 1, 1),
(1, 3, '鲜果多-东门旗舰店', 'XX大学东门向东100米', 39.9042, 116.4074, 'ACTIVE', 1, '鲜果多', 2, '仁果类', '红富士苹果', '一级', '山东烟台', '500g盒装', 500, 1590, 15.90, TRUE, 80, 'ACTIVE', 3.75, 1, 1),
(1, 4, '鲜果多-东门旗舰店', 'XX大学东门向东100米', 39.9042, 116.4074, 'ACTIVE', 1, '鲜果多', 3, '浆果类', '巨峰葡萄', '一级', '辽宁营口', '500g盒装', 500, 1890, 18.90, TRUE, 60, 'ACTIVE', 3.75, 1, 1),
(1, 5, '鲜果多-东门旗舰店', 'XX大学东门向东100米', 39.9042, 116.4074, 'ACTIVE', 1, '鲜果多', 4, '热带水果', '海南香蕉', '一级', '海南海口', '1kg把', 1000, 790, 3.95, TRUE, 120, 'ACTIVE', 3.75, 1, 1),
(2, 6, '鲜果多-西门店', 'XX大学西门商业街B-12', 39.9030, 116.3990, 'ACTIVE', 1, '鲜果多', 1, '柑橘类', '赣南脐橙', '一级', '江西赣州', '500g盒装', 500, 1390, 13.90, TRUE, 80, 'ACTIVE', 0, 0, 1),
(2, 7, '鲜果多-西门店', 'XX大学西门商业街B-12', 39.9030, 116.3990, 'ACTIVE', 1, '鲜果多', 2, '仁果类', '红富士苹果', '一级', '山东烟台', '500g盒装', 500, 1490, 14.90, TRUE, 70, 'ACTIVE', 0, 0, 1),
(2, 8, '鲜果多-西门店', 'XX大学西门商业街B-12', 39.9030, 116.3990, 'ACTIVE', 1, '鲜果多', 5, '热带水果', '徐闻菠萝', '一级', '广东徐闻', '个(约1.5kg)', 1500, 2990, 9.97, TRUE, 30, 'ACTIVE', 0, 0, 1),
(1, 9, '鲜果多-东门旗舰店', 'XX大学东门向东100米', 39.9042, 116.4074, 'ACTIVE', 1, '鲜果多', 5, '热带水果', '徐闻菠萝', '一��', '广东徐闻', '个', NULL, 3500, NULL, FALSE, 40, 'ACTIVE', 3.75, 1, 1);

-- 收藏
INSERT INTO favorites (user_id, store_id, created_at) VALUES
(1, 1, '2026-07-29 11:30:00');
