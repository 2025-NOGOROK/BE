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
public class TourApiService {

    @Value("${tour.api.service-key}")
    private String serviceKey;

    public String getLocationBasedList(int numOfRows, int pageNo, double mapX, double mapY) throws Exception {
        double deltaLat = 10.0 / 111.0;
        double deltaLon = 10.0 / (111.0 * Math.cos(Math.toRadians(mapY)));

        double gpsxfrom = mapX - deltaLon;
        double gpsxto = mapX + deltaLon;
        double gpsyfrom = mapY - deltaLat;
        double gpsyto = mapY + deltaLat;

        String apiUrl = "https://apis.data.go.kr/B553457/cultureinfo/period2";

        String urlStr = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("numOfrows", numOfRows)
                .queryParam("PageNo", pageNo)
                .queryParam("gpsxfrom", gpsxfrom)
                .queryParam("gpsxto", gpsxto)
                .queryParam("gpsyfrom", gpsyfrom)
                .queryParam("gpsyto", gpsyto)
                .queryParam("type", "json") // 무시되더라도 넣기
                .build()
                .toString();

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
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
