package com.example.geoquiz_frontend.Entities;

import com.example.geoquiz_frontend.Enums.GameMode;
import com.example.geoquiz_frontend.UI.Home.MainActivity;

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
    private int score;
    private int timeSpent;
    private boolean isOnline;
    private boolean isFinished;
    private boolean isSynced;
    private List<GameQuestion> questions;
    private int currentQuestionIndex;

    public GameSession(int mode, List<GameQuestion> questions) {
        this.id = UUID.randomUUID().toString();
        this.mode = mode;
        this.playedAt = new Date();
        this.questions = new ArrayList<>(questions);
        this.totalQuestions = questions.size();
        this.correctAnswers = 0;
        this.score = 0;
        this.timeSpent = 0;
        this.isOnline = false;
        this.isFinished = false;
        this.isSynced = false;
        this.currentQuestionIndex = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode; }

    public Date getPlayedAt() { return playedAt; }
    public void setPlayedAt(Date playedAt) { this.playedAt = playedAt; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTimeSpent() { return timeSpent; }
    public void setTimeSpent(int timeSpent) { this.timeSpent = timeSpent; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public boolean isFinished() { return isFinished; }
    public void setFinished(boolean finished) { isFinished = finished; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public List<GameQuestion> getQuestions() { return questions; }
    public void setQuestions(List<GameQuestion> questions) { this.questions = questions; }

    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

}