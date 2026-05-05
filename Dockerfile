# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom first so dependency downloads are cached in a separate layer
# (only re-runs when pom.xml changes)
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q

# Copy source and package — skip tests (they run in CI, not at image build time)
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S tmf && adduser -S tmf -G tmf
USER tmf

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
