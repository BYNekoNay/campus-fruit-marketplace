# 校园水果商城 - 本地开发启动指南

## 前置条件

| 工具 | 版本要求 | 检查命令 |
|------|----------|----------|
| Java | 21 LTS | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Node.js | 22 LTS | `node -v` |
| Docker | 27+ | `docker --version` |
| Docker Compose | 2.x | `docker compose version` |

## 快速启动（5 步）

### Step 1: 启动基础设施

```bash
# 在项目根目录 campus-fruit-marketplace/
docker compose -f deploy/compose/compose.core.yaml up -d
```

等待 MySQL、Redis、RabbitMQ、Nacos 全部 healthy：
```bash
docker compose -f deploy/compose/compose.core.yaml ps
```

### Step 2: 初始化数据库

```bash
# 创建各服务数据库
docker exec -i campus-fruit-mysql mysql -uroot -proot123 <<EOF
CREATE DATABASE IF NOT EXISTS identity_service DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS merchant_service DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS offer_service DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS order_service DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS review_service DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS discovery_service DEFAULT CHARACTER SET utf8mb4;
EOF
```

### Step 3: 编译项目

```bash
# 后端编译
./mvnw compile -T 1C

# 前端编译
cd web && npm ci && npm run build && cd ..
```

### Step 4: 启动微服务

```bash
# 方式1: 逐个启动（推荐开发调试）
./mvnw -pl apps/identity-service spring-boot:run &
./mvnw -pl apps/merchant-service spring-boot:run &
./mvnw -pl apps/offer-service spring-boot:run &
./mvnw -pl apps/order-service spring-boot:run &
./mvnw -pl apps/review-service spring-boot:run &
./mvnw -pl apps/discovery-service spring-boot:run &
./mvnw -pl apps/gateway-service spring-boot:run

# 方式2: Docker Compose 全量启动
docker compose -f deploy/compose/compose.yaml --profile marketplace up -d
```

### Step 5: 注入测试数据

```bash
# 连接到 MySQL 执行种子数据
docker exec -i campus-fruit-mysql mysql -uroot -proot123 < tests/seed/seed-data.sql
```

## 服务端口

| 服务 | 端口 | 健康检查 |
|------|------|----------|
| Gateway | 8080 | http://localhost:8080/actuator/health |
| Identity | 8081 | http://localhost:8081/actuator/health |
| Merchant | 8082 | http://localhost:8082/actuator/health |
| Offer | 8083 | http://localhost:8083/actuator/health |
| Order | 8084 | http://localhost:8084/actuator/health |
| Review | 8085 | http://localhost:8085/actuator/health |
| Discovery | 8086 | http://localhost:8086/actuator/health |
| Frontend | 5173 | http://localhost:5173 |

## 冒烟测试

### 1. 健康检查
```bash
for port in 8080 8081 8082 8083 8084 8085 8086; do
  echo "Port $port: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/actuator/health)"
done
```

### 2. 用户注册
```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123456","nickname":"测试用户"}' | jq .
```

### 3. 用户登录
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.com","password":"password123"}' | jq -r '.accessToken')
echo "Token: ${TOKEN:0:20}..."
```

### 4. 搜索水果
```bash
curl -s -X POST http://localhost:8080/api/discovery/search \
  -H "Content-Type: application/json" \
  -d '{"keyword":"脐橙","sortBy":"COMPREHENSIVE","page":1,"size":10}' | jq '.totalCount'
```

### 5. 查看门店报价
```bash
curl -s http://localhost:8080/api/discovery/stores/1/offers | jq '. | length'
```

### 6. 比价
```bash
curl -s "http://localhost:8080/api/discovery/compare?ids=1,6" | jq '.stats'
```

### 7. 查看购物车
```bash
curl -s http://localhost:8080/api/cart \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 8. 下单流程
```bash
# 添加商品到购物车
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"offerId":3,"quantity":1}' | jq .

# 下单
UUID=$(uuidgen | tr '[:upper:]' '[:lower:]')
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"idempotencyKey\":\"$UUID\"}" | jq '{orderNo,status,totalAmountYuan}'
```

### 9. 提交评价
```bash
curl -s -X POST http://localhost:8080/api/reviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"storeId":1,"orderId":1,"rating":4,"content":"不错的水果店","tags":["新鲜","实惠"]}' | jq .
```

## 前端启动

```bash
cd web
npm run dev
# 访问 http://localhost:5173
```

## Docker Compose Profiles

| Profile | 包含服务 |
|---------|----------|
| core | MySQL + Redis + RabbitMQ + Nacos |
| marketplace | 所有 7 个微服务 |
| observability | Prometheus + Grafana + Loki + Tempo |
| documents | MinIO + ClamAV |

```bash
# 仅基础设施
docker compose -f deploy/compose/compose.core.yaml --profile core up -d

# 完整环境
docker compose -f deploy/compose/compose.yaml --profile marketplace up -d

# 带可观测性
docker compose -f deploy/compose/compose.observability.yaml --profile observability up -d
```

## 常用命令

```bash
# 全量编译
./mvnw compile -T 1C

# 运行测试
./mvnw test

# 仅编译某个服务
./mvnw compile -pl apps/order-service -am

# 前端 lint + 类型检查
cd web && npm run lint && npm run typecheck

# 清理
docker compose -f deploy/compose/compose.core.yaml down -v
```
