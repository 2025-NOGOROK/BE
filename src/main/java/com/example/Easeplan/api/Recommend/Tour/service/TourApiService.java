package com.example.Easeplan.api.Recommend.Tour.service;

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

    public String getLocationBasedList(
            int numOfRows, int pageNo, String mobileOS, String mobileApp,
            double mapX, double mapY, int radius, String type) throws Exception {

        String apiUrl = "http://apis.data.go.kr/B551011/KorService1/locationBasedList1";
        String urlStr = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", mobileOS)
                .queryParam("MobileApp", mobileApp)
                .queryParam("mapX", mapX)
                .queryParam("mapY", mapY)
                .queryParam("radius", radius)
                .queryParam("_type", type) // "json" 또는 "xml"
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

        return sb.toString();
    }
}
