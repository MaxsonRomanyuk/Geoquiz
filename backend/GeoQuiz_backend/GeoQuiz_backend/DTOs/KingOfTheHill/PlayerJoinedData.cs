namespace GeoQuiz_backend.DTOs.KingOfTheHill
{
    public class PlayerJoinedData
    {
        public Guid LobbyId { get; set; }
        public Guid PlayerId { get; set; }
        public int TotalPlayers { get; set; }
    }
}
