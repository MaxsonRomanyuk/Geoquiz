namespace GeoQuiz_backend.Application.DTOs.Auth
{
    public class RefreshTokenRequest
    {
        public string RefreshToken { get; set; } = null!;
        public string DeviceId { get; set; } = null!;
    }
}
