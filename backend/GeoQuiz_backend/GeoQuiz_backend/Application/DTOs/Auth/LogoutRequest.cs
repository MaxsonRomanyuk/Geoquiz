namespace GeoQuiz_backend.Application.DTOs.Auth
{
    public class LogoutRequest
    {
        public string RefreshToken { get; set; } = null!;
    }
}
