package com.example.Easeplan.api.MainPage.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class DynamicCrawlingService1 {

    public String crawlLawtimesArticle() {
        // 크롬 드라이버 경로 설정
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        // 크롬 옵션 설정 (헤드리스 모드 등)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = null;
        StringBuilder result = new StringBuilder();

        try {
            // 크롬 드라이버 시작
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // Lawtimes 기사 페이지 열기
            driver.get("https://www.lawtimes.co.kr/opinion/197462");

            // div.css-1rywr2z.e1ogx6dn0 요소가 화면에 나타날 때까지 기다림
            WebElement articleDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.css-1rywr2z.e1ogx6dn0")));

            // 1. 텍스트 추출
            result.append("[글]\n");
            result.append(articleDiv.getText()).append("\n\n");

            // 2. 이미지 src 추출
            List<WebElement> images = articleDiv.findElements(By.tagName("img"));
            result.append("[이미지]\n");
            for (WebElement img : images) {
                String imgUrl = img.getAttribute("src");
                result.append(imgUrl).append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "크롤링 실패: " + e.getMessage();
        } finally {
            if (driver != null) {
                driver.quit(); // driver 종료
            }
        }
    }
}
