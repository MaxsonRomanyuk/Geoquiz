package com.example.geoquiz_frontend.data.remote.dtos.koth;

public class PlayerEliminatedData {
    private String playerId;
    private int roundsSurvived;
    private int place;
    private int correctAnswers;
    private int totalScore;

    public String getPlayerId() { return playerId; }
    public int getRoundsSurvived() { return roundsSurvived; }
    public int getPlace() { return place; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getTotalScore() { return totalScore; }
}
