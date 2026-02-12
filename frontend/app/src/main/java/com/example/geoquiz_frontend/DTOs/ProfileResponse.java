package com.example.geoquiz_frontend.DTOs;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

public class ProfileResponse {
    private UserDto user;
    private StatsDto stats;
    private GeographyDto geography;
    private GameModesDto gameModes;
    private PvPDto pvp;
    private List<AchievementDto> achievements;

    // Getters and Setters
    public UserDto getUser() { return user; }
    public StatsDto getStats() { return stats; }
    public GeographyDto getGeography() { return geography; }
    public GameModesDto getGameModes() { return gameModes; }
    public PvPDto getPvp() { return pvp; }
    public List<AchievementDto> getAchievements() { return achievements; }

    public static class UserDto {
        private String id;
        private String userName;
        private String email;
        private Date registeredAt;

        public String getUserName() { return userName; }
        public String getEmail() { return email; }
    }

    public static class StatsDto {
        private int totalGamesPlayed;
        private int totalGamesWon;
        private double winRate;
        private int level;
        private int experience;
        private int dailyLoginStreak;
        private int totalCorrectAnswers;
        private int currentWinStreak;
        private int maxWinStreak;

        public int getGamesPlayed() { return totalGamesPlayed; }
        public int getWins() { return totalGamesWon; }
        public double getWinRate() { return winRate; }
        public int getLevel() { return level; }
        public int getExperience() { return experience; }
        public int getDailyStreak() { return dailyLoginStreak; }
        public int getTotalCorrectAnswers() { return totalCorrectAnswers; }
    }

    public static class GeographyDto {
        private int europeCorrect;
        private int asiaCorrect;
        private int africaCorrect;
        private int americaCorrect;
        private int oceaniaCorrect;

        public String getBestContinent() {
            int[] values = {europeCorrect, asiaCorrect, africaCorrect, americaCorrect, oceaniaCorrect};
            String[] continents = {"Европа", "Азия", "Африка", "Америка", "Океания"};

            int maxIndex = 0;
            for (int i = 1; i < values.length; i++) {
                if (values[i] > values[maxIndex]) maxIndex = i;
            }
            return continents[maxIndex];
        }
    }

    public static class GameModesDto {
        private int flagsCorrect;
        private int capitalsCorrect;
        private int outlinesCorrect;
        private int languagesCorrect;
    }

    public static class PvPDto {
        private int pvpGamesPlayed;
        private int pvpGamesWon;
        private double winRate;
        private int currentPvpStreak;
    }

    public static class AchievementDto {
        private String code;
        private String title;
        private String icon;
        private int progress;
        private Date unlockedAt;
    }
}
