package com.example.Easeplan.api.MainPage.common;

public enum AnswerOption {
    NEVER(0),
    RARELY(1),
    OFTEN(2),
    ALWAYS(3);

    private final int score;
    AnswerOption(int score) {this.score=score;}
    public int score(){
        return score;
    }
}
