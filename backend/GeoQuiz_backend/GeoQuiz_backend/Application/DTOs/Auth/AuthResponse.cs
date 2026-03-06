namespace GeoQuiz_backend.Application.DTOs.Auth
{
    public class AuthResponse
    {
        public string Token { get; set; } = null!;
        public Guid UserId { get; set; }
        public string UserName { get; set; } = null!;
    }
}
