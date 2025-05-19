FROM eclipse-temurin:17-jdk-alpine

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 리소스 폴더를 외부 경로로 복사 (이중화)
COPY src/main/resources/ /external-resources/

ENV GOOGLE_APPLICATION_CREDENTIALS=/external-resources/firebase/serviceAccountKey.json

ENTRYPOINT ["java", "-jar", "app.jar"]
