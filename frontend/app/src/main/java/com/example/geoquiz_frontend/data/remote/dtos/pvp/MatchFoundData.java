package com.example.geoquiz_frontend.data.remote.dtos.pvp;

import java.util.List;

public class MatchFoundData {
    private String matchId;
    private String opponentId;
    private String opponentName;
    private int opponentLevel;
    private int opponentScore;
    private boolean opponentIsPremium;
    private List<String> availableModes;
    private List<String> bannedModes;
    private String currentTurnUserId;
    private long timerEndAt;
    private long serverTime;
    private String firstTurnStartTime;
    private String yourId;

    public String getMatchId() { return matchId; }
    public String getOpponentId() { return opponentId; }
    public String getOpponentName() { return opponentName; }
    public int getOpponentLevel() { return opponentLevel; }
    public int getOpponentScore() { return opponentScore; }
    public boolean isOpponentIsPremium() { return opponentIsPremium; }
    public List<String> getAvailableModes() { return availableModes; }
    public List<String> getBannedModes() { return bannedModes; }
    public String getCurrentTurnUserId() { return currentTurnUserId; }
    public long getTimerEndAt() { return timerEndAt; }
    public long getServerTime() { return serverTime; }
    public String getFirstTurnStartTime() { return firstTurnStartTime; }
    public String getYourId() { return yourId; }

    public boolean isYourTurn() {
        return yourId != null && yourId.equals(currentTurnUserId);
    }
}