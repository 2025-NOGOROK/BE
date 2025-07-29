package com.example.Easeplan.api.Recommend.Tour.service;

import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class TourDetailService {

    @Value("${tour.api.service-key}")
    private String serviceKey;

    public String getDetailInfo(String seq) throws Exception {
        String apiUrl = "https://apis.data.go.kr/B553457/cultureinfo/detail2";

        String urlStr = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("seq", seq)
                .queryParam("type", "json")
                .build()
                .toString();

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

        // XML → JSON 변환
        String xml = sb.toString();
        JSONObject jsonObject = XML.toJSONObject(xml);

        return jsonObject.toString(); // JSON 형식으로 반환
    }
}
