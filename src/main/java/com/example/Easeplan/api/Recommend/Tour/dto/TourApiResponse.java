package com.example.Easeplan.api.Recommend.Tour.dto;

import lombok.Data;

import java.util.List;

@Data
public class TourApiResponse {
    private Response response;

    @Data
    public static class Response {
        private Header header;
        private Body body;
    }

    @Data
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data
    public static class Body {
        private int totalCount;
        private int pageNo;
        private int numOfrows;
        private Items items;
    }

    @Data
    public static class Items {
        private List<Item> item;
    }

    @Data
    public static class Item {
        private String serviceName;
        private long seq;
        private String title;
        private String startDate;
        private String endDate;
        private String place;
        private String realmName;
        private String area;
        private String sigungu;
        private String thumbnail;
        private double gpsX;
        private double gpsY;
    }
}
