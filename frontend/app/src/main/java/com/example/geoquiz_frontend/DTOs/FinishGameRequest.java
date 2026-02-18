package com.example.geoquiz_frontend.DTOs;

public class FinishGameRequest {
    private int mode;
    private int totalQuestions;
    private int correctAnswers;
    private int score;
    private int timeSpent;
    private boolean isOnline;

    public FinishGameRequest(int mode, int totalQuestions, int correctAnswers,
                             int score, int timeSpent, boolean isOnline) {
        this.mode = mode;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.score = score;
        this.timeSpent = timeSpent;
        this.isOnline = isOnline;
    }

    public int getMode() { return mode; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getScore() { return score; }
    public int getTimeSpent() { return timeSpent; }
    public boolean isOnline() { return isOnline; }
}