using GeoQuiz_backend.Application.DTOs.User;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IProfileService
    {
        Task<object?> GetProfile(Guid userId);
        Task UpdateProfile(Guid userId, UpdateProfileRequest request);
    }
}
