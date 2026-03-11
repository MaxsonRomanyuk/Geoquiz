package com.example.geoquiz_frontend.data.remote.dtos.koth;

public class PlayerRoundResult {
    private String playerId;
    private boolean hasAnswered;
    private boolean isCorrect;
    private int timeSpentMs;
    private int scoreGained;

    public String getPlayerId() { return playerId; }
    public boolean isHasAnswered() { return hasAnswered; }
    public boolean isCorrect() { return isCorrect; }
    public int getTimeSpentMs() { return timeSpentMs; }
    public int getScoreGained() { return scoreGained; }
}
