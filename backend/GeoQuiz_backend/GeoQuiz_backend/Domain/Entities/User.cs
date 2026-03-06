using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Domain.Entities
{
    public class User
    {
        public Guid Id { get; set; }
        public string UserName { get; set; } = null!;
        public string Email { get; set; } = null!;
        public string PasswordHash { get; set; } = null!;
        public DateTime RegisteredAt { get; set; }
        public bool IsPremium { get; set; }

        public UserStats Stats { get; set; } = null!;

        public ICollection<GameSession> GameSessions { get; set; }
            = new List<GameSession>();

        public ICollection<PvPAnswer> PvPAnswers { get; set; } = new List<PvPAnswer>();

        public ICollection<UserAchievement> UserAchievements { get; set; }
            = new List<UserAchievement>();

        public Subscription? Subscription { get; set; }
    }
}
