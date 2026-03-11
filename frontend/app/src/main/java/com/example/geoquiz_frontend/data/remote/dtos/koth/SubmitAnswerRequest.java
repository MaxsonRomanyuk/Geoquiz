package com.example.geoquiz_frontend.data.remote.dtos.koth;

public class SubmitAnswerRequest {
    private String matchId;
    private int roundNumber;
    private String questionId;
    private int selectedOptionIndex;
    private int timeSpentMs;

    public SubmitAnswerRequest(String matchId, int roundNumber, String questionId,
                               int selectedOptionIndex, int timeSpentMs) {
        this.matchId = matchId;
        this.roundNumber = roundNumber;
        this.questionId = questionId;
        this.selectedOptionIndex = selectedOptionIndex;
        this.timeSpentMs = timeSpentMs;
    }
}
