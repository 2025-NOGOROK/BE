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
public class DynamicCrawlingService2 {

    public String crawlNctStressPage() {
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = null;
        StringBuilder result = new StringBuilder();

        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 페이지 로드
            driver.get("https://www.nct.go.kr/distMental/rating/rating01_6_3.do");

            // 요소가 로드될 때까지 기다리기 (contentDiv가 나타날 때까지)
            WebElement contentDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.content.fClr")));

            // 텍스트 추출
            result.append("[글]\n");
            result.append(contentDiv.getText()).append("\n\n");

            // 이미지 src 추출
            List<WebElement> images = contentDiv.findElements(By.tagName("img"));
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
                driver.quit();
            }
        }
    }
}
