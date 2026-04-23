using GeoQuiz_backend.Application.DTOs.Auth;
using GeoQuiz_backend.Domain.Entities;
namespace GeoQuiz_backend.Application.Interfaces
{
    public interface IAuthService
    {
        Task RegisterAsync(RegisterRequest request);
        Task<AuthResponse> LoginAsync(LoginRequest request);
        Task<RefreshTokenResponse> RefreshTokensAsync(RefreshTokenRequest request);
        Task LogoutAsync(LogoutRequest request);
        Task RevokeAllUserTokensAsync(Guid userId);
    }
}
