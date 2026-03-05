namespace GeoQuiz_backend.Domain.Entities
{
    public class KothLobby
    {
        public Guid Id { get; set; }
        //public List<Guid> PlayerIds { get; set; } = new();
        public List<PlayerLobby> PlayersLobby { get; set; } = new();
        public DateTime CreatedAt { get; set; }
    }
    public class PlayerLobby
    {
        public Guid Id { get; set; }
        public string Name { get; set; }
        public int Level { get; set; }
    }
}
