package com.example.geoquiz_frontend.domain.entities;

public class UserStats {
    private String userId;
    private int gamesPlayed;
    private int gamesWon;
    private float winRate;
    private int level;
    private int experience;
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

    public UserStats(String userId) {
        this.userId = userId;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.winRate = 0;
        this.level = 1;
        this.experience = 0;
        this.dailyStreak = 0;
        this.winStreak = 0;
        this.europeCorrect = 0;
        this.asiaCorrect = 0;
        this.africaCorrect = 0;
        this.americaCorrect = 0;
        this.oceaniaCorrect = 0;
        this.bestContinent = "Европа";
        this.capitalsCorrect = 0;
        this.flagsCorrect = 0;
        this.outlinesCorrect = 0;
        this.languagesCorrect = 0;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public int getGamesWon() { return gamesWon; }
    public void setGamesWon(int gamesWon) { this.gamesWon = gamesWon; }

    public float getWinRate() { return winRate; }
    public void setWinRate(float winRate) { this.winRate = winRate; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getDailyStreak() { return dailyStreak; }
    public void setDailyStreak(int dailyStreak) { this.dailyStreak = dailyStreak; }

    public int getWinStreak() { return winStreak; }
    public void setWinStreak(int winStreak) { this.winStreak = winStreak; }

    public int getEuropeCorrect() { return europeCorrect; }
    public void setEuropeCorrect(int europeCorrect) { this.europeCorrect = europeCorrect; }

    public int getAsiaCorrect() { return asiaCorrect; }
    public void setAsiaCorrect(int asiaCorrect) { this.asiaCorrect = asiaCorrect; }

    public int getAfricaCorrect() { return africaCorrect; }
    public void setAfricaCorrect(int africaCorrect) { this.africaCorrect = africaCorrect; }

    public int getAmericaCorrect() { return americaCorrect; }
    public void setAmericaCorrect(int americaCorrect) { this.americaCorrect = americaCorrect; }

    public int getOceaniaCorrect() { return oceaniaCorrect; }
    public void setOceaniaCorrect(int oceaniaCorrect) { this.oceaniaCorrect = oceaniaCorrect; }

    public String getBestContinent() { return bestContinent; }
    public void setBestContinent(String bestContinent) { this.bestContinent = bestContinent; }

    public int getCapitalsCorrect() { return capitalsCorrect; }
    public void setCapitalsCorrect(int capitalsCorrect) { this.capitalsCorrect = capitalsCorrect; }

    public int getFlagsCorrect() { return flagsCorrect; }
    public void setFlagsCorrect(int flagsCorrect) { this.flagsCorrect = flagsCorrect; }

    public int getOutlinesCorrect() { return outlinesCorrect; }
    public void setOutlinesCorrect(int outlinesCorrect) { this.outlinesCorrect = outlinesCorrect; }

    public int getLanguagesCorrect() { return languagesCorrect; }
    public void setLanguagesCorrect(int languagesCorrect) { this.languagesCorrect = languagesCorrect; }
}
