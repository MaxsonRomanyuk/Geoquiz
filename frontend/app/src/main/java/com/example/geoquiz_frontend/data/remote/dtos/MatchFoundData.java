package com.example.geoquiz_frontend.data.remote.dtos;

import java.util.List;

public class MatchFoundData {
    private String matchId;
    private String opponentId;
    private String opponentName;
    private int opponentLevel;
    private boolean opponentIsPremium;
    private List<String> availableModes;
    private List<String> bannedModes;
    private String currentTurnUserId;
    private int timePerTurnSeconds;
    private String firstTurnStartTime;
    private String yourId;

    public String getMatchId() { return matchId; }
    public String getOpponentId() { return opponentId; }
    public String getOpponentName() { return opponentName; }
    public int getOpponentLevel() { return opponentLevel; }
    public boolean isOpponentIsPremium() { return opponentIsPremium; }
    public List<String> getAvailableModes() { return availableModes; }
    public List<String> getBannedModes() { return bannedModes; }
    public String getCurrentTurnUserId() { return currentTurnUserId; }
    public int getTimePerTurnSeconds() { return timePerTurnSeconds; }
    public String getFirstTurnStartTime() { return firstTurnStartTime; }
    public String getYourId() { return yourId; }

    public boolean isYourTurn() {
        return yourId != null && yourId.equals(currentTurnUserId);
    }
}