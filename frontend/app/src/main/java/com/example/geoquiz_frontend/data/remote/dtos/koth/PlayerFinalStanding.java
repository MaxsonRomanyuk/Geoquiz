package com.example.geoquiz_frontend.data.remote.dtos.koth;

public class PlayerFinalStanding {
    private String playerId;

    private String playerName;

    private int place;

    private int correctAnswers;

    private int totalScore;

    private int roundsSurvived;

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getPlace() { return place; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getTotalScore() { return totalScore; }
    public int getRoundsSurvived() { return roundsSurvived; }
}
