package com.example.geoquiz_frontend.data.remote.dtos.profile;

import com.example.geoquiz_frontend.presentation.ui.leaderboard.LeaderboardEntry;

import java.util.List;

public class LeaderboardDto {
    private List<LeaderboardEntry> leaderboardEntries;
    private int yourRank;
    private int yourScore;
    public List<LeaderboardEntry> getLeaderboardEntries() { return leaderboardEntries;}
    public int getYourRank() { return yourRank; }
    public int getYourScore() { return yourScore; }
}
