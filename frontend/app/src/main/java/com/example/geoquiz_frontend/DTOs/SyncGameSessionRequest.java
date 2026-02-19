package com.example.geoquiz_frontend.DTOs;

import java.util.Date;
import java.util.UUID;

public class SyncGameSessionRequest {
    private UUID id;
    private int mode;
    private int totalQuestions;
    private int correctAnswers;
    private int europeCorrect;
    private int asiaCorrect;
    private int africaCorrect;
    private int americaCorrect;
    private int oceaniaCorrect;
    private int score;
    private int timeSpent;
    private boolean isOnline;
    private Date playedAt;

    public SyncGameSessionRequest(UUID id, int mode, int totalQuestions, int correctAnswers,
                                  int europeCorrect, int asiaCorrect, int africaCorrect, int americaCorrect, int oceaniaCorrect,
                                  int score, int timeSpent, boolean isOnline, Date playedAt) {
        this.id = id;
        this.mode = mode;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.europeCorrect = europeCorrect;
        this.asiaCorrect = asiaCorrect;
        this.africaCorrect = africaCorrect;
        this.americaCorrect = americaCorrect;
        this.oceaniaCorrect = oceaniaCorrect;
        this.score = score;
        this.timeSpent = timeSpent;
        this.isOnline = isOnline;
        this.playedAt = playedAt;
    }


    public UUID getId() { return id; }
    public int getMode() { return mode; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getScore() { return score; }
    public int getTimeSpent() { return timeSpent; }
    public boolean isOnline() { return isOnline; }
    public Date getPlayedAt() { return playedAt; }
}