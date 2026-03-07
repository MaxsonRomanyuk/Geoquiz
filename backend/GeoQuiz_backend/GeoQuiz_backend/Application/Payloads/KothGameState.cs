using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Payloads
{
    public class KothGameState
    {
        public Guid MatchId { get; set; }
        public List<Guid> ActivePlayerIds { get; set; } = new();
        public Dictionary<Guid, PlayerGameInfo> Players { get; set; } = new();
        public List<Guid> EliminatedPlayers { get; set; } = new();
        public List<Guid> EliminatedThisRound { get; set; } = new();
        public int CurrentRound { get; set; }
        public RoundType CurrentRoundType { get; set; }
        public List<GameQuestion> Questions { get; set; } = new();
        public DateTime RoundStartTime { get; set; }
        public Dictionary<Guid, Dictionary<int, PlayerAnswer>> PlayerAnswers { get; set; } = new();
        public Dictionary<Guid, int> PlayerScores { get; set; } = new();
        public Dictionary<Guid, int> PlayerCorrectCount { get; set; } = new();
    }
}
