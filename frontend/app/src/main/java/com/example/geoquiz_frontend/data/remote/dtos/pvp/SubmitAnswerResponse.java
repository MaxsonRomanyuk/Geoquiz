package com.example.geoquiz_frontend.data.remote.dtos.pvp;

public class SubmitAnswerResponse {
    private boolean isCorrect;
    private int correctOptionIndex;
    private int questionNumber;
    private int yourScore;
    private int opponentScore;
    private int yourAnswered;
    private int opponentAnswered;
    public SubmitAnswerResponse() {
    }

    public SubmitAnswerResponse(boolean isCorrect, int correctOptionIndex, int questionNumber,
                                int yourScore, int opponentScore,
                                int yourAnswered, int opponentAnswered) {
        this.isCorrect = isCorrect;
        this.correctOptionIndex = correctOptionIndex;
        this.questionNumber = questionNumber;
        this.yourScore = yourScore;
        this.opponentScore = opponentScore;
        this.yourAnswered = yourAnswered;
        this.opponentAnswered = opponentAnswered;
    }

    public boolean isCorrect() {
        return isCorrect;
    }
    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }
    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }
    public int getQuestionNumber(){
        return questionNumber;
    }
    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }
    public int getYourScore() {
        return yourScore;
    }
    public void setYourScore(int yourScore) {
        this.yourScore = yourScore;
    }
    public int getOpponentScore() {
        return opponentScore;
    }
    public void setOpponentScore(int opponentScore) {
        this.opponentScore = opponentScore;
    }

    public int getYourAnswered() {
        return yourAnswered;
    }
    public void setYourAnswered(int yourAnswered) {
        this.yourAnswered = yourAnswered;
    }

    public int getOpponentAnswered() {
        return opponentAnswered;
    }
    public void setOpponentAnswered(int opponentAnswered) {
        this.opponentAnswered = opponentAnswered;
    }

}
