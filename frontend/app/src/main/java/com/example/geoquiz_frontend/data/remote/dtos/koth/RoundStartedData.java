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

    @SerializedName("serverTime")
    private long serverTime;
    @SerializedName("roundEndAt")
    private long roundEndAt;

    public int getRoundNumber() { return roundNumber; }
    public int getRoundTypeValue() { return roundType; }
    public RoundType getRoundType() { return RoundType.fromValue(roundType); }
    public QuestionData getQuestion() { return question; }
    public long getServerTime() { return serverTime; }
    public long getRoundEndAt() { return roundEndAt; }
}