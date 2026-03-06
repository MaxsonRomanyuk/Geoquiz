using GeoQuiz.Backend.Application.Services;

namespace GeoQuiz.Backend.Application.Interfaces;
public interface IAchievementService
{
    Task CheckAndGrantAsync(AchievementContext context);
}
