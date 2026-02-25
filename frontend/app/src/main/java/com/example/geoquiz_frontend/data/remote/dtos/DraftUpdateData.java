package com.example.geoquiz_frontend.data.remote.dtos;

import java.util.List;

public class DraftUpdateData {
    private String matchId;
    private String bannedMode;
    private String bannedByUserId;
    private List<String> remainingModes;
    private String nextTurnUserId;
    private int step;

    public String getMatchId() { return matchId; }
    public String getBannedMode() { return bannedMode; }
    public String getBannedByUserId() { return bannedByUserId; }
    public List<String> getRemainingModes() { return remainingModes; }
    public String getNextTurnUserId() { return nextTurnUserId; }
    public int getStep() { return step; }
}
