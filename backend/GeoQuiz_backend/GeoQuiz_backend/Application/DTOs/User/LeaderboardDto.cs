namespace GeoQuiz_backend.Application.DTOs.User
{
    public class LeaderboardDto
    {
        public List<LeaderboardEntry> LeaderboardEntries { get; set; } = new List<LeaderboardEntry>();
        public int yourRank { get; set; }
        public int yourScore { get; set; }
    }
    public class LeaderboardEntry
    {
        public Guid playerId { get; set; }
        public string playerName { get; set; } = null!;
        public int rank { get; set; }
        public int level { get; set; }
        public int totalScore { get; set; }
    }
}
