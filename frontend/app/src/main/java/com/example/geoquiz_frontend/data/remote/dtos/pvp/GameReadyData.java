package com.example.geoquiz_frontend.data.remote.dtos.pvp;

import java.util.List;

public class GameReadyData {
    private String matchId;
    private String selectedMode;
    private int totalQuestions;
    private int totalGameTimeSeconds;
    private String gameStartTime;
    private List<QuestionData> questions;
    private int questionSeed;
    private int difficultyLevel;

    public String getMatchId() { return matchId; }
    public String getSelectedMode() { return selectedMode; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getTotalGameTimeSeconds() { return totalGameTimeSeconds; }
    public String getGameStartTime() { return gameStartTime; }
    public List<QuestionData> getQuestions() { return questions; }
    public int getQuestionSeed() { return questionSeed; }
    public int getDifficultyLevel() { return difficultyLevel; }
}
