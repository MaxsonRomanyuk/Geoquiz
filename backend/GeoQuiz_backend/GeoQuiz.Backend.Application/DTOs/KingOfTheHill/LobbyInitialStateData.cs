using GeoQuiz.Backend.Domain.Entities;

namespace GeoQuiz.Backend.Application.DTOs.KingOfTheHill;
public class LobbyInitialStateData
{
    public Guid LobbyId { get; set; }
    public List<PlayerLobby> Players { get; set; } = new();
    public int TotalPlayers { get; set; }
}
