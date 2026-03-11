package com.example.geoquiz_frontend.data.remote.dtos.koth;

import java.util.List;

public class MatchStartedData {
    private String matchId;

    private int totalPlayers;

    private int totalRounds;

    private String firstRoundStartTime;

    private List<PlayerInfo> allPlayers;

    public String getMatchId() { return matchId; }
    public int getTotalPlayers() { return totalPlayers; }
    public int getTotalRounds() { return totalRounds; }
    public String getFirstRoundStartTime() { return firstRoundStartTime; }
    public List<PlayerInfo> getAllPlayers() { return allPlayers; }
}
