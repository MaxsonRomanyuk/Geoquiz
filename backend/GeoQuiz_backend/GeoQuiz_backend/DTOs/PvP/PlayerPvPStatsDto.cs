namespace GeoQuiz_backend.DTOs.PvP
{
    public class PlayerPvPStatsDto
    {
        public Guid UserId { get; set; }
        public int TotalQuestions { get; set; }
        public int CorrectAnswers { get; set; }
        public int Score { get; set; }
    }
}
