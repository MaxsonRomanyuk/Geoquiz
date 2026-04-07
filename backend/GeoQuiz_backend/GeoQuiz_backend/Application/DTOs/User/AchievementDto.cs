namespace GeoQuiz_backend.Application.DTOs.User
{
    public class AchievementDto
    {
        public string Code { get; set; } = null!;
        public int Progress { get; set; }
        public int Rarity { get; set; }
        public bool IsUnlocked { get; set; }
        public DateTime UnlockedAt { get; set; }
    }
}
