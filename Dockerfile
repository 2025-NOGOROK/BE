FROM eclipse-temurin:17-jdk-jammy

# Chrome 및 chromedriver 설치에 필요한 패키지
RUN apt-get update && apt-get install -y \
    wget unzip gnupg2 fonts-liberation libasound2 libatk-bridge2.0-0 \
    libatk1.0-0 libdrm2 libgbm1 libgtk-3-0 libnspr4 libnss3 \
    libxcomposite1 libxdamage1 libxfixes3 libxkbcommon0 libxrandr2 xdg-utils --no-install-recommends

# Google Chrome 설치
RUN wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && dpkg -i google-chrome-stable_current_amd64.deb || apt-get install -y -f \
    && rm google-chrome-stable_current_amd64.deb

# ChromeDriver 설치 (Chrome 버전에 맞춰서)
RUN CHROME_VERSION=$(google-chrome --version | grep -oP '\d+\.\d+\.\d+') \
    && wget -q https://chromedriver.storage.googleapis.com/${CHROME_VERSION}/chromedriver_linux64.zip \
    && unzip chromedriver_linux64.zip \
    && mv chromedriver /usr/local/bin/ \
    && chmod +x /usr/local/bin/chromedriver \
    && rm chromedriver_linux64.zip

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 리소스 폴더를 외부 경로로 복사 (이중화)
COPY src/main/resources/ /external-resources/
ENV GOOGLE_APPLICATION_CREDENTIALS=/external-resources/firebase/serviceAccountKey.json

ENTRYPOINT ["java", "-jar", "app.jar"]
