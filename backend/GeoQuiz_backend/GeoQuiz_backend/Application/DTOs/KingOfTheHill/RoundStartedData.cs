using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class RoundStartedData
    {
        public int RoundNumber { get; set; }
        public RoundType RoundType { get; set; } 
        public QuestionData Question { get; set; } = null!;
        public DateTime RoundStartTime { get; set; }
        public int TimeLimitSeconds { get; set; } = 10;
    }

    public class QuestionData
    {
        public string QuestionId { get; set; } = null!;
        public LocalizedText QuestionText { get; set; } = null!; 
        public List<OptionData> Options { get; set; } = new(); 
        public string? ImageUrl { get; set; }
        public string? AudioUrl { get; set; }
    }

    public class OptionData
    {
        public int Index { get; set; }
        public LocalizedText Text { get; set; } = null!; 
    }
}
