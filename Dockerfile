FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/routecalc-0.0.1-SNAPSHOT.jar /app/routecalc-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "/app/routecalc-0.0.1-SNAPSHOT.jar"]
