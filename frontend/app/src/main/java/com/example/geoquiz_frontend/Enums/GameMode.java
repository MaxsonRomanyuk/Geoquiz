package com.example.geoquiz_frontend.Enums;

public enum GameMode {
    CAPITALS(1, "Столицы"),
    FLAGS(2, "Флаги"),
    OUTLINES(3, "Контуры"),
    LANGUAGES(4, "Языки");

    private final int value;
    private final String displayName;

    GameMode(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public int getValue() { return value; }
    public String getDisplayName() { return displayName; }

    public static GameMode fromValue(int value) {
        for (GameMode mode : values()) {
            if (mode.value == value) return mode;
        }
        return CAPITALS;
    }
}