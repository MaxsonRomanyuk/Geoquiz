package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.example.geoquiz_frontend.domain.enums.RoundType;

import java.util.List;

public class RoundFinishedData {
    private int roundNumber;
    private int roundType;
    private int correctOptionIndex;
    private List<String> eliminatedPlayerIds;
    private List<PlayerRoundResult> results;
    private int remainingPlayers;
    private boolean isMatchFinished;

    public int getRoundNumber() { return roundNumber; }
    public int getRoundTypeValue() { return roundType; }
    public RoundType getRoundType() { return RoundType.fromValue(roundType); }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public List<String> getEliminatedPlayerIds() { return eliminatedPlayerIds; }
    public List<PlayerRoundResult> getResults() { return results; }
    public int getRemainingPlayers() { return remainingPlayers; }
    public boolean isMatchFinished() { return isMatchFinished; }

    public boolean isEliminated(String userId) {
        return eliminatedPlayerIds != null && eliminatedPlayerIds.contains(userId);
    }
}
