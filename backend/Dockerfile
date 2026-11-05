# syntax=docker/dockerfile:1
# Build from the backend/ Gradle root (multi-module build).
#   Local (compose) — compile from source:
#     docker build -t nl-portal-backend backend/
#   CI/release — reuse the build-once output (app/build/libs/app.jar already produced):
#     docker build --build-arg BUILD_MODE=ci -t nl-portal-backend backend/
# BuildKit (default since Docker 23.0) skips the unused build stage entirely.
ARG BUILD_MODE=local

# --- local: compile the app module (and its project deps) from source ---
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-alpine AS build-local
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew && ./gradlew :app:bootJar --no-daemon

# --- ci: reuse the pre-built boot jar from the context (build-once); no compile ---
FROM alpine AS build-ci
WORKDIR /workspace
COPY app/build/libs/app.jar ./app/build/libs/app.jar

# --- select the build source via BUILD_MODE; BuildKit prunes the other stage ---
FROM build-${BUILD_MODE} AS build

FROM eclipse-temurin:25-jre-alpine AS image

# Create non-root user
RUN addgroup -S -g 1001 appgroup && adduser -S -u 1001 -G appgroup -H appuser

# Copy the Spring Boot fat jar with proper ownership
COPY --chown=1001:1001 --from=build /workspace/app/build/libs/app.jar /opt/app.jar

# Switch to non-root user
USER 1001

EXPOSE 8080

LABEL org.opencontainers.image.description="NL Portal Backend."

ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
