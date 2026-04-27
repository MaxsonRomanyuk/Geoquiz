using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.DTOs.PvP
{
    public class PvPGameStateDto
    {
        public Guid MatchId { get; set; }
        public GameMode Mode { get; set; }
        public AppLanguage Language { get; set; }
        //public List<string> QuestionIds { get; set; } = new();
        public List<QuestionData> Questions { get; set; } = new List<QuestionData>();
        public int YourAnswered { get; set; }
        public int OpponentAnswered { get; set; }
        public int YourCurrentScore { get; set; }
        public int OpponentCurrentScore { get; set; }
        public bool IsFinished { get; set; }
        public Domain.Entities.User Player { get; set; } = null!;
        public Domain.Entities.User Opponent { get; set; } = null!;
    }
}
