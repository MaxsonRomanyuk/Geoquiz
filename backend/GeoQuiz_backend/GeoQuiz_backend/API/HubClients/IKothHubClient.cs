using GeoQuiz_backend.Application.DTOs.KingOfTheHill;

namespace GeoQuiz_backend.API.HubClients
{
    public interface IKothHubClient
    {
        Task PlayerJoinedToOthers(PlayerJoinedData data);
        Task PlayerAboutLobby(LobbyInitialStateData data);
        Task PlayerLeft(PlayerLeftData data);
        Task LobbyCountdown(int secondsRemaining);
        Task LobbyCountdownCancelled();

        Task MatchStarted(MatchStartedData data);
        Task RoundStarted(RoundStartedData data);
        Task RoundFinished(RoundFinishedData data);
        Task PlayerEliminated(PlayerEliminatedData data);
        Task MatchFinished(MatchFinishedData data);
        Task AnswerResult(AnswerResultData data);
    }
}
