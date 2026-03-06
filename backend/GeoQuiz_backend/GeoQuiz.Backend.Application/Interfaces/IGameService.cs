using GeoQuiz.Backend.Application.DTOs.Game;

namespace GeoQuiz.Backend.Application.Interfaces;
public interface IGameService
{
    Task ProcessFinishedGameAsync(Guid userId, FinishGameRequest request, DateTime? playedAt = null, Guid? sessionId = null);
    Task SyncGamesAsync(Guid userId, List<SyncGameSessionRequest> games);
}
