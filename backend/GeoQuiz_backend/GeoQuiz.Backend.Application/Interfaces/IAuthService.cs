using GeoQuiz.Backend.Application.DTOs.Auth;
namespace GeoQuiz.Backend.Application.Interfaces;
public interface IAuthService
{
    Task RegisterAsync(RegisterRequest request);
    Task<AuthResponse> LoginAsync(LoginRequest request);
}
