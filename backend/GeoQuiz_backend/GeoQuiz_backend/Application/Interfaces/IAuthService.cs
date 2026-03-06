using GeoQuiz_backend.Application.DTOs.Auth;
namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IAuthService
    {
        Task RegisterAsync(RegisterRequest request);
        Task<AuthResponse> LoginAsync(LoginRequest request);

    }
}
