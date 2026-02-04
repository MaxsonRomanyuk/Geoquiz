using GeoQuiz_backend.DTOs.Game;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IGameService
    {
        Task ProcessFinishedGameAsync(Guid userId, FinishGameRequest request, Guid? sessionId = null);
        Task SyncGamesAsync(Guid userId, List<SyncGameSessionRequest> games);
    }
}
