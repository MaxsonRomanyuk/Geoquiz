package com.example.geoquiz_frontend.data.remote.dtos.pvp;

public class BanModeRequest {
    private String matchId;
    private int mode;
    private int language;

    public BanModeRequest(String matchId, int mode, int language) {
        this.matchId = matchId;
        this.mode = mode;
        this.language = language;
    }

    public String getMatchId() { return matchId; }
    public int getMode() { return mode; }
    public int getLanguage() { return language; }
}
