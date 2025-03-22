# ===== STAGE 1: Build the application =====
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# ===== STAGE 2: Runtime =====
FROM openjdk:17-jdk-slim

WORKDIR /app

# Install curl + unzip
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Prepare folders
RUN mkdir -p /app/gtfs /app/data /tmp/build_resources

# Copy JAR from build stage
COPY --from=builder /app/target/routecalc-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy resources if they exist — handle in RUN, not COPY
COPY src /tmp/src

# Conditionally copy or download OSM file
RUN if [ -f "/tmp/src/main/resources/data/ireland-and-northern-ireland-latest.osm.pbf" ]; then \
      echo "Copying local OSM PBF file..."; \
      cp /tmp/src/main/resources/data/ireland-and-northern-ireland-latest.osm.pbf /app/data/; \
    else \
      echo "Downloading OSM PBF file..."; \
      curl -L -o /app/data/ireland-and-northern-ireland-latest.osm.pbf \
      https://download.geofabrik.de/europe/ireland-and-northern-ireland-latest.osm.pbf; \
    fi

# Conditionally copy or download & unzip GTFS data
RUN if [ -d "/tmp/src/main/resources/gtfs" ]; then \
      echo "Copying local GTFS data..."; \
      cp -r /tmp/src/main/resources/gtfs/* /app/gtfs/; \
    else \
      echo "Downloading GTFS ZIP and extracting..."; \
      curl -L -o /tmp/gtfs.zip \
      https://transitfeeds.com/p/transport-for-ireland/782/latest/download && \
      unzip /tmp/gtfs.zip -d /app/gtfs && rm /tmp/gtfs.zip; \
    fi

# Verify results
RUN ls -l /app && ls -l /app/data && ls -l /app/gtfs

ENTRYPOINT ["java", "-jar", "app.jar"]
