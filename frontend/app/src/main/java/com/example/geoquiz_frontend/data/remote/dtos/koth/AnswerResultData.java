package com.example.geoquiz_frontend.data.remote.dtos.koth;

public class AnswerResultData {
    private boolean isCorrect;
    private int scoreGained;
    private int timeSpentMs;
    private int remainingPlayers;
    private int correctOptionIndex;

    public boolean isCorrect() { return isCorrect; }
    public int getScoreGained() { return scoreGained; }
    public int getTimeSpentMs() { return timeSpentMs; }
    public int getRemainingPlayers() { return remainingPlayers; }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
}
