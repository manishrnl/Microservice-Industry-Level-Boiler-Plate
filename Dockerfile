# =========================
# Stage 1: Build
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Microservice name. Accepts either "api-gateway" or "backend/api-gateway".
ARG MODULE=api-gateway

# Copy root pom
COPY pom.xml .

# Copy backend source
COPY backend ./backend

# Build only requested service + dependencies.
RUN --mount=type=cache,target=/root/.m2 set -eux; \
    MODULE_PATH="${MODULE#backend/}"; \
    test -n "$MODULE_PATH"; \
    test -f "backend/${MODULE_PATH}/pom.xml"; \
    mvn clean package -DskipTests -pl "backend/${MODULE_PATH}" -am; \
    cp "backend/${MODULE_PATH}"/target/*.jar /tmp/app.jar

# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Optional curl
RUN apt-get update &&     apt-get install -y --no-install-recommends curl &&     rm -rf /var/lib/apt/lists/*

# Copy generated jar
COPY --from=build /tmp/app.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]
