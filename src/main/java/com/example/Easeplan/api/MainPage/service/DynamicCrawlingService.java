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
        // ChromeDriver 경로 설정 (서버에서 설치된 경로로 수정 필요)
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver"); // 서버에 맞는 경로로 설정

        // ChromeOptions 설정 (headless 모드)
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/usr/bin/google-chrome-stable");  // 서버에서 구글 크롬 실행 경로
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage"); // 리소스 부족 문제 해결

        // WebDriver 생성
        WebDriver driver = new ChromeDriver(options);

        StringBuilder result = new StringBuilder();

        try {
            // 삼성병원 로그인 페이지로 접근
            driver.get("https://www.samsunghospital.com/home/member/login.do?prevURI=http%3A%2F%2Fwww.samsunghospital.com%2Fhome%2Fmain%2Findex.do");  // 로그인 페이지 URL (정확한 로그인 URL로 변경 필요)

            // 1. 사용자 아이디 입력 (MST_ID)
            WebElement userIdField = driver.findElement(By.id("MST_ID"));
            userIdField.sendKeys("hyolin");  // 실제 사용자 ID를 입력

            // 2. 비밀번호 입력 (PASS)
            WebElement passwordField = driver.findElement(By.id("PASS"));
            passwordField.sendKeys("mongsillove1!");  // 실제 비밀번호를 입력

            // 3. 로그인 버튼 클릭 (a 태그)
            WebElement loginButton = driver.findElement(By.xpath("//a[@role='button']"));
            loginButton.click();  // 로그인 버튼 클릭

            // 로그인 후 페이지가 로드될 시간을 잠시 대기
            Thread.sleep(5000);  // 로그인 후 5초 대기 (네트워크 상태에 따라 조정)

            // 로그인 후 크롤링할 페이지로 이동
            driver.get("http://www.samsunghospital.com/home/healthMedical/private/lifeClinicStress05.do");

            // 페이지가 로드될 시간을 잠시 대기
            Thread.sleep(3000);

            // 4. section id="contents" 찾기
            WebElement section = driver.findElement(By.id("contents"));

            // 5. section 내부의 모든 <img> 태그 찾기
            List<WebElement> images = section.findElements(By.tagName("img"));

            // 6. 이미지 src 모두 저장
            result.append("[section id=contents 내부 이미지 목록]\n");
            for (WebElement img : images) {
                String imgUrl = img.getAttribute("src");
                result.append(imgUrl).append("\n");
            }

            // 추가적으로 원하는 텍스트를 추출하고자 하면 아래와 같이 작성
            result.append("\n[section-step 텍스트]\n");
            List<WebElement> steps = driver.findElements(By.className("section-step"));
            for (WebElement step : steps) {
                result.append(step.getText()).append("\n");
            }

            return result.toString();  // 결과를 문자열로 반환

        } catch (Exception e) {
            e.printStackTrace();
            return "크롤링 실패: " + e.getMessage(); // 에러 메시지 반환
        } finally {
            driver.quit();  // 드라이버 종료
        }
    }
}
