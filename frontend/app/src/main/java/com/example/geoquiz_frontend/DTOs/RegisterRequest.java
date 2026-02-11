package com.example.geoquiz_frontend.DTOs;

public class RegisterRequest {
    private String userName;
    private String email;
    private String password;

    public RegisterRequest(String userName, String email, String password) {
        this.userName = userName;
        this.email = email;
        this.password = password;
    }
}
