package com.example.geoquiz_frontend.domain.entities;

import com.example.geoquiz_frontend.domain.enums.LocalizedText;

public class Achievement {
    public String code;
    public LocalizedText title;
    public LocalizedText description;
    public int category;
    public int rarity;
    public int progress;
    public boolean isUnlocked;
    public String unlockedAt;
    public String icon;

    public Achievement(String code, LocalizedText title, LocalizedText description, int category, int rarity,
                       int progress, boolean isUnlocked, String unlockedAt, String icon) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.progress = progress;
        this.isUnlocked = isUnlocked;
        this.unlockedAt = unlockedAt;
        this.icon = icon;
    }
}
