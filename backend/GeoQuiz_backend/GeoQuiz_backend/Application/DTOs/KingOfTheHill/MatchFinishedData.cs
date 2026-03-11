using GeoQuiz_backend.Application.DTOs.PvP;

namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class MatchFinishedData
    {
        public Guid MatchId { get; set; }
        public Guid WinnerId { get; set; }
        public List<PlayerFinalStanding> FinalStandings { get; set; } = new();
        //public List<UnlockedAchievement> UnlockedAchievements { get; set; } = new();
    }

    public class PlayerFinalStanding
    {
        public Guid PlayerId { get; set; }
        public string PlayerName { get; set; } = null!;
        public int Place { get; set; }
        public int CorrectAnswers { get; set; }
        public int TotalScore { get; set; }
        public int RoundsSurvived { get; set; }
    }
}
