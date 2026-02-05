using GeoQuiz_backend.Domain.Enums;

namespace GeoQuiz_backend.Domain.Entities
{
    public class PvPMatch
    {
        public Guid Id { get; set; }

        public Guid Player1Id { get; set; }
        public User Player1 { get; set; } = null!;

        public Guid Player2Id { get; set; }
        public User Player2 { get; set; } = null!;

        public PvPMatchStatus Status { get; set; }

        public GameMode? SelectedMode { get; set; }

        public ModeDraft? Draft { get; set; }
        public QuestionSet? QuestionSet { get; set; }

        public ICollection<PvPAnswer> Answers { get; set; } = new List<PvPAnswer>();

        public Guid? WinnerId { get; set; }
        public User? Winner { get; set; }

        public DateTime CreatedAt { get; set; }
        public DateTime? FinishedAt { get; set; }
    }
}
