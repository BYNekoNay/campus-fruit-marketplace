# 版本基线 Version Baseline

## 运行环境

| 组件   | 版本                               | 备注                   |
| ------ | ---------------------------------- | ---------------------- |
| Java   | 21 LTS                             | Amazon Corretto / Eclipse Temurin |
| Maven  | 3.9.9 (wrapper)                    | 使用项目自带 mvnw      |
| Node.js| 22 LTS                             |                        |
| Docker | 27+                                |                        |

## 框架版本

| 框架                          | 版本       | 备注                  |
| ----------------------------- | ---------- | --------------------- |
| Spring Boot                   | 3.4.5      |                       |
| Spring Cloud                  | 2024.0.1   | BOM 管理              |
| Spring Cloud Alibaba          | 2023.0.3.2 |                       |
| Spring Cloud Gateway          | 由 SC BOM 管理 | -                 |
| Spring Authorization Server   | 1.4.x      |                       |
| MySQL                         | 8.4 LTS    |                       |
| Redis                         | 7.4+       |                       |
| RabbitMQ                      | 4.0        |                       |

## 服务器端口

| 服务      | HTTP 端口 |
| --------- | --------- |
| Gateway   | 8080      |
| Identity  | 8081      |
| Merchant  | 8082      |
| Offer     | 8083      |
| Order     | 8084      |
| Review    | 8085      |
| Discovery | 8086      |

## 兼容性矩阵

- Spring Boot 3.4.x 兼容 Spring Cloud 2024.0.x
- Spring Cloud Alibaba 2023.0.3.x 兼容 Spring Cloud 2024.0.x
- Flyway 11.x 兼容 MySQL 8.4
- Testcontainers 1.20.x 兼容 Docker 27+

## CI/CD

- GitHub Actions CI 工作流：`.github/workflows/ci.yaml`
- 触发条件：push 到 main 分支、pull_request 到 main
- 构建流程：
  1. Backend: Maven 构建，排除 web 模块
  2. Frontend: npm lint + typecheck + 单元测试
  3. Compose Smoke: 启动核心服务并验证健康检查

## 可观测性

- Prometheus: 抓取所有微服务的 `/actuator/prometheus` 端点
- Grafana: 统一可视化面板
- Loki: 日志聚合
- Tempo: 分布式链路追踪 (OTLP gRPC on port 4317)

## 事件契约

- 事件信封 JSON Schema: `contracts/events/event-envelope-v1.json`
- 版本: v1
- 格式: JSON

## 验证记录

| 日期       | 验证人   | 验证项           | 结果   |
| ---------- | -------- | ---------------- | ------ |
| 2026-07-29 | WorkBuddy | 项目骨架创建     | 已创建 |
| 2026-07-29 | WorkBuddy | Docker Compose 配置 | 已创建 |
| 2026-07-29 | WorkBuddy | CI/CD 工作流     | 已创建 |
| 2026-07-29 | WorkBuddy | 事件契约         | 已创建 |
