namespace GeoQuiz.Backend.Domain.Entities
{
    public class PvPAnswer
    {
        public Guid Id { get; set; }

        public Guid MatchId { get; set; }
        public Guid UserId { get; set; }

        public string QuestionId { get; set; } = null!;

        public bool IsCorrect { get; set; }
        public int TimeSpentMs { get; set; }
        public int ScoreGained { get; set; }

        public DateTime AnsweredAt { get; set; }

        public PvPMatch Match { get; set; } = null!;
        public User User { get; set; } = null!;
    }
}
