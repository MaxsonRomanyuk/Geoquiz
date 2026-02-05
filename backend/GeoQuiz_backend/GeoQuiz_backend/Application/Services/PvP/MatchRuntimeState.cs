namespace GeoQuiz_backend.Application.Services.PvP
{
    public class MatchRuntimeState
    {
        public Guid MatchId { get; set; }
        public DateTime StartedAt { get; set; }
        public Dictionary<Guid, PlayerRuntimeState> Players { get; set; } = new();
        public bool IsFinished { get; set; }
    }
    public class PlayerRuntimeState
    {
        public Guid UserId { get; set; }
        public int Correct { get; set; }
        public int Total { get; set; }
        public bool Finished { get; set; }
        public int Score { get; set; }
    }
}
