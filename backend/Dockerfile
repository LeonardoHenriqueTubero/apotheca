# Multi-stage build: a JDK image compiles the jar, a smaller JRE image runs it.
# Tests are not run here: they need Docker (Testcontainers) and already run in CI (backend-ci.yml).

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only the build files first, so the dependency download is cached
# and repeated builds skip it unless pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B -q

COPY src src
RUN ./mvnw package -DskipTests -B -q

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user instead of root.
RUN useradd --system --no-create-home apotheca
USER apotheca

COPY --from=build /app/target/apotheca-*.jar app.jar

# Render's free plan has 512 MB of RAM; let the JVM size its heap from the container limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# The port comes from the PORT environment variable (set by Render), 8080 by default.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
