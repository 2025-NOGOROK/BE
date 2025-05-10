package com.example.Easeplan.api.Evaluation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_feedback")
public class ScheduleFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private Boolean completed;

    @Enumerated(EnumType.STRING)
    private IncompleteReason reason; // Enum

    @Column(name = "feedback_at", nullable = false)
    private LocalDateTime feedbackAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public IncompleteReason getReason() { return reason; }
    public void setReason(IncompleteReason reason) { this.reason = reason; }

    public LocalDateTime getFeedbackAt() { return feedbackAt; }
    public void setFeedbackAt(LocalDateTime feedbackAt) { this.feedbackAt = feedbackAt; }
}
