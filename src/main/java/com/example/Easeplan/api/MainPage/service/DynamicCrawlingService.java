package com.example.Easeplan.api.MainPage.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DynamicCrawlingService {

    public String crawlSamsungHospital() {
        System.setProperty("webdriver.chrome.driver", "D:/chromedriver-win64/chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");

        WebDriver driver = new ChromeDriver(options);

        StringBuilder result = new StringBuilder();

        try {
            driver.get("http://www.samsunghospital.com/home/healthMedical/private/lifeClinicStress05.do");
            Thread.sleep(3000);

            // 1. section id="contents" 찾기
            WebElement section = driver.findElement(By.id("contents"));

            // 2. section 내부의 모든 <img> 태그 찾기
            List<WebElement> images = section.findElements(By.tagName("img"));

            // 3. 이미지 src 모두 저장
            result.append("[section id=contents 내부 이미지 목록]\n");
            for (WebElement img : images) {
                String imgUrl = img.getAttribute("src");
                result.append(imgUrl).append("\n");
            }

            // (기존 코드) div.section-step 텍스트도 같이 추출하고 싶으면 아래 유지
            result.append("\n[section-step 텍스트]\n");
            List<WebElement> steps = driver.findElements(By.className("section-step"));
            for (WebElement step : steps) {
                result.append(step.getText()).append("\n");
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
