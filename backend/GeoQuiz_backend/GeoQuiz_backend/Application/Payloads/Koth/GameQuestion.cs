using GeoQuiz_backend.Domain.Mongo;

namespace GeoQuiz_backend.Application.Payloads.Koth
{
    public class GameQuestion
    {
        public string QuestionId { get; set; } = null!;
        public LocalizedText QuestionText { get; set; } = null!;
        public List<GameOption> Options { get; set; } = new();
        public int CorrectOptionIndex { get; set; }
        public string? ImageUrl { get; set; }
        public string? AudioUrl { get; set; }
    }
    public class GameOption
    {
        public int Index { get; set; }
        public LocalizedText Text { get; set; } = null!;
    }

}
