namespace GeoQuiz_backend.Application.DTOs.Game
{
    public class GameHistoryResponse
    {
        public List<GameSessionDto> Matches { get; set; } = new List<GameSessionDto> ();
        public int TotalCount { get; set; }
        public int TotalPages { get; set; }
        public int TotalWins { get; set; }
        public long ServerTime { get; set; }
    }
}
