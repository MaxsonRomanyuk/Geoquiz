using GeoQuiz_backend.Application.Services.Achievement;
using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IAchievementService
    {
        Task CheckAndGrantAsync(Guid userId, UserStats oldStats, UserStats newStats, GameSession? session = null);
    }
}
