package com.example.geoquiz_frontend.data.remote.dtos.history;

import com.google.gson.annotations.SerializedName;

public class GameSessionDto {
    @SerializedName("matchId")
    private String matchId;

    @SerializedName("gameType")
    private int gameType;

    @SerializedName("playedAt")
    private long playedAt;

    @SerializedName("totalScore")
    private int totalScore;

    @SerializedName("correctAnswers")
    private int correctAnswers;

    @SerializedName("totalQuestions")
    private int totalQuestions;

    @SerializedName("gameMode")
    private int gameMode;

    @SerializedName("isWin")
    private boolean isWin;

    @SerializedName("place")
    private int place;

    @SerializedName("roundsSurvived")
    private int roundsSurvived;

    @SerializedName("experienceGained")
    private int experienceGained;

    public String getMatchId() { return matchId; }
    public int getGameType() { return gameType; }
    public long getPlayedAt() { return playedAt; }
    public int getTotalScore() { return totalScore; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getGameMode() { return gameMode; }
    public boolean isWin() { return isWin; }
    public int getPlace() { return place; }
    public int getRoundsSurvived() { return roundsSurvived; }
    public int getExperienceGained() { return experienceGained; }
}