FROM eclipse-temurin:17-jdk-alpine

# 1단계: JAR 복사 (빌드 결과물)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 2단계: 리소스 외부 경로 복사 (이중화)
COPY src/main/resources/ /external-resources/

# 3단계: 환경 변수 설정 (선택)
ENV GOOGLE_APPLICATION_CREDENTIALS=/external-resources/firebase/serviceAccountKey.json

ENTRYPOINT ["java", "-jar", "app.jar"]
