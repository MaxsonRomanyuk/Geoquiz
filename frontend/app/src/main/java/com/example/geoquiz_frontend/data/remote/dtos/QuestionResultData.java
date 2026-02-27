package com.example.geoquiz_frontend.data.remote.dtos;

public class QuestionResultData {
    private String matchId;
    private int questionNumber;
    private int correctOptionIndex;
    private PlayerRoundResult yourResult;
    private PlayerRoundResult opponentResult;
    private int yourTotalScore;
    private int opponentTotalScore;
    private int yourCorrectCount;
    private int opponentCorrectCount;
    private int remainingTimeSeconds;
    private boolean isLastQuestion;

    public String getMatchId() { return matchId; }
    public int getQuestionNumber() { return questionNumber; }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public PlayerRoundResult getYourResult() { return yourResult; }
    public PlayerRoundResult getOpponentResult() { return opponentResult; }
    public int getYourTotalScore() { return yourTotalScore; }
    public int getOpponentTotalScore() { return opponentTotalScore; }
    public int getYourCorrectCount() { return yourCorrectCount; }
    public int getOpponentCorrectCount() { return opponentCorrectCount; }
    public int getRemainingTimeSeconds() { return remainingTimeSeconds; }
    public boolean isLastQuestion() { return isLastQuestion; }
}
