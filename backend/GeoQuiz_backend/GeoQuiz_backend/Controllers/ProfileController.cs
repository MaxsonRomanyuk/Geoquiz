using GeoQuiz_backend.DTOs.User;
using GeoQuiz_backend.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz_backend.Controllers
{
    [ApiController]
    [Route("api/profile")]
    [Authorize]
    public class ProfileController : ControllerBase
    {
        private readonly AppDbContext _db;
        public ProfileController(AppDbContext db)
        {
            _db = db;
        }
        [HttpGet("me")]
        public async Task<IActionResult> GetProfile()
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );

            var data = await _db.Users
                .Where(u => u.Id == userId)
                .Select(u => new
                {
                    User = new
                    {
                        u.Id,
                        u.UserName,
                        u.Email,
                        RegisteredAt = new DateTimeOffset(u.RegisteredAt.ToUniversalTime())
                    },
                    Stats = new
                    {
                        u.Stats.TotalGamesPlayed,
                        u.Stats.TotalGamesWon,
                        WinRate = u.Stats.TotalGamesPlayed == 0
                            ? 0
                            : Math.Round((double)u.Stats.TotalGamesWon /
                                         u.Stats.TotalGamesPlayed * 100, 1),
                        u.Stats.TotalCorrectAnswers,
                        u.Stats.CurrentWinStreak,
                        u.Stats.MaxWinStreak,
                        u.Stats.DailyLoginStreak,
                        u.Stats.Level,
                        u.Stats.Experience
                    },
                    Geography = new
                    {
                        u.Stats.EuropeCorrect,
                        u.Stats.AsiaCorrect,
                        u.Stats.AfricaCorrect,
                        u.Stats.AmericaCorrect,
                        u.Stats.OceaniaCorrect
                    },
                    GameModes = new
                    {
                        u.Stats.FlagsCorrect,
                        u.Stats.CapitalsCorrect,
                        u.Stats.OutlinesCorrect,
                        u.Stats.LanguagesCorrect
                    },
                    PvP = new
                    {
                        u.Stats.PvPGamesPlayed,
                        u.Stats.PvPGamesWon,
                        WinRate = u.Stats.PvPGamesPlayed == 0
                            ? 0
                            : Math.Round((double)u.Stats.PvPGamesWon /
                                         u.Stats.PvPGamesPlayed * 100, 1),
                        u.Stats.CurrentPvPStreak
                    }
                })
                .FirstOrDefaultAsync();

            if (data == null)
                return NotFound();

            var achievementsRaw = await _db.UserAchievements
                .Where(x => x.UserId == userId && x.IsUnlocked)
                .OrderByDescending(x => x.UnlockedAt)
                .Select(x => new
                {
                    x.Achievement.Code,
                    x.Achievement.Title,
                    x.Achievement.Icon,
                    x.Progress,
                    x.UnlockedAt
                })
                .ToListAsync();

            var achievements = achievementsRaw
                .Select(x => new
                {
                    x.Code,
                    x.Title,
                    x.Icon,
                    x.Progress,
                    UnlockedAt = new DateTimeOffset(x.UnlockedAt).ToUniversalTime()
                })
                .ToList();

            return Ok(new
            {
                data.User,
                data.Stats,
                data.Geography,
                data.GameModes,
                data.PvP,
                Achievements = achievements
            });
        }
        [HttpPut("update")]
        public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileRequest request)
        {
            var userId = Guid.Parse(
                User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue(JwtRegisteredClaimNames.Sub)!
            );

            var user = await _db.Users.FindAsync(userId);
            if (user == null)
                return NotFound();

            if (!string.IsNullOrEmpty(request.Username))
                user.UserName = request.Username;

            //if (!string.IsNullOrEmpty(request.AvatarUrl))
            //    user.AvatarUrl = request.AvatarUrl;

            await _db.SaveChangesAsync();

            return Ok(new { Message = "Profile updated successfully" });
        }
    }
}
