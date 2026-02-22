package com.example.geoquiz_frontend.Entities;

import java.io.Serializable;
import java.util.Date;

public class PendingGame implements Serializable {
    private String id;
    private int mode;
    private Date playedAt;
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

    public PendingGame(GameSession session) {
        this.id = session.getId();
        this.mode = session.getMode();
        this.playedAt = session.getPlayedAt();
        this.totalQuestions = session.getTotalQuestions();
        this.correctAnswers = session.getCorrectAnswers();
        this.europeCorrect = session.getEuropeCorrect();
        this.asiaCorrect = session.getAsiaCorrect();
        this.africaCorrect = session.getAfricaCorrect();
        this.americaCorrect = session.getAmericaCorrect();
        this.oceaniaCorrect = session.getOceaniaCorrect();
        this.score = session.getScore();
        this.timeSpent = session.getTimeSpent();
        this.isOnline = session.isOnline();
    }

    public String getId() { return id; }
    public int getModeValue() { return mode; }
    public Date getPlayedAt() { return playedAt; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getEuropeCorrect() {return  europeCorrect; }
    public int getAsiaCorrect() {return  asiaCorrect; }
    public int getAfricaCorrect() {return  africaCorrect; }
    public int getAmericaCorrect() {return  americaCorrect; }
    public int getOceaniaCorrect() {return  oceaniaCorrect; }
    public int getScore() { return score; }
    public int getTimeSpent() { return timeSpent; }
    public boolean isOnline() { return isOnline; }
}