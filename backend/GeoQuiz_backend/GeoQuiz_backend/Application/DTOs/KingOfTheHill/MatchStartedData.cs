namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class MatchStartedData
    {
        public Guid MatchId { get; set; }
        public int TotalPlayers { get; set; }
        public int TotalRounds { get; set; } 
        public DateTime FirstRoundStartTime { get; set; }
        public List<PlayerInfo> AllPlayers { get; set; } = new(); 
    }

    public class PlayerInfo
    {
        public Guid PlayerId { get; set; }
        public string PlayerName { get; set; } = null!;
        public int PlayerLevel { get; set; }
    }
}
