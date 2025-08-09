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

    /**
     * @param seq     상세 seq
     * @param currLon 현재 위치 경도 (mapX)
     * @param currLat 현재 위치 위도 (mapY)
     */
    public String getDetailInfo(String seq, Double currLon, Double currLat) throws Exception {
        String apiUrl = "https://apis.data.go.kr/B553457/cultureinfo/detail2";

        String urlStr = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("seq", seq)
                .build()
                .toString();

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json; charset=UTF-8");

        BufferedReader rd = (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300)
                ? new BufferedReader(new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))
                : new BufferedReader(new InputStreamReader(conn.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) sb.append(line);
        rd.close();
        conn.disconnect();

        // XML → JSON
        String xml = sb.toString();
        JSONObject root = XML.toJSONObject(xml);

        // 현재 위치가 들어온 경우만 distanceKm 주입
        if (currLat != null && currLon != null) {
            injectDistance(root, currLat, currLon); // 주의: (lat, lon) 순서
        }

        return root.toString(); // JSON 반환
    }

    /** 상세 응답 JSON에서 좌표 추출 → distanceKm / distanceText 주입 */
    private void injectDistance(JSONObject root, double currLat, double currLon) {
        // 구조 예: { response: { body: { items: { item: { gpsX/gpsx, gpsY/gpsy } } } } }
        JSONObject item = optObj(optObj(optObj(optObj(root, "response"), "body"), "items"), "item");
        if (item == null) return;

        // 경도/위도 키가 gpsX/gpsY 또는 gpsx/gpsy로 올 수 있으므로 모두 대응
        Double lon = firstNonNull(
                toDoubleSafe(item.opt("gpsX")),
                toDoubleSafe(item.opt("gpsx"))
        );
        Double lat = firstNonNull(
                toDoubleSafe(item.opt("gpsY")),
                toDoubleSafe(item.opt("gpsy"))
        );

        if (lat != null && lon != null) {
            double km = haversineKm(currLat, currLon, lat, lon);
            double rounded = round1(km);
            item.put("distanceKm", rounded);              // ✅ 먼저 숫자 필드 세팅
            item.put("distanceText", rounded + " km");    // ✅ 표시용 텍스트(옵션)
        } else {
            item.put("distanceKm", JSONObject.NULL);
            item.put("distanceText", JSONObject.NULL);
        }
    }

    /** Haversine 거리(km) */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0088; // km (mean Earth radius)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // ---- JSON 유틸 ----
    private static JSONObject optObj(JSONObject o, String k) {
        if (o == null) return null;
        Object v = o.opt(k);
        return (v instanceof JSONObject jo) ? jo : null;
    }

    private static Double firstNonNull(Double a, Double b) { return a != null ? a : b; }

    private static Double toDoubleSafe(Object v) {
        if (v == null || v == JSONObject.NULL) return null;
        try {
            if (v instanceof Number n) return n.doubleValue();
            String s = v.toString().trim();
            if (s.isEmpty()) return null;
            return Double.parseDouble(s);
        } catch (Exception e) { return null; }
    }

    private static double round1(double x) { return Math.round(x * 10.0) / 10.0; }
}
