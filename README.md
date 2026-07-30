# 校园水果商城 Campus Fruit Marketplace

基于 Spring Cloud 微服务架构的校园水果在线交易平台。

## 技术栈

### 后端
- **Java 21 LTS** — 运行环境 (Amazon Corretto / Eclipse Temurin)
- **Spring Boot 3.4.5** — 应用框架
- **Spring Cloud 2024.0.1** — 微服务基础设施
- **Spring Cloud Alibaba 2023.0.3.2** — 服务注册与配置 (Nacos)
- **Spring Cloud Gateway** — API 网关
- **Spring Authorization Server 1.4.x** — 认证授权
- **MySQL 8.4** — 关系数据库
- **Redis 7.4** — 缓存
- **RabbitMQ 4.0** — 消息队列

### 前端
- **Node.js 22 LTS** — 运行环境

### 可观测性
- **Prometheus** — 指标收集
- **Grafana** — 监控面板
- **Loki** — 日志聚合
- **Tempo** — 分布式链路追踪

## 快速启动

### 前置要求
- Java 21+
- Node.js 22+
- Docker 27+ & Docker Compose
- Maven 3.9+（或使用项目自带的 mvnw）

### 1. 启动基础设施

```bash
# 启动核心中间件 (MySQL, Redis, RabbitMQ, Nacos)
docker compose -f deploy/compose/compose.core.yaml --profile core up -d

# 启动可观测性组件 (Prometheus, Grafana, Loki, Tempo)
docker compose -f deploy/compose/compose.observability.yaml --profile observability up -d

# 或一次性启动所有
docker compose -f deploy/compose/compose.core.yaml -f deploy/compose/compose.observability.yaml --profile all up -d
```

### 2. 启动后端服务

```bash
# 构建所有后端模块
./mvnw clean package -DskipTests

# 按顺序启动服务
# 1. Discovery (Nacos 已启动则跳过)
# 2. Gateway
# 3. Identity
# 4. Merchant
# 5. Offer
# 6. Order
# 7. Review
```

### 3. 启动前端

```bash
npm ci
npm run dev
```

## 项目结构

```
campus-fruit-marketplace/
├── .github/
│   └── workflows/
│       └── ci.yaml                   # CI/CD 流水线
├── deploy/
│   └── compose/
│       ├── compose.core.yaml         # 核心基础设施
│       ├── compose.observability.yaml # 可观测性组件
│       └── prometheus/
│           └── prometheus.yml        # Prometheus 配置
├── contracts/
│   └── events/
│       └── event-envelope-v1.json    # 事件信封 JSON Schema
├── docs/
│   └── architecture/
│       └── version-baseline.md       # 版本基线
├── gateway/                          # API 网关
├── identity/                         # 认证授权服务
├── merchant/                         # 商家服务
├── offer/                            # 商品服务
├── order/                            # 订单服务
├── review/                           # 评价服务
├── discovery/                        # 服务发现
├── web/                              # 前端项目
├── pom.xml                           # Maven 父 POM
└── README.md
```

## 服务端口

| 服务      | HTTP 端口 | 说明       |
| --------- | --------- | ---------- |
| Gateway   | 8080      | API 网关   |
| Identity  | 8081      | 认证授权   |
| Merchant  | 8082      | 商家管理   |
| Offer     | 8083      | 商品管理   |
| Order     | 8084      | 订单管理   |
| Review    | 8085      | 评价管理   |
| Discovery | 8086      | 服务发现   |

### 基础设施端口

| 组件      | 端口               | 管理界面          |
| --------- | ------------------ | ----------------- |
| MySQL     | 3306               | -                 |
| Redis     | 6379               | -                 |
| RabbitMQ  | 5672, 15672        | http://localhost:15672 |
| Nacos     | 8848, 9848         | http://localhost:8848/nacos |
| Prometheus| 9090               | http://localhost:9090      |
| Grafana   | 3000               | http://localhost:3000      |
| Loki      | 3100               | -                           |
| Tempo     | 3200, 4317         | -                           |

## CI/CD

GitHub Actions 在 push 到 main 分支或创建 PR 时自动触发：
1. **Backend Build** — Maven 构建与测试
2. **Frontend Build** — npm lint、类型检查、单元测试
3. **Compose Smoke** — 启动核心服务并验证健康状态
