namespace GeoQuiz_backend.Application.DTOs.Auth
{
    public class LoginRequest
    {
        public string Email { get; set; } = null!;
        public string Password { get; set; } = null!;
        public string DeviceId { get; set; } = null!;
    }
}
