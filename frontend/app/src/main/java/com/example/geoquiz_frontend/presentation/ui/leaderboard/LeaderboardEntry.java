package com.example.geoquiz_frontend.presentation.ui.leaderboard;

public class LeaderboardEntry {
    private String playerId;
    private String playerName;
    private int rank;
    private int level;
    private int totalScore;

    public LeaderboardEntry(String playerId, String playerName, int rank, int level, int totalScore) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.rank = rank;
        this.level = level;
        this.totalScore = totalScore;
    }

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getRank() { return rank; }
    public int getLevel() { return level; }
    public int getTotalScore() { return totalScore; }
}