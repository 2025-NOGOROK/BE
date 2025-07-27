package com.example.Easeplan.api.MainPage.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class DynamicCrawlingService {

    public Map<String, Object> crawlStressArticleWithImages() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> contents = new ArrayList<>();

        // 크롬 드라이버 경로 설정
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless"); // 테스트 시에는 주석 처리
        options.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);

        try {
            String mainUrl = "https://doctornow.co.kr/";
            String targetUrl = "https://doctornow.co.kr/content/magazine/ce509c92b93d4329b03435840ef2a608";

            // ✅ 세션 확보
            driver.get(mainUrl);
            Thread.sleep(2000);

            // ✅ 본문 페이지 이동
            driver.navigate().to(targetUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[class*=hZqIoV]")));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // ✅ 본문 요소 수집
            List<WebElement> elements = driver.findElements(By.cssSelector(
                    "div[class*=hZqIoV] > h1, div[class*=hZqIoV] > h2, div[class*=hZqIoV] > h3, " +
                            "div[class*=hZqIoV] > h4, div[class*=hZqIoV] > h5, div[class*=hZqIoV] > p, " +
                            "div[class*=hZqIoV] > li, div[class*=hZqIoV] > figure"
            ));

            for (WebElement elem : elements) {
                Map<String, String> item = new HashMap<>();
                String tagName = elem.getTagName();

                switch (tagName) {
                    case "figure":
                        try {
                            WebElement img = elem.findElement(By.cssSelector("img"));
                            if (img != null) {
                                item.put("type", "image");
                                item.put("tag", "img");
                                item.put("src", img.getAttribute("src"));
                                contents.add(new HashMap<>(item));
                            }
                            String figText = elem.getText().trim();
                            if (!figText.isEmpty()) {
                                item.clear();
                                item.put("type", "text");
                                item.put("tag", "figure");
                                item.put("content", figText);
                                contents.add(item);
                            }
                        } catch (Exception ignore) {}
                        break;

                    case "h1":
                    case "h2":
                    case "h3":
                    case "h4":
                    case "h5":
                    case "p":
                    case "li":
                        String text = elem.getText().trim();
                        try {
                            String before = (String) js.executeScript(
                                    "return window.getComputedStyle(arguments[0], '::before').getPropertyValue('content');",
                                    elem
                            );
                            if (before != null) {
                                before = before.replaceAll("^\"|\"$", "");
                                if (!before.equals("none") && !before.isEmpty()) {
                                    text = before + " " + text;
                                }
                            }
                        } catch (Exception ignore) {}

                        if (!text.isEmpty()) {
                            item.put("type", tagName.equals("li") ? "list" : "text");
                            item.put("tag", tagName);
                            item.put("content", text);
                            contents.add(item);
                        }
                        break;
                }
            }

            // ✅ <header> 영역 대표 이미지 크롤링
            try {
                WebElement header = driver.findElement(By.cssSelector("header[class*=sc-cca45d9c]"));
                List<WebElement> headerImgs = header.findElements(By.tagName("img"));

                for (WebElement img : headerImgs) {
                    Map<String, String> item = new HashMap<>();
                    item.put("type", "image");
                    item.put("tag", "img");
                    item.put("src", img.getAttribute("src"));
                    contents.add(0, item); // 맨 앞에 삽입
                }
            } catch (Exception e) {
                log.warn("header 대표 이미지 크롤링 실패: {}", e.getMessage());
            }

            // ✅ .object-cover 이미지도 추가 수집
            List<WebElement> coverImages = driver.findElements(By.cssSelector("img.object-cover"));
            for (WebElement img : coverImages) {
                Map<String, String> item = new HashMap<>();
                item.put("type", "image");
                item.put("tag", "img");
                item.put("src", img.getAttribute("src"));
                contents.add(0, item);
            }

            result.put("contents", contents);

        } catch (Exception e) {
            result.put("error", "크롤링 실패: " + e.getMessage());
            log.error("크롤링 중 오류 발생", e);
        } finally {
            driver.quit();
        }

        return result;
    }
}
