using GeoQuiz_backend.DTOs.KingOfTheHill;
using static System.Runtime.InteropServices.JavaScript.JSType;

namespace GeoQuiz_backend.Application.Hubs
{
    public interface IKothHubClient
    {
        Task PlayerJoinedToOthers(PlayerJoinedData data);
        Task PlayerAboutLobby(LobbyInitialStateData data);
        Task PlayerLeft(PlayerLeftData data);
        Task LobbyCountdown(int secondsRemaining);
        Task LobbyCountdownCancelled();

    }
}
