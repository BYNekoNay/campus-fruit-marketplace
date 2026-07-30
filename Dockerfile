# ============================================
# 校园水果商城 — 多服务统一 Dockerfile
# 构建: docker build --build-arg SERVICE=gateway-service -t campus-fruit/gateway .
# ============================================

FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG SERVICE

WORKDIR /build

# 先复制 pom 和 libs，利用 Docker 缓存层
COPY pom.xml ./
COPY libs/ libs/

# 复制所有服务的 pom.xml（仅 pom，不含源码），满足 Maven reactor 要求
COPY apps/gateway-service/pom.xml apps/gateway-service/
COPY apps/identity-service/pom.xml apps/identity-service/
COPY apps/merchant-service/pom.xml apps/merchant-service/
COPY apps/offer-service/pom.xml apps/offer-service/
COPY apps/order-service/pom.xml apps/order-service/
COPY apps/review-service/pom.xml apps/review-service/
COPY apps/discovery-service/pom.xml apps/discovery-service/

# 复制目标服务完整源码
COPY apps/${SERVICE}/src apps/${SERVICE}/src/

# 编译目标服务及其依赖模块
RUN mvn package -pl apps/${SERVICE} -am -DskipTests -q

# ============================================
# 运行时镜像
# ============================================
FROM eclipse-temurin:21-jre-alpine

ARG SERVICE

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /build/apps/${SERVICE}/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="docker"

HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
