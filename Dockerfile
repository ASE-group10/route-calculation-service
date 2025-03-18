FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/routecalc-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/gtfs /app/data

COPY src/main/resources/data/ireland-and-northern-ireland-latest.osm.pbf /app/data/
COPY src/main/resources/gtfs /app/gtfs/

ENV RUNNING_IN_DOCKER=1

ENTRYPOINT ["java", "-jar", "app.jar"]
