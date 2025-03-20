# ===== STAGE 1: Build the application =====
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and resolve dependencies (caching)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ===== STAGE 2: Create the final runtime image =====
FROM openjdk:17-jdk-slim

WORKDIR /app

# Create necessary directories
RUN mkdir -p /app/gtfs /app/data

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/routecalc-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy necessary data files
COPY src/main/resources/data/ireland-and-northern-ireland-latest.osm.pbf /app/data/
COPY src/main/resources/gtfs /app/gtfs/

# Verify the copied files
RUN ls -l /app && ls -l /app/data && ls -l /app/gtfs

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
