using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Payloads.Koth;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IKothGameService
    {
        Task StartMatchFromLobbyAsync(List<PlayerInfo> realPlayers, Guid lobbyId);
        Task StartNextRoundAsync(Guid matchId);
        Task<AnswerResultData> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request);
        Task LeaveMatchAsync(Guid userId, Guid matchId);
        Task<KothGameState?> GetGameStateAsync(Guid matchId);
        DateTime? GetRoundTimerEndsAt(Guid matchId);
    }
}
