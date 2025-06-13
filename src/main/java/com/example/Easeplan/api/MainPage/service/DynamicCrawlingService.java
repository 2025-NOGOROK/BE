package com.example.Easeplan.api.MainPage.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
public class DynamicCrawlingService {

    public String crawlSamsungHospital() {
        // 크롬 드라이버 경로 설정
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        // 크롬 옵션 설정 (헤드리스 모드)
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/usr/bin/google-chrome-stable");
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // 크롬 드라이버 초기화
        WebDriver driver = new ChromeDriver(options);

        try {
            // 삼성병원 로그인 페이지 접속
            driver.get("https://www.samsunghospital.com");

            // 아이디 및 비밀번호 입력
            WebElement userIdField = driver.findElement(By.id("MST_ID"));
            WebElement passwordField = driver.findElement(By.id("PASS"));
            userIdField.sendKeys("your-username");  // 실제 아이디 입력
            passwordField.sendKeys("your-password");  // 실제 비밀번호 입력

            // 로그인 버튼 클릭
            WebElement loginButton = driver.findElement(By.xpath("//a[@role='button']"));
            loginButton.click();

            // 페이지 로드 대기
            Thread.sleep(5000); // 충분히 대기

            // 로그인 후 크롤링할 페이지로 이동
            driver.get("http://www.samsunghospital.com/home/healthMedical/private/lifeClinicStress05.do");
            Thread.sleep(3000); // 페이지 로드 대기

            // 크롤링: section id=contents 내부의 이미지 추출
            WebElement section = driver.findElement(By.id("contents"));
            List<WebElement> images = section.findElements(By.tagName("img"));

            StringBuilder result = new StringBuilder();
            for (WebElement img : images) {
                String imgUrl = img.getAttribute("src");
                result.append(imgUrl).append("\n");
            }

            return result.toString();
        } catch (Exception e) {
            log.error("크롤링 실패: ", e); // 보다 구체적인 예외 로깅 추가
            e.printStackTrace();
            return "크롤링 실패: " + e.getMessage();
        } finally {
            driver.quit();
        }
    }

}
