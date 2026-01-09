# Build stage
FROM gradle:8.10-jdk21 AS build
WORKDIR /app

# Copy Gradle files first for better caching
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Download dependencies (this layer will be cached if build.gradle.kts doesn't change)
RUN ./gradlew dependencies --no-daemon || true

# Copy only main source code (exclude test)
COPY src/main ./src/main

# Build the application without tests
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the JAR file from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

