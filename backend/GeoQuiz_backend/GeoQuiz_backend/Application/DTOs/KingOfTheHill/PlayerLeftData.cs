namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class PlayerLeftData
    {
        public Guid LobbyId { get; set; }
        public Guid PlayerId { get; set; }
        public int TotalPlayers { get; set; }
    }
}
