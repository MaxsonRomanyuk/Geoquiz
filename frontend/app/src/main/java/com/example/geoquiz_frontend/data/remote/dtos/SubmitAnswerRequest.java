package com.example.geoquiz_frontend.data.remote.dtos;

public class SubmitAnswerRequest {
    private String matchId;
    private String questionId;
    private int selectedIndex;
    private int timeSpentMs;
    private int questionNumber;

    public SubmitAnswerRequest(String matchId, String questionId,
                               int selectedIndex, int timeSpentMs, int questionNumber) {
        this.matchId = matchId;
        this.questionId = questionId;
        this.selectedIndex = selectedIndex;
        this.timeSpentMs = timeSpentMs;
        this.questionNumber = questionNumber;
    }

    public String getMatchId() { return matchId; }
    public String getQuestionId() { return questionId; }
    public int getSelectedIndex() { return selectedIndex; }
    public int getTimeSpentMs() { return timeSpentMs; }
    public int getQuestionNumber() { return questionNumber; }
}