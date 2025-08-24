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
import java.util.NoSuchElementException;

@Slf4j
@Service
public class DynamicCrawlingService {

    public Map<String, Object> crawlStressArticleWithImages() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> contents = new ArrayList<>();

        System.setProperty("webdriver.chrome.driver", "D:\\\\chromedriver-win64\\\\chromedriver.exe" );
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        try {
            String targetUrl = "https://doctornow.co.kr/content/magazine/ce509c92b93d4329b03435840ef2a608";
            driver.get(targetUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            // 1) 안정적인 루트: main article 등장까지 대기
            WebElement article = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("main article")));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 2) 본문 요소 수집: h1~h5, p, li, figure, img, (텍스트 있는 leaf div)
            List<WebElement> elements = article.findElements(By.xpath(
                    ".//*[self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or " +
                            "self::p or self::li or self::figure or self::img or " +
                            "(self::div and normalize-space(string(.))!='')]" // 텍스트 가진 div
            ));

            for (WebElement elem : elements) {
                String tag = elem.getTagName();

                // 3) 래핑 div(자식에 p/li/figure/h*/img가 있는 경우)는 스킵 → 중복 방지
                if ("div".equals(tag)) {
                    boolean hasContentChild = !elem.findElements(By.xpath(
                            ".//*[self::p or self::li or self::figure or self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::img]"
                    )).isEmpty();
                    if (hasContentChild) continue; // 래퍼 div는 건너뜀
                }

                Map<String, String> item = new HashMap<>();

                if ("figure".equals(tag)) {
                    // figure 내 img 우선 추출
                    try {
                        WebElement img = elem.findElement(By.cssSelector("img"));
                        String src = pickImgSrc(img);
                        if (src != null && !src.isBlank()) {
                            item.put("type", "image");
                            item.put("tag", "img");
                            item.put("src", src);
                            contents.add(new HashMap<>(item));
                        }
                    } catch (NoSuchElementException ignore) {}
                    String figText = elem.getText().trim();
                    if (!figText.isEmpty()) {
                        item.clear();
                        item.put("type", "text");
                        item.put("tag", "figure");
                        item.put("content", figText);
                        contents.add(new HashMap<>(item));
                    }
                    continue;
                }

                if ("img".equals(tag)) {
                    String src = pickImgSrc(elem);
                    if (src != null && !src.isBlank()) {
                        item.put("type", "image");
                        item.put("tag", "img");
                        item.put("src", src);
                        contents.add(new HashMap<>(item));
                    }
                    continue;
                }

                // h1~h5, p, li, div(leaf) → 텍스트
                String text = elem.getText().trim();

                // ::before에 점/아이콘이 들어갈 가능성 보정
                try {
                    String before = (String) js.executeScript(
                            "return window.getComputedStyle(arguments[0], '::before').getPropertyValue('content');",
                            elem
                    );
                    if (before != null) {
                        before = before.replaceAll("^\"|\"$", "");
                        if (!"none".equals(before) && !before.isEmpty()) {
                            text = before + " " + text;
                        }
                    }
                } catch (Exception ignore) {}

                if (!text.isEmpty()) {
                    item.put("type", "text");
                    item.put("tag", tag.equals("li") ? "li" : tag); // div면 div로 남김(요청하신 노란 div 포함)
                    item.put("content", text);
                    contents.add(new HashMap<>(item));
                }
            }

            // 4) 대표/커버 이미지(지연 로딩 대응)
            List<WebElement> coverImgs = article.findElements(By.cssSelector("picture img, img.object-cover, img"));
            for (WebElement img : coverImgs) {
                String src = pickImgSrc(img);
                if (src != null && !src.isBlank()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("type", "image");
                    item.put("tag", "img");
                    item.put("src", src);
                    contents.add(0, item); // 맨 앞에 넣어 대표처럼
                    break; // 대표 1장만
                }
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

    // 보조 메서드(클래스 내부에 추가)
    private static String pickImgSrc(WebElement img) {
        String s = img.getAttribute("src");
        if (s == null || s.isBlank()) s = img.getAttribute("data-src");
        if (s == null || s.isBlank()) s = img.getAttribute("data-lazy");
        if (s == null || s.isBlank()) {
            String srcset = img.getAttribute("srcset");
            if (srcset != null && !srcset.isBlank()) {
                // srcset에서 첫 URL만 추출
                String[] parts = srcset.split("\\s*,\\s*");
                if (parts.length > 0) s = parts[0].split("\\s+")[0];
            }
        }
        return s;
    }

}
