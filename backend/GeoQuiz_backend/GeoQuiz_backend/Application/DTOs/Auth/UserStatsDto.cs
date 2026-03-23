namespace GeoQuiz_backend.Application.DTOs.Auth
{
    public class UserStatsDto
    {
        public int Level { get; set; }
        public int Experience { get; set; }
        public int GamesPlayed { get; set; }
        public int GamesWon { get; set; }
        public float WinRate { get; set; }
        public int DailyStreak { get; set; }
        public int WinStreak { get; set; }
        public int EuropeCorrect { get; set; }
        public int AsiaCorrect { get; set; }
        public int AfricaCorrect { get; set; }
        public int AmericaCorrect { get; set; }
        public int OceaniaCorrect { get; set; }
        public string BestContinent { get; set; } = string.Empty;
        public int CapitalsCorrect { get; set; }
        public int FlagsCorrect { get; set; }
        public int OutlinesCorrect { get; set; }
        public int LanguagesCorrect { get; set; }
    }
}
