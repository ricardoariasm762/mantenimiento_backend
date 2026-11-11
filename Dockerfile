# ==============================================================================
# Multi-stage Dockerfile for Spring Boot 3 + Java 21 + ONNX Runtime (Render Deployment)
# ==============================================================================

# Stage 1: Build JAR with Maven and OpenJDK 21
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and build package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Image with JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy compiled JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Environment variable for port (Render injects $PORT)
ENV PORT=8082
EXPOSE 8082

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

