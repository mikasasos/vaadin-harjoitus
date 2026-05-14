FROM eclipse-temurin:25-jdk-jammy
VOLUME /tmp
COPY target/vaadin-harjoitusty-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]