package com.example.Easeplan.api.MainPage.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class DynamicCrawlingService1 {

    public List<Map<String, String>> crawlTeenStressSectionWithImage() {
        List<Map<String, String>> result = new ArrayList<>();

        try {
            String url = "https://www.teenstress.co.kr/teen03";
            Document doc = Jsoup.connect(url).get();

            Element section = doc.selectFirst("section#stress_resolve");
            if (section == null) return result;

            Elements elements = section.select("*");
            String currentH3 = null;
            String currentH4 = null;
            String currentH5 = null;
            StringBuilder paragraphBuilder = new StringBuilder();
            List<String> imageUrls = new ArrayList<>();

            for (Element el : elements) {
                switch (el.tagName()) {
                    case "h3":
                        currentH3 = el.text();
                        break;
                    case "h4":
                        if (currentH4 != null && currentH5 != null && paragraphBuilder.length() > 0) {
                            Map<String, String> item = new LinkedHashMap<>();
                            item.put("h3", currentH3);
                            item.put("h4", currentH4);
                            item.put("h5", currentH5);
                            item.put("p", paragraphBuilder.toString().trim());
                            if (!imageUrls.isEmpty()) {
                                item.put("image", String.join(", ", imageUrls));
                            }
                            result.add(item);
                        }
                        currentH4 = el.text();
                        currentH5 = null;
                        paragraphBuilder.setLength(0);
                        imageUrls.clear();
                        break;

                    case "h5":
                        if (currentH5 != null && paragraphBuilder.length() > 0) {
                            Map<String, String> item = new LinkedHashMap<>();
                            item.put("h4", currentH4);
                            item.put("h5", currentH5);
                            item.put("p", paragraphBuilder.toString().trim());
                            if (!imageUrls.isEmpty()) {
                                item.put("image", String.join(", ", imageUrls));
                            }
                            result.add(item);
                        }
                        currentH5 = el.text();
                        paragraphBuilder.setLength(0);
                        imageUrls.clear();
                        break;

                    case "p":
                        paragraphBuilder.append(el.text()).append(" ");
                        break;

                    case "img":
                        String src = el.attr("src").trim();
                        if (!src.startsWith("http")) {
                            src = "https://www.teenstress.co.kr" + (src.startsWith("/") ? "" : "/") + src;
                        }
                        imageUrls.add(src);
                        break;
                }
            }

            // 마지막 블록 처리
            if (currentH4 != null && currentH5 != null && paragraphBuilder.length() > 0) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("h4", currentH4);
                item.put("h5", currentH5);
                item.put("p", paragraphBuilder.toString().trim());
                if (!imageUrls.isEmpty()) {
                    item.put("image", String.join(", ", imageUrls));
                }
                result.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
