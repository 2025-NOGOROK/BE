package com.example.Easeplan.api.Evaluation.dto;


import com.example.Easeplan.api.Evaluation.domain.IncompleteReason;

public class ScheduleFeedbackRequest {
    private Long scheduleId;
    private Boolean completed;
    private IncompleteReason reason; // Enum

    // getters/setters
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public IncompleteReason getReason() { return reason; }
    public void setReason(IncompleteReason reason) { this.reason = reason; }
}
