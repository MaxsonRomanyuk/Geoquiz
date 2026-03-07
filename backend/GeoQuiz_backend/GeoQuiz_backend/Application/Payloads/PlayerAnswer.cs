namespace GeoQuiz_backend.Application.Payloads
{
    public class PlayerAnswer
    {
        public string QuestionId { get; set; } = null!;
        public int SelectedOptionIndex { get; set; }
        public bool IsCorrect { get; set; }
        public int TimeSpentMs { get; set; }
        public int ScoreGained { get; set; }
        public DateTime AnsweredAt { get; set; }
    }
}
