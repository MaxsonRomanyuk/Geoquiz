using GeoQuiz_backend.Domain.Entities;
using System.Security.Claims;

namespace GeoQuiz_backend.Application.Interfaces
{
    public interface ITokenService
    {
        string GenerateAccessToken(User user);
        string GenerateRefreshToken();
        string ComputeSha256Hash(string rawData);
    }
}
