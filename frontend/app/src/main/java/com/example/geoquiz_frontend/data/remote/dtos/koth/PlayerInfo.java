package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class PlayerInfo implements Serializable {
    @SerializedName("playerId")
    private String playerId;

    @SerializedName("playerName")
    private String playerName;

    @SerializedName("playerLevel")
    private int playerLevel;

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getPlayerLevel() { return playerLevel; }
}