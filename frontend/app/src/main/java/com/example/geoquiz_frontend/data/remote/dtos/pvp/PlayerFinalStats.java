package com.example.geoquiz_frontend.data.remote.dtos.pvp;

public class PlayerFinalStats {
    private String userId;
    private int finalScore;
    private int correctAnswers;
    private int totalQuestionsAnswered;
    private double averageAnswerTimeMs;

    public String getUserId() { return userId; }
    public int getFinalScore() { return finalScore; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getTotalQuestionsAnswered() { return totalQuestionsAnswered; }
    public double getAverageAnswerTimeMs() { return averageAnswerTimeMs; }
}