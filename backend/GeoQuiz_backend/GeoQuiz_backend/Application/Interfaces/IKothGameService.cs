using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Payloads;
using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IKothGameService
    {
        Task<KothMatch> StartMatchFromLobbyAsync(Guid lobbyId, List<(Guid UserId, string UserName, int Level)> players);
        Task<RoundStartedData?> StartNextRoundAsync(Guid matchId);
        Task<AnswerResultData> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request);
        Task<RoundFinishedData> FinishRoundAsync(Guid matchId);
        Task<MatchFinishedData> FinishMatchAsync(Guid matchId);
        Task<KothGameState?> GetGameStateAsync(Guid matchId);
    }
}
