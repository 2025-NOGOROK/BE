package com.example.Easeplan.api.Calendar.dto;

import com.google.api.client.util.DateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TimeSlot {
    private final DateTime start;
    private final DateTime end;


    public DateTime getStart() { return start; }
    public DateTime getEnd() { return end; }
}
