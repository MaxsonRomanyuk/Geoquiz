package com.example.geoquiz_frontend.data.remote.dtos;

public class PlayerRoundResult {
    private boolean hasAnswered;
    private boolean isCorrect;
    private int timeSpentMs;
    private int scoreGained;

    public boolean hasAnswered() { return hasAnswered; }
    public boolean isCorrect() { return isCorrect; }
    public int getTimeSpentMs() { return timeSpentMs; }
    public int getScoreGained() { return scoreGained; }
}