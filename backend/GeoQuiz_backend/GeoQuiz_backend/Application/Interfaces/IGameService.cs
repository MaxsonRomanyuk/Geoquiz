using GeoQuiz_backend.Application.DTOs.Game;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IGameService
    {
        Task ProcessFinishedGameAsync(Guid userId, FinishGameRequest request, DateTime? playedAt = null, Guid? sessionId = null);
        Task SyncGamesAsync(Guid userId, List<SyncGameSessionRequest> games);
    }
}
