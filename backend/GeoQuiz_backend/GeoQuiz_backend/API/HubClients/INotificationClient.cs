using GeoQuiz_backend.Application.DTOs.User;

namespace GeoQuiz_backend.API.HubClients
{
    public interface INotificationClient
    {
        Task AchievementUnlocked(AchievementUnlockedMessage data);
    }
}
