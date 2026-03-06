namespace GeoQuiz_backend.Application.DTOs.PvP
{
    public class PvPMatchResultDto
    {
        public Guid MatchId { get; set; }
        public Guid Player1Id { get; set; }
        public Guid Player2Id { get; set; }
        public Guid WinnerId { get; set; }
        public int Player1Score { get; set; }
        public int Player2Score { get; set; }
    }
}
