package com.example.geoquiz_frontend.data.remote.dtos.pvp;

import java.util.List;

public class GameFinishedData {
    private String matchId;
    private String winnerId;
    private String finishReason;
    private PlayerFinalStats yourStats;
    private PlayerFinalStats opponentStats;
    private int experienceGained;
    private List<UnlockedAchievement> unlockedAchievements;

    public String getMatchId() { return matchId; }
    public String getWinnerId() { return winnerId; }
    public String getFinishReason() { return finishReason; }
    public PlayerFinalStats getYourStats() { return yourStats; }
    public PlayerFinalStats getOpponentStats() { return opponentStats; }
    public int getExperienceGained() { return experienceGained; }
    public List<UnlockedAchievement> getUnlockedAchievements() { return unlockedAchievements; }

    public boolean isWinner() {
        return winnerId != null && winnerId.equals(yourStats != null ? yourStats.getUserId() : null);
    }
}
