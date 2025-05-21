package com.example.Easeplan.api.MainPage.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DynamicCrawlingService1 {

    public String crawlLawtimesArticle() {
        System.setProperty("webdriver.chrome.driver", "D:/chromedriver-win64/chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);
        StringBuilder result = new StringBuilder();

        try {
            driver.get("https://www.lawtimes.co.kr/opinion/197462");
            Thread.sleep(3000); // 렌더링 대기

            // 1. div.class=css-1rywr2z.e1ogx6dn0 찾기 (CSS 선택자)
            WebElement articleDiv = driver.findElement(By.cssSelector("div.css-1rywr2z.e1ogx6dn0"));

            // 2. 텍스트 추출
            result.append("[글]\n");
            result.append(articleDiv.getText()).append("\n\n");

            // 3. 이미지 src 추출
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
            driver.quit();
        }
    }
}
