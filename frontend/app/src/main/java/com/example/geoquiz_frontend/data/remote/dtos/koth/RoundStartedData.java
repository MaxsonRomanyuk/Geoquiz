package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.example.geoquiz_frontend.domain.enums.RoundType;

public class RoundStartedData {
    private int roundNumber;
    private int roundType;
    private QuestionData question;

    private String roundStartTime;

    private int timeLimitSeconds;

    public int getRoundNumber() { return roundNumber; }
    public int getRoundTypeValue() { return roundType; }
    public RoundType getRoundType() { return RoundType.fromValue(roundType); }
    public QuestionData getQuestion() { return question; }
    public String getRoundStartTime() { return roundStartTime; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
}
