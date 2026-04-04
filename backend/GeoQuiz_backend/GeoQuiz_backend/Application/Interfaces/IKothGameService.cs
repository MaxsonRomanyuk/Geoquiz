using GeoQuiz_backend.Application.DTOs.KingOfTheHill;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IKothGameService
    {
        Task StartMatchFromLobbyAsync(List<PlayerInfo> realPlayers, Guid lobbyId);
        Task StartNextRoundAsync(Guid matchId);
        Task<AnswerResultData> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request);
        Task LeaveMatchAsync(Guid userId, Guid matchId);
    }
}
