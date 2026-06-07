package com.example.geoquiz_frontend.domain.entities;

import java.util.Date;

public class User {
    private String uid;
    private String email;
    private String name;
    private Date registeredAt;
    public User() {
    }

    public User(String uid, String email, String name) {
        this.uid = uid;
        this.email = email;
        this.name = name;
        this.registeredAt = new Date();
    }

    public String getId() { return uid; }
    public void setId(String uid) {this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
