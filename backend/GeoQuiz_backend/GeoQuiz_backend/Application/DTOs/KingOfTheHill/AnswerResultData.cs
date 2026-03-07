namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class AnswerResultData
    {
        public bool IsCorrect { get; set; }
        public int ScoreGained { get; set; }
        public int TimeSpentMs { get; set; }
        public int RemainingPlayers { get; set; }
        public int CorrectOptionIndex { get; set; } 
    }
}
