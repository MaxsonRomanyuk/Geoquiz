package com.example.geoquiz_frontend.data.remote.dtos.auth;

import com.example.geoquiz_frontend.domain.entities.UserStats;

public class UserStatsRequest {
    private int level;
    private int experience;
    private int gamesPlayed;
    private int gamesWon;
    private float winRate;
    private int score;
    private int dailyStreak;
    private int winStreak;
    private int europeCorrect;
    private int asiaCorrect;
    private int africaCorrect;
    private int americaCorrect;
    private int oceaniaCorrect;
    private String bestContinent;
    private int capitalsCorrect;
    private int flagsCorrect;
    private int outlinesCorrect;
    private int languagesCorrect;

    public UserStatsRequest(UserStats stats) {
        this.level = stats.getLevel();
        this.experience = stats.getExperience();
        this.gamesPlayed = stats.getGamesPlayed();
        this.gamesWon = stats.getGamesWon();
        this.winRate = stats.getWinRate();
        this.score = stats.getScore();
        this.dailyStreak = stats.getDailyStreak();
        this.winStreak = stats.getWinStreak();
        this.europeCorrect = stats.getEuropeCorrect();
        this.asiaCorrect = stats.getAsiaCorrect();
        this.africaCorrect = stats.getAfricaCorrect();
        this.americaCorrect = stats.getAmericaCorrect();
        this.oceaniaCorrect = stats.getOceaniaCorrect();
        this.bestContinent = stats.getBestContinent();
        this.capitalsCorrect = stats.getCapitalsCorrect();
        this.flagsCorrect = stats.getFlagsCorrect();
        this.outlinesCorrect = stats.getOutlinesCorrect();
        this.languagesCorrect = stats.getLanguagesCorrect();
    }

    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getGamesPlayed() { return gamesPlayed; }
    public int getGamesWon() { return gamesWon; }
    public float getWinRate() { return winRate; }
    public int getScore() { return score; }
    public int getDailyStreak() { return dailyStreak; }
    public int getWinStreak() { return winStreak; }
    public int getEuropeCorrect() { return europeCorrect; }
    public int getAsiaCorrect() { return asiaCorrect; }
    public int getAfricaCorrect() { return africaCorrect; }
    public int getAmericaCorrect() { return americaCorrect; }
    public int getOceaniaCorrect() { return oceaniaCorrect; }
    public String getBestContinent() { return bestContinent; }
    public int getCapitalsCorrect() { return capitalsCorrect; }
    public int getFlagsCorrect() { return flagsCorrect; }
    public int getOutlinesCorrect() { return outlinesCorrect; }
    public int getLanguagesCorrect() { return languagesCorrect; }
}
