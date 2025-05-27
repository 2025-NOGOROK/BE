package com.example.Easeplan.api.Recommend.Long.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class Event {
    @JsonProperty("CODENAME")
    private String codename;
    @JsonProperty("TITLE")
    private String title;
    @JsonProperty("PLACE")
    private String place;
    @JsonProperty("STRTDATE")
    private String strtdate;
    @JsonProperty("END_DATE")
    private String endDate;
}
