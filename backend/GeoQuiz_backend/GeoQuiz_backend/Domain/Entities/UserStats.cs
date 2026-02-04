using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace GeoQuiz_backend.Domain.Entities
{
    public class UserStats
    {
        public Guid UserId { get; set; }

        public int Level { get; set; }
        public int Experience { get; set; }


        public int TotalGamesPlayed { get; set; }
        public int TotalGamesWon { get; set; }
        public int TotalCorrectAnswers { get; set; }

        public int TotalQuickAnswers { get; set; }
        public int TotalLastSecondWins { get; set; }


        public int CurrentWinStreak { get; set; }
        public int MaxWinStreak { get; set; }
        public int DailyLoginStreak { get; set; }
        public DateTime LastLoginDate { get; set; }


        public int EuropeCorrect { get; set; }
        public int AsiaCorrect { get; set; }
        public int AfricaCorrect { get; set; }
        public int AmericaCorrect { get; set; }
        public int OceaniaCorrect { get; set; }


        public int FlagsCorrect { get; set; }
        public int CapitalsCorrect { get; set; }
        public int OutlinesCorrect { get; set; }
        public int LanguagesCorrect { get; set; }


        public int PvPGamesPlayed { get; set; }
        public int PvPGamesWon { get; set; }
        public int CurrentPvPStreak { get; set; }

        public User User { get; set; } = null!;
    }
}
