package com.example.geoquiz_frontend.data.remote.dtos.auth;

import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    private String userName;
    private String email;
    private String password;
    @SerializedName("stats")
    private UserStatsRequest stats;

    public RegisterRequest(String userName, String email, String password) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.stats = null;
    }

    public RegisterRequest(String userName, String email, String password, UserStats userStats) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        if (userStats != null) {
            this.stats = new UserStatsRequest(userStats);
        }
    }
}
