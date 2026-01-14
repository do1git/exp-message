# Build stage
FROM gradle:8.10-jdk21 AS build
WORKDIR /app

# Copy Gradle files first for better caching
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Download dependencies (this layer will be cached if build.gradle.kts doesn't change)
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon || true

# Copy Kotlin source code (rarely changes)
COPY src/main/kotlin ./src/main/kotlin
COPY src/main/resources/application.properties ./src/main/resources/application.properties

# Build without SQL files first (this layer gets cached)
RUN --mount=type=cache,target=/root/.gradle ./gradlew compileKotlin --no-daemon

# Copy SQL migration files (frequently changes - last layer)
COPY src/main/resources/db ./src/main/resources/db

# Build the final JAR
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the JAR file from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Environment variables (override at runtime)
ENV DB_HOST=localhost
ENV DB_PORT=3306
ENV DB_NAME=monolitic_db
ENV DB_USERNAME=root
ENV DB_PASSWORD=password

ENTRYPOINT ["java", "-jar", "app.jar"]
