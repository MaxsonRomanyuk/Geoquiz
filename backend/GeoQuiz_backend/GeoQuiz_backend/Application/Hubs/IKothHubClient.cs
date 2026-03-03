using GeoQuiz_backend.DTOs.KingOfTheHill;

namespace GeoQuiz_backend.Application.Hubs
{
    public interface IKothHubClient
    {
        Task PlayerJoined(PlayerJoinedData data);
        Task PlayerLeft(PlayerLeftData data);
        Task LobbyCountdown(int secondsRemaining);
        Task LobbyCountdownCancelled();

    }
}
