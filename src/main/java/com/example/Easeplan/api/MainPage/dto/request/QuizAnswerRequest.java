package com.example.Easeplan.api.MainPage.dto.request;

import com.example.Easeplan.api.MainPage.common.AnswerOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuizAnswerRequest (
        @NotBlank
        String questionCode,
        @NotNull
        AnswerOption answer
)

{

        }
