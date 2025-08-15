package com.example.Easeplan.api.MainPage.service;

import com.example.Easeplan.api.MainPage.common.EmotionQuestion;
import com.example.Easeplan.api.MainPage.common.StressSeverity;
import com.example.Easeplan.api.MainPage.dto.request.QuizAnswerRequest;
import com.example.Easeplan.api.MainPage.dto.request.QuizSubmitRequest;
import com.example.Easeplan.api.MainPage.dto.response.QuizSubmitResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class QuizService {

    @Transactional
    public QuizSubmitResponse submit(QuizSubmitRequest req){
        List<QuizAnswerRequest> items = Objects.requireNonNull(req.answers(), "answers is required");

        if(items.size()!= EmotionQuestion.values().length){
            throw new IllegalArgumentException("All 7 answers are required");
        }

        Set<EmotionQuestion> seen= EnumSet.noneOf(EmotionQuestion.class);
        int total=0;
        Map<String,Integer> perItem=new LinkedHashMap<>();

        for(QuizAnswerRequest a : items){
            EmotionQuestion q=EmotionQuestion.valueOf(a.questionCode());
            if(!seen.add(q)){
                throw new IllegalArgumentException("Duplicate answer: " +q.name());
            }
            int score=a.answer().score();
            total+=score;
            perItem.put(q.name(), score);
        }
        String severity = StressSeverity.ofTotal(total).name();
        return new QuizSubmitResponse(total, severity, perItem);
    }
}
