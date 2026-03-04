package com.example.geoquiz_frontend.data.remote.dtos.koth;

public class PlayerLeftData {
    private String lobbyId;

    private String playerId;

    private int totalPlayers;

    public String getLobbyId() { return lobbyId; }
    public String getPlayerId() { return playerId; }
    public int getTotalPlayers() { return totalPlayers; }
}
