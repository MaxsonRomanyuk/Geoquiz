using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;

namespace GeoQuiz_backend.Domain.Entities
{
    public class GameSession
    {
        public Guid Id { get; set; }

        public Guid UserId { get; set; }
        public User User { get; set; } = null!;

        public Guid? PvPMatchId { get; set; }
        public PvPMatch? PvPMatch { get; set; }

        public Guid? KothMatchId { get; set; }
        public KothMatch? KothMatch { get; set; }
        public int? Place { get; set; }
        public int? RoundsSurvived { get; set; }

        public GameMode Mode { get; set; }

        public int TotalQuestions { get; set; }
        public int CorrectAnswers { get; set; }
        public int Score { get; set; }

        public bool IsOnline { get; set; }
        public DateTime PlayedAt { get; set; }
    }
}
