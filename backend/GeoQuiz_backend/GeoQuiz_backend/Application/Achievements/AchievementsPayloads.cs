namespace GeoQuiz_backend.Application.Achievements
{
    public class AchievementsPayloads
    {
        public class GameCompletedData
        {
            public int CorrectAnswers { get; set; }
            public int TimeSpent { get; set; }
            public string? Continent { get; set; }
            public string GameMode { get; set; } = null!;
            public bool UsedAllHints { get; set; }
        }
        public class PvPResultData
        {
            public int UserLevel { get; set; }
            public int OpponentLevel { get; set; }
            public bool UserWon { get; set; }
        }
    }
}
