# ============================================
# GATEWAY DOCKERFILE
# ============================================
# File: gateway/Dockerfile
# ============================================

# ============================================
# STAGE 1: BUILD
# ============================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 1. Copy parent POM
COPY pom.xml ./pom.xml

# 2. Copy all module POMs
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml

# 3. Install parent POM
RUN mvn install -N -DskipTests -B

# 4. Copy and install common-lib (if gateway needs it in future)
COPY common-lib/src ./common-lib/src
RUN mvn install -pl common-lib -DskipTests -B

# 5. Copy and build gateway
COPY gateway/src ./gateway/src
RUN mvn clean package -pl gateway -DskipTests -B

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from builder stage
COPY --from=builder /app/gateway/target/*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
