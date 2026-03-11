package com.example.geoquiz_frontend.data.remote.dtos.koth;

import java.util.List;

public class MatchFinishedData {
    private String matchId;
    private String winnerId;
    private List<PlayerFinalStanding> finalStandings;

    public String getMatchId() { return matchId; }
    public String getWinnerId() { return winnerId; }
    public List<PlayerFinalStanding> getFinalStandings() { return finalStandings; }

    public boolean isWinner(String userId) {
        return winnerId != null && winnerId.equals(userId);
    }
}
