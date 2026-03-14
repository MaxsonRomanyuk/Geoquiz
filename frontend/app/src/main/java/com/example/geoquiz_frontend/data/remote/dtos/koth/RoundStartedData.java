package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.example.geoquiz_frontend.domain.enums.RoundType;
import com.google.gson.annotations.SerializedName;

public class RoundStartedData {

    @SerializedName("roundNumber")
    private int roundNumber;

    @SerializedName("roundType")
    private int roundType;

    @SerializedName("question")
    private QuestionData question;

    @SerializedName("roundStartTime")
    private String roundStartTime;

    @SerializedName("timeLimitSeconds")
    private int timeLimitSeconds;

    public int getRoundNumber() { return roundNumber; }
    public int getRoundTypeValue() { return roundType; }
    public RoundType getRoundType() { return RoundType.fromValue(roundType); }
    public QuestionData getQuestion() { return question; }
    public String getRoundStartTime() { return roundStartTime; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
}