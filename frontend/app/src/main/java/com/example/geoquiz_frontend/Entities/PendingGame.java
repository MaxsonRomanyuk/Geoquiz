package com.example.geoquiz_frontend.Entities;

import java.io.Serializable;
import java.util.Date;

public class PendingGame implements Serializable {
    private String id;
    private int modeValue;
    private Date playedAt;
    private int totalQuestions;
    private int correctAnswers;
    private int score;
    private int timeSpent;
    private boolean isOnline;

    public PendingGame(GameSession session) {
        this.id = session.getId();
        this.modeValue = session.getMode().getValue();
        this.playedAt = session.getPlayedAt();
        this.totalQuestions = session.getTotalQuestions();
        this.correctAnswers = session.getCorrectAnswers();
        this.score = session.getScore();
        this.timeSpent = session.getTimeSpent();
        this.isOnline = session.isOnline();
    }

    public String getId() { return id; }
    public int getModeValue() { return modeValue; }
    public Date getPlayedAt() { return playedAt; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getScore() { return score; }
    public int getTimeSpent() { return timeSpent; }
    public boolean isOnline() { return isOnline; }
}