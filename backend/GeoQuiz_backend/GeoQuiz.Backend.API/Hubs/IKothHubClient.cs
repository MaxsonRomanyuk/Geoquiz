using GeoQuiz.Backend.Application.DTOs.KingOfTheHill;

namespace GeoQuiz.Backend.API.Hubs;

public interface IKothHubClient
{
    Task PlayerJoinedToOthers(PlayerJoinedData data);
    Task PlayerAboutLobby(LobbyInitialStateData data);
    Task PlayerLeft(PlayerLeftData data);
    Task LobbyCountdown(int secondsRemaining);
    Task LobbyCountdownCancelled();
}