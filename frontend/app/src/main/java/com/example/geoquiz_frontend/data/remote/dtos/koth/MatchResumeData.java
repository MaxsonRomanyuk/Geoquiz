package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.google.gson.annotations.SerializedName;

public class MatchResumeData {
    @SerializedName("matchId")
    private String matchId;
    @SerializedName("roundStartedData")
    private RoundStartedData roundStartedData;

    @SerializedName("currentScore")
    private int currentScore;

    @SerializedName("totalPlayers")
    private int totalPlayers;

    @SerializedName("playersLeft")
    private int playersLeft;
    public String getMatchId() { return matchId; }
    public RoundStartedData getRoundStartedData() { return roundStartedData; }
    public int getCurrentScore() { return currentScore; }
    public int getTotalPlayers() { return totalPlayers; }
    public int getPlayersLeft() { return playersLeft; }
}
