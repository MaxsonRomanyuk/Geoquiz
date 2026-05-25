namespace GeoQuiz_backend.Application.DTOs.Game
{
    public class GameSessionDto
    {
        public Guid MatchId { get; set; }
        public int GameType { get; set; }
        public long PlayedAt { get; set; }
        public int TotalScore { get; set; }
        public int CorrectAnswers { get; set; }
        public int TotalQuestions { get; set; }
        public int GameMode { get; set; }
        public bool IsWin {  get; set; }
        public int? Place {  get; set; }
        public int? RoundsSurvived { get; set; }
        public int ExperienceGained { get; set; }
    }
}
