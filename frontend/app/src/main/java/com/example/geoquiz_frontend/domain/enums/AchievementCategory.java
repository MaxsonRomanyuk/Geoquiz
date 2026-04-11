package com.example.geoquiz_frontend.domain.enums;

import com.example.geoquiz_frontend.presentation.ui.achievements.AchievementsActivity;

public enum AchievementCategory {
    General(1, new LocalizedText("Общие", "General")),
    Gameplay(2, new LocalizedText("Режимы игры", "Gameplay")),
    Knowledge(3, new LocalizedText("Знания", "Knowledge")),
    Streaks(4, new LocalizedText("Серии", "Streaks")),
    PvP(5, new LocalizedText("Дуэль", "PvP")),
    Koth(6, new LocalizedText("Царь горы", "King of the hill")),
    Special(7, new LocalizedText("Особые", "Special"));

    private int id;
    private LocalizedText title;
    public LocalizedText getTitle() { return title; };

    AchievementCategory(int id, LocalizedText title) {
        this.id = id;
        this.title = title;
    }

    public static AchievementCategory fromId(int id) {
        for (AchievementCategory cat : values()) {
            if (cat.id == id) return cat;
        }
        return General;
    }
}
