package com.example.Easeplan.api.Recommend.Long.RecommendationResult;
//Flask(파이썬) 추천 서버에서 받은 추천 결과 JSON을
//Java(Spring)에서 객체로 바꿔서 쉽게 다루기 위한 클래스
public class RecommendationResult {
    private String title;
    private String label;        // 장르(분류)
    private String description;  // 공연 설명



    public RecommendationResult(String title, String label, String description) {
        this.title = title;
        this.label = label;
        this.description = description;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
