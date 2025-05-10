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

    public String getEvents(String dtype, String title, int numOfRows, int pageNo) throws Exception {
        String apiUrl = "http://api.kcisa.kr/openapi/CNV_060/request";
        String urlStr = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("serviceKey", URLEncoder.encode(serviceKey, "UTF-8"))
                .queryParam("dtype", URLEncoder.encode(dtype, "UTF-8"))
                .queryParam("title", URLEncoder.encode(title, "UTF-8"))
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .build(false) // 이미 인코딩했으므로 false
                .toUriString();

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

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

        return sb.toString(); // 그대로 반환 (JSON 또는 XML)
    }
}
