using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.DTOs.Auth
{
    public class RegisterRequest
    {
        public string UserName { get; set; } = null!;
        public string Email { get; set; } = null!;
        public string Password { get; set; } = null!;
        public UserStatsDto? Stats { get; set; } 
    }
}
