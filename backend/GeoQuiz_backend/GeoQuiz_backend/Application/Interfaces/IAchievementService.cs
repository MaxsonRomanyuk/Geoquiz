using GeoQuiz_backend.Application.Services;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IAchievementService
    {
        Task CheckAndGrantAsync(AchievementContext context);
    }
}
