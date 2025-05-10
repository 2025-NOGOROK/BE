package com.example.Easeplan.api.Evaluation.service;

import com.example.Easeplan.api.Evaluation.domain.IncompleteReason;
import com.example.Easeplan.api.Evaluation.domain.ScheduleFeedback;
import com.example.Easeplan.api.Evaluation.dto.ScheduleFeedbackRequest;

import com.example.Easeplan.api.Evaluation.repository.ScheduleFeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleFeedbackService {

    private final ScheduleFeedbackRepository repository;

    public ScheduleFeedbackService(ScheduleFeedbackRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveFeedback(ScheduleFeedbackRequest request) {
        ScheduleFeedback feedback = new ScheduleFeedback();
        feedback.setScheduleId(request.getScheduleId());
        feedback.setCompleted(request.getCompleted());
        feedback.setReason(request.getReason());
        feedback.setFeedbackAt(LocalDateTime.now());
        repository.save(feedback);
    }

    public Map<IncompleteReason, Integer> getIncompleteReasonPercent(LocalDateTime start, LocalDateTime end) {
        List<Object[]> reasonCounts = repository.countByReason(start, end);
        long total = repository.countIncomplete(start, end);

        Map<IncompleteReason, Integer> result = new EnumMap<>(IncompleteReason.class);
        // Enum별로 0으로 초기화
        for (IncompleteReason reason : IncompleteReason.values()) {
            result.put(reason, 0);
        }
        for (Object[] row : reasonCounts) {
            IncompleteReason reason = (IncompleteReason) row[0];
            Long count = (Long) row[1];
            int percent = total == 0 ? 0 : (int) Math.round((double) count / total * 100);
            result.put(reason, percent);
        }
        return result;
    }
}
