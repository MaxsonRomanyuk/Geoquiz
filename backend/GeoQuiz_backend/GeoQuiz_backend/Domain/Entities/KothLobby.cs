namespace GeoQuiz_backend.Domain.Entities
{
    public class KothLobby
    {
        public Guid Id { get; set; }
        public List<Guid> PlayerIds { get; set; } = new();
        public DateTime CreatedAt { get; set; }
        public DateTime? CountdownStartTime { get; set; } 
        public bool IsCountdownActive { get; set; }
    }
}
