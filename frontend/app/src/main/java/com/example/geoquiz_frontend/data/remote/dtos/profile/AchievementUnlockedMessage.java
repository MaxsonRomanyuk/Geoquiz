package com.example.geoquiz_frontend.data.remote.dtos.profile;

import java.util.List;

public class AchievementUnlockedMessage {
    private List<ProfileResponse.AchievementDto> achievements;

    public List<ProfileResponse.AchievementDto> getAchievements() {
        return achievements;
    }
}
