package com.example.geoquiz_frontend.data.remote.dtos.pvp;

public class TimerUpdateData {
    //private String matchId;
    private long serverTime;
    private long timerEndsAt;

    public TimerUpdateData(long serverTime, long timerEndsAt)
    {
        this.serverTime = serverTime;
        this.timerEndsAt = timerEndsAt;
    }
    //public String getMatchId() { return matchId; }
    public long getServerTime() { return serverTime; }
    public long getTimerEndsAt() { return timerEndsAt; }

}