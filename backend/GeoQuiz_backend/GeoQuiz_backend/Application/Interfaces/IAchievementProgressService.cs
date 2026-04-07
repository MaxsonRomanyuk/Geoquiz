using static GeoQuiz_backend.Application.Services.Achievement.AchievementProgressService;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IAchievementProgressService
    {
        List<ProgressLevel> GetProgressLevels(string code);
        ProgressLevel? GetCurrentProgressLevel(string code, int currentValue);
        ProgressLevel? GetNextProgressLevel(string code, int currentValue);
        List<ProgressLevel> GetNewlyReachedLevels(string code, int oldValue, int newValue);
    }
}
