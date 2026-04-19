using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace GeoQuiz_backend.Domain.Entities
{
    public class UserStats
    {
        public Guid UserId { get; set; }

        public int Level { get; set; }
        public int Experience { get; set; }
        public int Score { get; set; }


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

        public int KothGamesPlayed { get; set; }
        public int KothGamesWon { get; set; }
        public int KothTop3Finishes { get; set; }
        public DateTime LastAchievementSync { get; set; }

        public User User { get; set; } = null!;
        public UserStats Clone()
        {
            return new UserStats
            {
                UserId = this.UserId,
                Level = this.Level,
                Experience = this.Experience,
                Score = this.Score,
                TotalGamesPlayed = this.TotalGamesPlayed,
                TotalGamesWon = this.TotalGamesWon,
                TotalCorrectAnswers = this.TotalCorrectAnswers,
                TotalQuickAnswers = this.TotalQuickAnswers,
                TotalLastSecondWins = this.TotalLastSecondWins,
                CurrentWinStreak = this.CurrentWinStreak,
                MaxWinStreak = this.MaxWinStreak,
                DailyLoginStreak = this.DailyLoginStreak,
                LastLoginDate = this.LastLoginDate,
                EuropeCorrect = this.EuropeCorrect,
                AsiaCorrect = this.AsiaCorrect,
                AfricaCorrect = this.AfricaCorrect,
                AmericaCorrect = this.AmericaCorrect,
                OceaniaCorrect = this.OceaniaCorrect,
                FlagsCorrect = this.FlagsCorrect,
                CapitalsCorrect = this.CapitalsCorrect,
                OutlinesCorrect = this.OutlinesCorrect,
                LanguagesCorrect = this.LanguagesCorrect,
                PvPGamesPlayed = this.PvPGamesPlayed,
                PvPGamesWon = this.PvPGamesWon,
                CurrentPvPStreak = this.CurrentPvPStreak,
                KothGamesPlayed = this.KothGamesPlayed,
                KothGamesWon = this.KothGamesWon,
                KothTop3Finishes = this.KothTop3Finishes,
                LastAchievementSync = this.LastAchievementSync,
            };
        }
    }
}
