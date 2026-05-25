package com.example.geoquiz_frontend.data.remote.dtos.history;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GameHistoryResponse {
    @SerializedName("matches")
    private List<GameSessionDto> matches;

    @SerializedName("totalCount")
    private int totalCount;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("totalWins")
    private int totalWins;

    @SerializedName("serverTime")
    private long serverTime;

    public List<GameSessionDto> getMatches() { return matches; }
    public int getTotalCount() { return totalCount; }
    public int getTotalPages() { return totalPages; }
    public int getTotalWins() { return totalWins; }
    public long getServerTime() { return serverTime; }
}
