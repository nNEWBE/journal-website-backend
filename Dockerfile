# Multi-stage build for Spring Boot Backend on Render / Cloud
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy gradle files first for layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Fix line endings and permissions on gradlew
RUN chmod +x gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build production jar
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Render assigns a dynamic port via the PORT environment variable
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
