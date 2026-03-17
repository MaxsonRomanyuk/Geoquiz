using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Payloads.Koth
{
    public class KothGameState
    {
        public Guid MatchId { get; set; }
        public KothMatch Match { get; set; } = null!;
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
        public Dictionary<Guid, int> PlayerPlaces { get; set; } = new();
        public List<KothAnswer> PendingAnswers { get; set; } = new();
        public bool IsRoundStarting { get; set; }      
        public bool IsRoundFinishing { get; set; }     
        public bool IsRoundFinished { get; set; }      
        public bool IsMatchFinishing { get; set; }
        public HashSet<Guid> AnsweredPlayers { get; set; } = new();
    }
}
