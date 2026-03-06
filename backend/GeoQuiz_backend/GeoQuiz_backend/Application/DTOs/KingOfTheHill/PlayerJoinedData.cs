namespace GeoQuiz_backend.Application.DTOs.KingOfTheHill
{
    public class PlayerJoinedData
    {
        public Guid LobbyId { get; set; }
        public Guid PlayerId { get; set; }
        public string PlayerName { get; set; } = null!;  
        public int PlayerLevel { get; set; } 
        public int TotalPlayers { get; set; }
    }
}
