package com.example.geoquiz_frontend.domain.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GameSession implements Serializable {
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
    private boolean isFinished;
    private boolean isSynced;
    private List<GameQuestion> questions;
    private int currentQuestionIndex;
    public GameSession(){}
    public GameSession(int mode, List<GameQuestion> questions) {
        this.id = UUID.randomUUID().toString();
        this.mode = mode;
        this.playedAt = new Date();
        this.questions = new ArrayList<>(questions);
        this.totalQuestions = questions.size();
        this.correctAnswers = 0;
        this.europeCorrect = 0;
        this.asiaCorrect = 0;
        this.africaCorrect = 0;
        this.americaCorrect = 0;
        this.oceaniaCorrect = 0;
        this.score = 0;
        this.timeSpent = 0;
        this.isOnline = false;
        this.isFinished = false;
        this.isSynced = false;
        this.currentQuestionIndex = 0;
    }

    public String getId() { return id; }
    public int getMode() { return mode; }
    public Date getPlayedAt() { return playedAt; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getScore() { return score; }
    public int getTimeSpent() { return timeSpent; }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }

    public boolean isOnline() { return isOnline; }
    public boolean isFinished() { return isFinished; }
    public boolean isSynced() { return isSynced; }

    public List<GameQuestion> getQuestions() { return questions; }

    public int getEuropeCorrect() { return europeCorrect; }
    public int getAsiaCorrect() { return asiaCorrect; }
    public int getAfricaCorrect() { return africaCorrect; }
    public int getAmericaCorrect() { return americaCorrect; }
    public int getOceaniaCorrect() { return oceaniaCorrect; }


    public void setId(String id) { this.id = id; }
    public void setMode(int mode) { this.mode = mode; }
    public void setPlayedAt(Date playedAt) { this.playedAt = playedAt; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public void setScore(int score) { this.score = score; }
    public void setTimeSpent(int timeSpent) { this.timeSpent = timeSpent; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }

    public void setOnline(boolean online) { isOnline = online; }
    public void setFinished(boolean finished) { isFinished = finished; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public void setQuestions(List<GameQuestion> questions) { this.questions = questions; }

    public void setEuropeCorrect(int europeCorrect) { this.europeCorrect = europeCorrect; }
    public void setAsiaCorrect(int asiaCorrect) { this.asiaCorrect = asiaCorrect; }
    public void setAfricaCorrect(int africaCorrect) { this.africaCorrect = africaCorrect; }
    public void setAmericaCorrect(int americaCorrect) { this.americaCorrect = americaCorrect; }
    public void setOceaniaCorrect(int oceaniaCorrect) { this.oceaniaCorrect = oceaniaCorrect; }
}