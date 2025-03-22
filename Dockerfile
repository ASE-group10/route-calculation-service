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

# Install curl and tar (needed for downloads and extraction)
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /app/gtfs /app/data

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/routecalc-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy or download OSM PBF data
COPY src/main/resources/data/ireland-and-northern-ireland-latest.osm.pbf /app/data/ 2>/dev/null || \
    (echo "OSM PBF file not found locally, downloading..." && \
    curl -L -o /app/data/ireland-and-northern-ireland-latest.osm.pbf \
    "https://download.geofabrik.de/europe/ireland-and-northern-ireland-latest.osm.pbf")

# Copy or download GTFS data
RUN if [ -d "src/main/resources/gtfs" ]; then \
      echo "Copying existing GTFS data..."; \
      cp -r src/main/resources/gtfs/* /app/gtfs/; \
    else \
      echo "GTFS data not found locally, downloading..."; \
      curl -L -o /app/gtfs/latest-gtfs.zip \
      "https://transitfeeds.com/p/transport-for-ireland/782/latest/download"; \
      echo "Extracting GTFS data..."; \
      unzip /app/gtfs/latest-gtfs.zip -d /app/gtfs/; \
      rm /app/gtfs/latest-gtfs.zip; \
    fi

# Verify the copied/downloaded files
RUN ls -l /app && ls -l /app/data && ls -l /app/gtfs

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
