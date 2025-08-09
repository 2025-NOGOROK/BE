package com.example.Easeplan.api.MainPage.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class DynamicCrawlingService2 {

    public List<Map<String, String>> crawlBoardmixArticle() {
        List<Map<String, String>> contents = new ArrayList<>();

        // 크롬드라이버 경로 설정
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--lang=ko-KR");

        WebDriver driver = new ChromeDriver(options);

        try {
            String url = "https://www.sciencetimes.co.kr/nscvrg/view/menu/251?searchCategory=223&nscvrgSn=260043";
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // (1) 제목·설명: div.sub_txt 내부 모든 텍스트 추출
            try {
                WebElement subTxtDiv = driver.findElement(By.cssSelector("div.sub_txt"));
                List<WebElement> subTxtElements = subTxtDiv.findElements(By.xpath(".//*"));
                for (WebElement el : subTxtElements) {
                    String tag = el.getTagName();
                    String text = el.getText().trim();
                    if (!text.isEmpty()) {
                        Map<String, String> item = new HashMap<>();
                        item.put("type", "sub_txt");
                        item.put("tag", tag);
                        item.put("content", text);
                        contents.add(item);
                    }
                }
            } catch (NoSuchElementException ignore) {
                // 제목/설명 없는 경우 무시
            }

            // (2) 본문: div.atc_cont 내부에서 p/h2/text/img 수집
            WebElement contentDiv = wait.until(driver1 ->
                    driver1.findElement(By.cssSelector("div.atc_cont"))
            );

            wait.until((ExpectedCondition<Boolean>) d ->
                    !contentDiv.getText().trim().isEmpty()
            );

            List<WebElement> elements = contentDiv.findElements(By.xpath(".//*"));
            for (WebElement el : elements) {
                String tag = el.getTagName();
                String text = el.getText().trim();

                if ((tag.equals("p") || tag.equals("h2") || tag.equals("span") || tag.equals("strong") || tag.equals("b")) && !text.isEmpty()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("type", "text");
                    item.put("tag", tag);
                    item.put("content", text);
                    contents.add(item);
                }
                if (tag.equals("img")) {
                    String src = el.getAttribute("src");
                    if (src != null && !src.isEmpty()) {
                        Map<String, String> imgItem = new HashMap<>();
                        imgItem.put("type", "image");
                        imgItem.put("tag", "img");
                        imgItem.put("content", src);
                        contents.add(imgItem);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Boardmix 크롤링 실패", e);
        } finally {
            driver.quit();
        }

        return contents;
    }
}
