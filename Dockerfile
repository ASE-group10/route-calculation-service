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

# Install curl and unzip (for downloading and extracting files)
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /app/gtfs /app/data /tmp/build_resources

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/routecalc-0.0.1-SNAPSHOT.jar /app/app.jar

# Optionally copy resource files (won't fail if missing)
COPY src/main/resources/ /tmp/build_resources/ || true

# Conditional logic for OSM PBF
RUN if [ -f "/tmp/build_resources/data/ireland-and-northern-ireland-latest.osm.pbf" ]; then \
      echo "Copying existing OSM PBF file..."; \
      cp /tmp/build_resources/data/ireland-and-northern-ireland-latest.osm.pbf /app/data/; \
    else \
      echo "Downloading OSM PBF file..."; \
      curl -L -o /app/data/ireland-and-northern-ireland-latest.osm.pbf \
      "https://download.geofabrik.de/europe/ireland-and-northern-ireland-latest.osm.pbf"; \
    fi

# Conditional logic for GTFS
RUN if [ -d "/tmp/build_resources/gtfs" ]; then \
      echo "Copying existing GTFS directory..."; \
      cp -r /tmp/build_resources/gtfs/* /app/gtfs/; \
    else \
      echo "Downloading GTFS ZIP and extracting..."; \
      curl -L -o /tmp/gtfs.zip \
      "https://transitfeeds.com/p/transport-for-ireland/782/latest/download"; \
      unzip /tmp/gtfs.zip -d /app/gtfs/ && rm /tmp/gtfs.zip; \
    fi

# Verify
RUN ls -l /app && ls -l /app/data && ls -l /app/gtfs

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
