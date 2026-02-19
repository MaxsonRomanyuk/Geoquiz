using GeoQuiz_backend.Domain.Enums;

namespace GeoQuiz_backend.DTOs.Game
{
    public class SyncGameSessionRequest
    {
        public Guid Id { get; set; }
        public GameMode Mode { get; set; }
        public int TotalQuestions { get; set; }
        public int CorrectAnswers { get; set; }
        public int EuropeCorrect { get; set; }
        public int AsiaCorrect { get; set; }
        public int AfricaCorrect { get; set; }
        public int AmericaCorrect { get; set; }
        public int OceaniaCorrect { get; set; }
        public int Score { get; set; }
        public int TimeSpent { get; set; }
        public bool IsOnline { get; set; }
        public DateTime PlayedAt { get; set; }
    }
}
