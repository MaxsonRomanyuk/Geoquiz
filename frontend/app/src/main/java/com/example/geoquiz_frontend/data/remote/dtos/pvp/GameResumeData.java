package com.example.geoquiz_frontend.data.remote.dtos.pvp;

import com.google.gson.annotations.SerializedName;

public class GameResumeData {

    @SerializedName("gameData")
    private GameReadyData gameData = new GameReadyData();

    @SerializedName("opponentName")
    private String opponentName;

    @SerializedName("opponentTotalScore")
    private int opponentTotalScore;

    @SerializedName("yourTotalScore")
    private int yourTotalScore;

    @SerializedName("opponentCurrentScore")
    private int opponentCurrentScore;

    @SerializedName("yourCurrentScore")
    private int yourCurrentScore;

    @SerializedName("currentQuestion")
    private int currentQuestion;

    @SerializedName("timerEndAt")
    private long timerEndAt;

    @SerializedName("serverTime")
    private long serverTime;

    public GameResumeData() {}

    public GameResumeData(GameReadyData gameData, String opponentName, int opponentTotalScore,
                          int yourTotalScore, int opponentCurrentScore, int yourCurrentScore,
                          int currentQuestion, long timerEndAt, long serverTime) {
        this.gameData = gameData;
        this.opponentName = opponentName;
        this.opponentTotalScore = opponentTotalScore;
        this.yourTotalScore = yourTotalScore;
        this.opponentCurrentScore = opponentCurrentScore;
        this.yourCurrentScore = yourCurrentScore;
        this.currentQuestion = currentQuestion;
        this.timerEndAt = timerEndAt;
        this.serverTime = serverTime;
    }

    public GameReadyData getGameData() {
        return gameData;
    }

    public void setGameData(GameReadyData gameData) {
        this.gameData = gameData;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public int getOpponentTotalScore() {
        return opponentTotalScore;
    }

    public void setOpponentTotalScore(int opponentTotalScore) {
        this.opponentTotalScore = opponentTotalScore;
    }

    public int getYourTotalScore() {
        return yourTotalScore;
    }

    public void setYourTotalScore(int yourTotalScore) {
        this.yourTotalScore = yourTotalScore;
    }

    public int getOpponentCurrentScore() {
        return opponentCurrentScore;
    }

    public void setOpponentCurrentScore(int opponentCurrentScore) {
        this.opponentCurrentScore = opponentCurrentScore;
    }

    public int getYourCurrentScore() {
        return yourCurrentScore;
    }

    public void setYourCurrentScore(int yourCurrentScore) {
        this.yourCurrentScore = yourCurrentScore;
    }

    public int getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(int currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public long getTimerEndAt() {
        return timerEndAt;
    }

    public void setTimerEndAt(long timerEndAt) {
        this.timerEndAt = timerEndAt;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }
}
