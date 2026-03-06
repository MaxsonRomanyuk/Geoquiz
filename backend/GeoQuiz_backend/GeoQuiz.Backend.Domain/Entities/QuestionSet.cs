using GeoQuiz.Backend.Domain.Enums;

namespace GeoQuiz.Backend.Domain.Entities
{
    public class QuestionSet
    {
        public Guid Id { get; set; }

        public Guid? PvPMatchId { get; set; }
        public PvPMatch? PvPMatch { get; set; }
        public Guid? KothMatchId { get; set; }
        public KothMatch? KothMatch { get; set; }

        public GameMode Mode { get; set; }
        public AppLanguage Language { get; set; }
        public List<string> QuestionIds { get; set; } = new();
        public int Seed { get; set; }

        public DateTime CreatedAt { get; set; }
    }
}
