# ============================================================
# Multi-stage Dockerfile for OmniChat Microservices
# Build: JDK 21 (Maven) → Runtime: JRE 21 Alpine (~200MB)
# Usage: docker build --build-arg SERVICE_NAME=conversation-service .
# ============================================================

# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Install dos2unix to handle line ending issues
RUN apk add --no-cache dos2unix

# Copy Maven wrapper & parent POM first (Docker layer caching)
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Fix line endings for mvnw
RUN dos2unix mvnw && chmod +x mvnw

# Copy all module POMs for dependency resolution
COPY conversation-service/pom.xml conversation-service/pom.xml
COPY routing-service/pom.xml routing-service/pom.xml
COPY customer-service/pom.xml customer-service/pom.xml
COPY integration-service/pom.xml integration-service/pom.xml
COPY websocket-service/pom.xml websocket-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

# Download all dependencies (cached unless POM changes)
RUN ./mvnw dependency:go-offline -B -q 2>/dev/null || true

# Build argument: which service to build
ARG SERVICE_NAME

# Copy only the target service source
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src

# Build only the target service, skip tests
RUN ./mvnw package -pl ${SERVICE_NAME} -am -DskipTests -B -q \
    && mv ${SERVICE_NAME}/target/*.jar /app.jar

# ---- Stage 2: Runtime (lightweight JRE) ----
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app.jar app.jar

# JVM tuning for containers (respect cgroup memory limits)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

USER appuser

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
