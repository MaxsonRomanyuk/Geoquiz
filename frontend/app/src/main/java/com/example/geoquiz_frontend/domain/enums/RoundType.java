package com.example.geoquiz_frontend.domain.enums;

public enum RoundType {
    UNKNOWN(0),
    CLASSIC(1),
    SPEED(2);
    private final int value;
    RoundType(int value) {
        this.value = value;
    }

    public int getValue() { return value; }

    public static RoundType fromValue(int value) {
        for (RoundType type : RoundType.values()) {
            if (type.value == value) return type;
        }
        return UNKNOWN;
    }
}
