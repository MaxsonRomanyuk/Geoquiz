package com.example.geoquiz_frontend.data.remote.dtos.pvp;

public class TimerUpdateData {
    private String matchId;
    private int remainingTimeSeconds;
    private String serverTime;

    public String getMatchId() { return matchId; }
    public int getRemainingTimeSeconds() { return remainingTimeSeconds; }
    public String getServerTime() { return serverTime; }
}