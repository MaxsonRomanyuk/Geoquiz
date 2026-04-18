package com.example.geoquiz_frontend.data.remote.dtos.profile;

import java.util.Date;
import java.util.List;

public class ProfileResponse {
    private UserDto user;
    private StatsDto stats;
    private GeographyDto geography;
    private GameModesDto gameModes;
    private PvPDto pvp;
    private List<AchievementDto> achievements;

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public StatsDto getStats() { return stats; }
    public void setStats(StatsDto stats) { this.stats = stats; }

    public GeographyDto getGeography() { return geography; }
    public void setGeography(GeographyDto geography) { this.geography = geography; }

    public GameModesDto getGameModes() { return gameModes; }
    public void setGameModes(GameModesDto gameModes) { this.gameModes = gameModes; }

    public PvPDto getPvp() { return pvp; }
    public void setPvp(PvPDto pvp) { this.pvp = pvp; }

    public List<AchievementDto> getAchievements() { return achievements; }
    public void setAchievements(List<AchievementDto> achievements) { this.achievements = achievements; }

    public static class UserDto {
        private String id;
        private String userName;
        private String email;
        private Date registeredAt;

        public String getId() { return id; }
        public String getUserName() { return userName; }
        public String getEmail() { return email; }
        public Date getRegisteredAt() { return registeredAt; }

        public void setId(String id) { this.id = id; }
        public void setUserName(String userName) { this.userName = userName; }
        public void setEmail(String email) { this.email = email; }
        public void setRegisteredAt(Date registeredAt) { this.registeredAt = registeredAt; }
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
        public int getCurrentWinStreak() { return currentWinStreak; }
        public int getMaxWinStreak() { return maxWinStreak; }

        public void setGamesPlayed(int totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }
        public void setWins(int totalGamesWon) { this.totalGamesWon = totalGamesWon; }
        public void setWinRate(double winRate) { this.winRate = winRate; }
        public void setLevel(int level) { this.level = level; }
        public void setExperience(int experience) { this.experience = experience; }
        public void setDailyStreak(int dailyLoginStreak) { this.dailyLoginStreak = dailyLoginStreak; }
        public void setTotalCorrectAnswers(int totalCorrectAnswers) { this.totalCorrectAnswers = totalCorrectAnswers; }
        public void setCurrentWinStreak(int currentWinStreak) { this.currentWinStreak = currentWinStreak; }
        public void setMaxWinStreak(int maxWinStreak) { this.maxWinStreak = maxWinStreak; }

    }

    public static class GeographyDto {
        private int europeCorrect;
        private int asiaCorrect;
        private int africaCorrect;
        private int americaCorrect;
        private int oceaniaCorrect;

        public int getEuropeCorrect() { return europeCorrect; }
        public int getAsiaCorrect() { return asiaCorrect; }
        public int getAfricaCorrect() { return africaCorrect; }
        public int getAmericaCorrect() { return americaCorrect; }
        public int getOceaniaCorrect() { return oceaniaCorrect; }

        public String getBestContinent() {
            int[] values = {europeCorrect, asiaCorrect, africaCorrect, americaCorrect, oceaniaCorrect};
            String[] continents = {"europe", "asia", "africa", "america", "oceania"};

            int maxIndex = 0;
            for (int i = 1; i < values.length; i++) {
                if (values[i] > values[maxIndex]) maxIndex = i;
            }
            return continents[maxIndex];
        }

        public void setEuropeCorrect(int europeCorrect) { this.europeCorrect = europeCorrect; }
        public void setAsiaCorrect(int asiaCorrect) { this.asiaCorrect = asiaCorrect; }
        public void setAfricaCorrect(int africaCorrect) { this.africaCorrect = africaCorrect; }
        public void setAmericaCorrect(int americaCorrect) { this.americaCorrect = americaCorrect; }
        public void setOceaniaCorrect(int oceaniaCorrect) { this.oceaniaCorrect = oceaniaCorrect; }
    }

    public static class GameModesDto {
        private int flagsCorrect;
        private int capitalsCorrect;
        private int outlinesCorrect;
        private int languagesCorrect;
        public int getFlagsCorrect() { return flagsCorrect; }
        public int getCapitalsCorrect() { return capitalsCorrect; }
        public int getOutlinesCorrect() { return outlinesCorrect; }
        public int getLanguagesCorrect() { return languagesCorrect; }

        public void setFlagsCorrect(int flagsCorrect) { this.flagsCorrect = flagsCorrect; }
        public void setCapitalsCorrect(int capitalsCorrect) { this.capitalsCorrect = capitalsCorrect; }
        public void setOutlinesCorrect(int outlinesCorrect) { this.outlinesCorrect = outlinesCorrect; }
        public void setLanguagesCorrect(int languagesCorrect) { this.languagesCorrect = languagesCorrect; }

    }

    public static class PvPDto {
        private int pvpGamesPlayed;
        private int pvpGamesWon;
        private double winRate;
        private int currentPvpStreak;
        public int getPvpGamesPlayed() { return pvpGamesPlayed; }
        public int getPvpGamesWon() { return pvpGamesWon; }
        public double getWinRate() { return winRate; }
        public int getCurrentPvpStreak() { return currentPvpStreak; }

        public void setPvpGamesPlayed(int pvpGamesPlayed) { this.pvpGamesPlayed = pvpGamesPlayed; }
        public void setPvpGamesWon(int pvpGamesWon) { this.pvpGamesWon = pvpGamesWon; }
        public void setWinRate(double winRate) { this.winRate = winRate; }
        public void setCurrentPvpStreak(int currentPvpStreak) { this.currentPvpStreak = currentPvpStreak; }
    }

    public static class AchievementDto {
        private String userId;
        private String code;
        private int progress;
        private int rarity;
        private boolean isUnlocked;
        private Date unlockedAt;

        public AchievementDto() {}

        public String getUserId() { return userId; }
        public String getCode() { return code; }
        public int getProgress() { return progress; }
        public int getRarity() { return rarity; }
        public boolean isUnlocked() { return isUnlocked; }
        public Date getUnlockedAt() { return unlockedAt; }

        public void setUserId(String userId) { this.userId = userId; }
        public void setCode(String code) { this.code = code; }
        public void setProgress(int progress) { this.progress = progress; }
        public void setRarity(int rarity) { this.rarity = rarity; }
        public void setUnlocked(boolean isUnlocked) { this.isUnlocked = isUnlocked; }
        public void setUnlockedAt(Date unlockedAt) { this.unlockedAt = unlockedAt; }
    }
}
