package com.example.Easeplan.api.Recommend.Event.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Service
public class CultureEventService {

    @Value("${culture.api.service-key}")
    private String serviceKey; // 반드시 발급받은 서비스키로 변경

    // Service
    public String getEvents(String codename, String title, String date, int startIndex, int endIndex) throws Exception {
        // codename, title이 없으면 %20로 처리
        String codenamePath = (codename == null || codename.isEmpty()) ? "%20" : URLEncoder.encode(codename, "UTF-8");
        String titlePath = (title == null || title.isEmpty()) ? "%20" : URLEncoder.encode(title, "UTF-8");
        String datePath = (date == null || date.isEmpty()) ? "" : date;

        String apiUrl = String.format(
                "http://openapi.seoul.go.kr:8088/%s/xml/culturalEventInfo/%d/%d/%s/%s/%s",
                serviceKey, startIndex, endIndex, codenamePath, titlePath, datePath
        );

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl);

        if (codename != null && !codename.isEmpty()) {
            builder.queryParam("CODENAME", URLEncoder.encode(codename, "UTF-8"));
        }
        if (title != null && !title.isEmpty()) {
            builder.queryParam("TITLE", URLEncoder.encode(title, "UTF-8"));
        }
        if (date != null && !date.isEmpty()) {
            builder.queryParam("DATE", URLEncoder.encode(date, "UTF-8"));
        }

        URL url = new URL(builder.toUriString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/xml");

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        return sb.toString();
    }


}

