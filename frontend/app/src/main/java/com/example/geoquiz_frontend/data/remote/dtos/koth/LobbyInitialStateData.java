package com.example.geoquiz_frontend.data.remote.dtos.koth;

import java.util.List;
import java.util.UUID;

public class LobbyInitialStateData {
    private String lobbyId;
    private List<PlayerLobby> players;
    private int totalPlayers;

    public String getLobbyId() { return lobbyId; }
    public List<PlayerLobby> getPlayers() { return players; }
    public int getTotalPlayers() { return totalPlayers; }
}

