namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class MatchResumeData
    {
        public Guid MatchId { get; set; }
        public int CurrentScore { get; set; }
        public int TotalPlayers { get; set; }
        public int PlayersLeft { get; set; }
        public RoundStartedData RoundStartedData { get; set; }
    }
}
