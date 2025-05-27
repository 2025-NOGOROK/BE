package com.example.Easeplan.api.Recommend.Long.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)

public class CulturalEventInfoResponse {
    //공공데이터(OpenAPI)에서 실제로 쓰는 응답 필드명
    private List<Event> row;
}
