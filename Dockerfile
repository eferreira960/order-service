# ============================================
# Stage 1: Build
# ============================================
FROM gradle:8.7-jdk17 AS build

WORKDIR /app

COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN gradle build -x test --no-daemon

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

RUN apk add --no-cache tzdata curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 9898 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:9898/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "/app/app.jar"]