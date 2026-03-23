using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using System.IdentityModel.Tokens.Jwt;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using GeoQuiz_backend.Application.DTOs.Auth;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;

namespace GeoQuiz_backend.Application.Services
{
    public class AuthService : IAuthService
    {
        private readonly AppDbContext _db;
        private readonly IConfiguration _config;

        public AuthService(AppDbContext db, IConfiguration config)
        {
            _db = db;
            _config = config;
        }

        public async Task RegisterAsync(RegisterRequest request)
        {
            if (await _db.Users.AnyAsync(u => u.Email == request.Email))
                throw new Exception("User already exists");

            var userId = Guid.NewGuid();

            var user = new User
            {
                Id = userId,
                UserName = request.UserName,
                Email = request.Email,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
                RegisteredAt = DateTime.UtcNow,
                IsPremium = false,
                Stats = request.Stats == null
                    ? new UserStats
                    {
                        UserId = userId,
                        LastLoginDate = DateTime.UtcNow
                    }
                    : new UserStats
                    {
                        UserId = userId,
                        Level = request.Stats.Level,
                        Experience = request.Stats.Experience,
                        TotalGamesPlayed = request.Stats.GamesPlayed,
                        TotalGamesWon = request.Stats.GamesWon,
                        CurrentWinStreak = request.Stats.WinStreak,
                        MaxWinStreak = request.Stats.WinStreak,
                        DailyLoginStreak = request.Stats.DailyStreak,
                        LastLoginDate = DateTime.UtcNow,
                        EuropeCorrect = request.Stats.EuropeCorrect,
                        AsiaCorrect = request.Stats.AsiaCorrect,
                        AfricaCorrect = request.Stats.AfricaCorrect,
                        AmericaCorrect = request.Stats.AmericaCorrect,
                        OceaniaCorrect = request.Stats.OceaniaCorrect,
                        FlagsCorrect = request.Stats.FlagsCorrect,
                        CapitalsCorrect = request.Stats.CapitalsCorrect,
                        OutlinesCorrect = request.Stats.OutlinesCorrect,
                        LanguagesCorrect = request.Stats.LanguagesCorrect,
                        TotalCorrectAnswers = 0,
                        TotalQuickAnswers = 0,
                        TotalLastSecondWins = 0,
                        PvPGamesPlayed = 0,
                        PvPGamesWon = 0,
                        CurrentPvPStreak = 0,
                        KothGamesPlayed = 0,
                        KothGamesWon = 0,
                        KothTop3Finishes = 0
                    }
            };
            _db.Users.Add(user);
            await _db.SaveChangesAsync();
        }

        public async Task<AuthResponse> LoginAsync(LoginRequest request)
        {
            var user = await _db.Users
                .FirstOrDefaultAsync(u => u.Email == request.Email);
            var userId = user?.Id;
            var userName = user?.UserName;

            if (user == null ||
                !BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
                throw new Exception("Invalid credentials");
            return new AuthResponse { Token = GenerateJwt(user), UserId = (Guid)userId, UserName = userName };
        }

        private string GenerateJwt(User user)
        {
            var jwt = _config.GetSection("Jwt");

            var claims = new[]
            {
                new Claim(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
                new Claim(JwtRegisteredClaimNames.Email, user.Email),
                new Claim("username", user.UserName)
            };

            var key = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(jwt["Key"]!)
            );

            var token = new JwtSecurityToken(
                issuer: jwt["Issuer"],
                audience: jwt["Audience"],
                claims: claims,
                expires: DateTime.UtcNow.AddMinutes(
                    int.Parse(jwt["ExpiresMinutes"]!)
                ),
                signingCredentials:
                    new SigningCredentials(key, SecurityAlgorithms.HmacSha256)
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}
