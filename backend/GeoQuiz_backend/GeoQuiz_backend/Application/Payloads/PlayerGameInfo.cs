namespace GeoQuiz_backend.Application.Payloads
{
    public class PlayerGameInfo
    {
        public string UserName { get; set; } = null!;
        public int Level { get; set; }
        public bool IsActive { get; set; } = true;
        public int EliminatedAtRound { get; set; }
    }
}
