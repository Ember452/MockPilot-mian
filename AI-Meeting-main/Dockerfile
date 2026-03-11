# Xunzhi backend Dockerfile
# Multi-stage build for a lean runtime image.

ARG BUILDER_IMAGE=maven:3.9-eclipse-temurin-17
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre-jammy

FROM ${BUILDER_IMAGE} AS builder

WORKDIR /app

COPY pom.xml .
COPY admin/pom.xml admin/

RUN mvn dependency:go-offline -B

COPY . .

RUN mvn clean package -Dmaven.test.skip=true -B -e

FROM ${RUNTIME_IMAGE}

LABEL maintainer="xunzhi-agent-team"
LABEL description="Xunzhi Agent Backend Service"
LABEL version="1.0.0"

RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r xunzhi && useradd -r -g xunzhi xunzhi

WORKDIR /app

RUN mkdir -p /app/logs /app/data && chown -R xunzhi:xunzhi /app

COPY --from=builder /app/admin/target/xunzhi-admin-*.jar app.jar

RUN chown xunzhi:xunzhi app.jar

USER xunzhi

EXPOSE 8002

ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"
ENV SPRING_PROFILES_ACTIVE=prod
ENV XUNZHI_STORAGE_BASE_DIR=/app/data
ENV XUNZHI_LOG_DIR=/app/logs

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8002/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
