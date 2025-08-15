package com.example.Easeplan.api.MainPage.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuizSubmitRequest (
        @NotNull
        List<QuizAnswerRequest> answers
) {

        }