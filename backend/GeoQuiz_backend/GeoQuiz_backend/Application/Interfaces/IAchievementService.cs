using GeoQuiz_backend.Application.Services.Achievement;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IAchievementService
    {
        Task CheckAndGrantAsync(Guid userId, UserStats oldStats, UserStats newStats, GameSession? session = null);
        Task CheckAndGrantMissingAchievements(AppDbContext db, Guid userId, UserStats newStats);
    }
}
