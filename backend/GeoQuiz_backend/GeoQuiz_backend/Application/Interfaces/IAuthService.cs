using GeoQuiz_backend.DTOs.Auth;
namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IAuthService
    {
        Task RegisterAsync(RegisterRequest request);
        Task<string> LoginAsync(LoginRequest request);

    }
}
