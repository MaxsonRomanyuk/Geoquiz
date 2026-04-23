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
        private readonly ITokenService _tokenService;

        public AuthService(AppDbContext db, ITokenService tokenService)
        {
            _db = db;
            _tokenService = tokenService;
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
                        KothTop3Finishes = 0,
                        LastLoginDate = DateTime.UtcNow,
                        LastAchievementSync = new DateTime()
                    }
            };
            _db.Users.Add(user);
            await _db.SaveChangesAsync();
        }

        public async Task<AuthResponse> LoginAsync(LoginRequest request)
        {
            var user = await _db.Users
                .FirstOrDefaultAsync(u => u.Email == request.Email);

            if (user == null || !BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
                throw new Exception("Invalid credentials");

            var accessToken = _tokenService.GenerateAccessToken(user);
            var refreshToken = _tokenService.GenerateRefreshToken();

            var hashedToken = _tokenService.ComputeSha256Hash(refreshToken);

            var oldTokens = await _db.RefreshTokens
                .Where(rt => rt.UserId == user.Id && rt.DeviceId == request.DeviceId && !rt.IsRevoked)
                .ToListAsync();
            foreach (var oldToken in oldTokens)
            {
                oldToken.IsRevoked = true;
                oldToken.RevokedAt = DateTime.UtcNow;
            }

            var newRefreshToken = new RefreshToken
            {
                UserId = user.Id,
                TokenHash = hashedToken,
                DeviceId = request.DeviceId,
                ExpiryDate = DateTime.UtcNow.AddDays(30), 
                CreatedAt = DateTime.UtcNow,
                IsRevoked = false
            };

            _db.RefreshTokens.Add(newRefreshToken);
            await _db.SaveChangesAsync();

            if (user.Stats != null)
            {
                user.Stats.LastLoginDate = DateTime.UtcNow;
                await _db.SaveChangesAsync();
            }

            return new AuthResponse
            {
                AccessToken = accessToken,
                RefreshToken = refreshToken,
                UserId = user.Id,
                UserName = user.UserName,
                ExpiresIn = 1800 
            };
        }

        public async Task<RefreshTokenResponse> RefreshTokensAsync(RefreshTokenRequest request)
        {
            var hashedToken = _tokenService.ComputeSha256Hash(request.RefreshToken);
            var dbToken = await _db.RefreshTokens
                .Include(rt => rt.User)
                .FirstOrDefaultAsync(rt => rt.TokenHash == hashedToken);

            if (dbToken == null)
                throw new Exception("Invalid refresh token");

            if (dbToken.IsRevoked)
                throw new Exception("Token has been revoked");

            if (dbToken.ExpiryDate < DateTime.UtcNow)
                throw new Exception("Refresh token expired");

            if (dbToken.DeviceId != request.DeviceId)
                throw new Exception("Device ID mismatch");

            dbToken.IsRevoked = true;
            dbToken.RevokedAt = DateTime.UtcNow;

            var newAccessToken = _tokenService.GenerateAccessToken(dbToken.User);
            var newRefreshToken = _tokenService.GenerateRefreshToken();

            var newHashedToken = _tokenService.ComputeSha256Hash(newRefreshToken);
            var newDbToken = new RefreshToken
            {
                UserId = dbToken.UserId,
                TokenHash = newHashedToken,
                DeviceId = request.DeviceId,
                ExpiryDate = DateTime.UtcNow.AddDays(30),
                CreatedAt = DateTime.UtcNow,
                IsRevoked = false
            };

            _db.RefreshTokens.Add(newDbToken);
            await _db.SaveChangesAsync();

            return new RefreshTokenResponse
            {
                AccessToken = newAccessToken,
                RefreshToken = newRefreshToken,
                ExpiresIn = 1800
            };
        }
        public async Task LogoutAsync(LogoutRequest request)
        {
            var hashedToken = _tokenService.ComputeSha256Hash(request.RefreshToken);
            var dbToken = await _db.RefreshTokens
                .FirstOrDefaultAsync(rt => rt.TokenHash == hashedToken);

            if (dbToken != null && !dbToken.IsRevoked)
            {
                dbToken.IsRevoked = true;
                dbToken.RevokedAt = DateTime.UtcNow;
                await _db.SaveChangesAsync();
            }
        }
        public async Task RevokeAllUserTokensAsync(Guid userId)
        {
            var tokens = await _db.RefreshTokens
                .Where(rt => rt.UserId == userId && !rt.IsRevoked)
                .ToListAsync();

            foreach (var token in tokens)
            {
                token.IsRevoked = true;
                token.RevokedAt = DateTime.UtcNow;
            }

            await _db.SaveChangesAsync();
        }
    }
}
