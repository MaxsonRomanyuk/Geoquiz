namespace GeoQuiz_backend.Domain.Entities
{
    public class KothAnswer
    {
        public Guid Id { get; set; }

        public Guid MatchId { get; set; }
        public KothMatch Match { get; set; } = null!;

        public Guid UserId { get; set; }
        public User User { get; set; } = null!;

        public string QuestionId { get; set; } = null!; 
        public int RoundNumber { get; set; } 

        public bool IsCorrect { get; set; }
        public int TimeSpentMs { get; set; }
        public int ScoreGained { get; set; }

        public DateTime AnsweredAt { get; set; }
    }
}
