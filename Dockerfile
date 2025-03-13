FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/routecalc-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/gtfs /app/data

ENV RUNNING_IN_DOCKER=1

ENTRYPOINT ["java", "-jar", "app.jar"]
