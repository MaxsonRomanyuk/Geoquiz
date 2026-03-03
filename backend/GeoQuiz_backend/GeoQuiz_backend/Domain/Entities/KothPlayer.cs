namespace GeoQuiz_backend.Domain.Entities
{
    public class KothPlayer
    {
        public Guid Id { get; set; }

        public Guid MatchId { get; set; }
        public KothMatch Match { get; set; } = null!;

        public Guid UserId { get; set; }
        public User User { get; set; } = null!;

        public DateTime JoinedAt { get; set; }
        public int? Place { get; set; } 
        public bool IsActive { get; set; } = true; 
        public int RoundEliminated { get; set; } 
    }
}
