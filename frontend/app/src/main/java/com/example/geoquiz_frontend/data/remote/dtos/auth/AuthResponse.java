package com.example.geoquiz_frontend.data.remote.dtos.auth;

public class AuthResponse {
    private String token;
    private String userId;
    private String userName;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserId() { return userId; }
    public void setUserId(String id) { this.userId = id; }
    public String getUserName() { return userName; }
    public void setUserName(String name) { this.userName = name; }
}
