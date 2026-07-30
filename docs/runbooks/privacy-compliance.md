# 隐私合规说明

## 定位数据处理

### 用户精确坐标保护
- 用户精确坐标（lat/lng 原始值）**绝不落库存储**
- 前端通过 `navigator.geolocation.getCurrentPosition()` 获取坐标后，**仅用于当次搜索请求**
- 搜索请求完成后，前端不缓存原始坐标（仅保留模糊区域文本如"附近区域"）
- 搜索结果仅返回**门店坐标**（公开信息），**不返回用户坐标**

### 搜索日志规范
- 搜索日志中**不记录**用户的 `lat`/`lng` 原始值
- 若需记录搜索区域统计，仅使用模糊的区/街道级数据（如 "XX区"），不使用精确经纬度

### Consent 端点体系
系统通过以下端点管理用户定位授权，独立于账号 consent：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/me/consent/LOCATION` | GET | 查询当前授权状态 |
| `/api/me/consent/LOCATION/grant` | PUT | 授权定位 |
| `/api/me/consent/LOCATION/revoke` | PUT | 撤销授权 |
| `/api/me/consents` | GET | 获取所有授权列表 |
| `/api/me/data/location` | DELETE | 软删除所有存储的位置数据 |

**状态说明：**
- `GRANTED`：用户已授权
- `REVOKED`：用户已撤销
- `NOT_SET`：用户未做任何操作

**授权变更审计：**
- 每次 GRANT/REVOKE 操作均记录审计日志
- 撤销 consent **不清除**已授权期间的合法审计日志
- 审计日志不记录坐标数据，仅记录操作类型和时间

**数据删除机制：**
- `DELETE /api/me/data/location` 执行**软删除**（`deleted=true`），不物理删除记录
- 已软删除的记录通过 `deleted=true` 标记过滤
- 撤销操作同样将 status 设为 `REVOKED`

## 第三方告知

### 百度地图 API 使用规范
- 使用百度地图 API 展示门店位置前，需取得用户 LOCATION consent
- Consent 端点同时提供 **GRANT** 和 **REVOKE** 双操作
- 用户可**随时撤销授权**并删除历史定位数据
- 仅在 consent 为 GRANTED 状态时，前端才调用 `navigator.geolocation`
- 前端首次加载时通过 `GET /api/me/consent/LOCATION` 查询状态才决定是否定位

### 用户控制权
- 用户在发现页（DiscoveryPage）和首页（HomePage）均可看到定位状态
- 已定位状态显示为"当前位置：XX区"，不显示精确坐标
- 未授权状态显示"点击授权位置，查看附近门店"，引导用户授权
- 用户可通过"切换"按钮撤销已有授权

## 数据最小化

### 商品信息
- 按个/按盒等非称重商品**不需要**重量信息，仍可正常购买
- 仅称重商品（如散装水果）才需要重量字段
- 价格计算基于数量，不依赖重量

### 定位数据
- 仅收集搜索所需的最小定位精度（`enableHighAccuracy: false`）
- 浏览器定位使用 5 分钟缓存（`maximumAge: 300000`）
- 定位超时时间限制在 5-8 秒内

### 前端安全
- 前端**不发送**精确定位到任何日志/tracking/analytics 端点
- 坐标仅通过 HTTPS 请求体发送至搜索 API
- 搜索结果缓存不包含用户坐标
- localStorage/sessionStorage 中不存储 lat/lng 原始值
