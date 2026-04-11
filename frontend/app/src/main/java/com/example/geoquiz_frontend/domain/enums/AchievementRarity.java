package com.example.geoquiz_frontend.domain.enums;
public enum AchievementRarity {
    Common(1),
    Rare(2),
    Epic(3),
    Legendary(4);

    private final int id;
    AchievementRarity(int id) {
        this.id = id;
    }
    public int getId() { return id; }
    public static AchievementRarity fromId(int id) {
        for (AchievementRarity rarity : values()) {
            if (rarity.id == id) return rarity;
        }
        return Common;
    }
}
