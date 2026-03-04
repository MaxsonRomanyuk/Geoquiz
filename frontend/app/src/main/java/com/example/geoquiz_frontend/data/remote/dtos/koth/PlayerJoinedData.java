package com.example.geoquiz_frontend.data.remote.dtos.koth;

public class PlayerJoinedData {
    private String lobbyId;
    private String playerId;
    private String playerName;
    private int playerLevel;
    private int totalPlayers;

    public String getLobbyId() { return lobbyId; }
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getPlayerLevel() { return playerLevel; }
    public int getTotalPlayers() { return totalPlayers; }

}
