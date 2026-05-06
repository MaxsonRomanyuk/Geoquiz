using GeoQuiz_backend.Domain.Enums;

namespace GeoQuiz_backend.Domain.Entities
{
    public class QuestionSet
    {
        public Guid Id { get; set; }

        public Guid? PvPMatchId { get; set; }
        public PvPMatch? PvPMatch { get; set; }
        public Guid? KothMatchId { get; set; }
        public KothMatch? KothMatch { get; set; }
        public GameMode Mode { get; set; }
        public List<string> CountryIds { get; set; } = new();
        public int Difficality { get; set; } = 1;
        //public List<string> QuestionIds { get; set; } = new();
        public List<Region> Regions { get; set; } = new();
        public int Seed { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
