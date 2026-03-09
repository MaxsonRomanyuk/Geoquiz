using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class RoundFinishedData
    {
        public int RoundNumber { get; set; }
        public RoundType RoundType { get; set; }
        public List<Guid> EliminatedPlayerIds { get; set; } = new(); 
        public List<PlayerRoundResult> Results { get; set; } = new(); 
        public int RemainingPlayers { get; set; }
        public bool IsMatchFinished { get; set; }
    }

    public class PlayerRoundResult
    {
        public Guid PlayerId { get; set; }
        public bool HasAnswered { get; set; }
        public bool IsCorrect { get; set; }
        public int TimeSpentMs { get; set; }
        public int ScoreGained { get; set; }
    }
}
