package com.example.Easeplan.api.Report.Month.dto;

import java.util.List;

public class DailyStressResponse {
    private List<DailyStress> dailyStressList;

    public DailyStressResponse(List<DailyStress> dailyStressList) {
        this.dailyStressList = dailyStressList;
    }

    public List<DailyStress> getDailyStressList() {
        return dailyStressList;
    }

    public static class DailyStress {
        private String date;   // yyyy-MM-dd
        private double avg;    // 일별 평균 스트레스
        private String emoji;  // 😄, 🙂, 😐, 😟, 😫

        public DailyStress(String date, double avg, String emoji) {
            this.date = date;
            this.avg = avg;
            this.emoji = emoji;
        }

        public String getDate() { return date; }
        public double getAvg() { return avg; }
        public String getEmoji() { return emoji; }
    }
}
