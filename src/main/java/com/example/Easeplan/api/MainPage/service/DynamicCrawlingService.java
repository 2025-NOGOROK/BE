package com.example.Easeplan.api.MainPage.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class DynamicCrawlingService {

    public String crawlSamsungHospital() {
        // 크롬 드라이버 경로 설정
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        // 크롬 옵션 설정 (헤드리스 모드, User-Agent 등)
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/usr/bin/google-chrome-stable");
        options.addArguments("--headless=new"); // 최신 버전은 new 권장
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("window-size=1920,1080");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // 삼성병원 로그인 페이지 접속 (로그인 전용 URL 사용 권장)
            driver.get("https://www.samsunghospital.com/home/member/login.do?prevURI=http%3A%2F%2Fwww.samsunghospital.com%2Fhome%2Fmain%2Findex.do");

            // 아이디 및 비밀번호 입력 (wait로 대기)
            WebElement userIdField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("MST_ID")));
            WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("PASS")));
            userIdField.sendKeys("hyolin");  // 실제 아이디 입력
            passwordField.sendKeys("mongsillove1!");  // 실제 비밀번호 입력

            // 로그인 버튼 클릭 (정확한 CSS selector 사용)
            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.btn_norm[role='button']")));
            loginButton.click();

            // 로그인 후 페이지 로드 대기
            Thread.sleep(4000);

            // 크롤링할 페이지로 이동
            driver.get("https://www.samsunghospital.com/home/healthMedical/private/lifeClinicStress05.do");
            WebElement section = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("contents")));

            // 이미지 추출
            List<WebElement> images = section.findElements(By.tagName("img"));
            StringBuilder result = new StringBuilder();
            for (WebElement img : images) {
                String imgUrl = img.getAttribute("src");
                result.append(imgUrl).append("\n");
            }

            return result.toString();
        } catch (TimeoutException e) {
            log.error("요소를 찾지 못했습니다 (Timeout): ", e);
            return "크롤링 실패: 요소를 찾지 못했습니다 (Timeout)";
        } catch (NoSuchElementException e) {
            log.error("요소를 찾지 못했습니다 (NoSuchElement): ", e);
            return "크롤링 실패: 요소를 찾지 못했습니다 (NoSuchElement)";
        } catch (WebDriverException e) {
            log.error("WebDriver 에러: ", e);
            return "크롤링 실패: WebDriver 에러 - " + e.getMessage();
        } catch (Exception e) {
            log.error("크롤링 실패: ", e);
            return "크롤링 실패: " + e.getMessage();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
