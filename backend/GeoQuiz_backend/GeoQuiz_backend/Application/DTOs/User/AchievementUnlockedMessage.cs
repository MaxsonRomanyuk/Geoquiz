namespace GeoQuiz_backend.Application.DTOs.User
{
    public class AchievementUnlockedMessage
    {
        public List<AchievementDto> Achievements { get; set; }
        public AchievementUnlockedMessage() { }
        public AchievementUnlockedMessage(List<AchievementDto> achievementUnlockeds) {
            Achievements = achievementUnlockeds;
        }
    }
}
