namespace GeoQuiz.Backend.Domain.Entities
{
    public class UserAchievement
    {
        public Guid UserId { get; set; }
        public Guid AchievementId { get; set; }

        public int Progress { get; set; }
        public bool IsUnlocked { get; set; }
        public DateTime UnlockedAt { get; set; }

        public User User { get; set; } = null!;
        public Achievement Achievement { get; set; } = null!;
    }
}
