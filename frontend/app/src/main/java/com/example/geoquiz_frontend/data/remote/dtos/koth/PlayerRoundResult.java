package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.google.gson.annotations.SerializedName;

public class PlayerRoundResult {
    @SerializedName("playerId")
    private String playerId;
    @SerializedName("hasAnswered")
    private boolean hasAnswered;
    @SerializedName("isCorrect")
    private boolean isCorrect;
    @SerializedName("timeSpentMs")
    private int timeSpentMs;
    @SerializedName("scoreGained")
    private int scoreGained;

    public String getPlayerId() { return playerId; }
    public boolean isHasAnswered() { return hasAnswered; }
    public boolean isCorrect() { return isCorrect; }
    public int getTimeSpentMs() { return timeSpentMs; }
    public int getScoreGained() { return scoreGained; }
}
