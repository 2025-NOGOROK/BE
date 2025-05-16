FROM eclipse-temurin:17-jdk-alpine
COPY build/libs/Easeplan-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]