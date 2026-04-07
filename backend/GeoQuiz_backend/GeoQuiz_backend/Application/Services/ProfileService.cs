using GeoQuiz_backend.Application.DTOs.User;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Services.Achievement;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;

namespace GeoQuiz_backend.Application.Services
{
    public class ProfileService : IProfileService
    {
        private readonly IAchievementProgressService _progressService;
        private readonly AppDbContext _db;
        public ProfileService(IAchievementProgressService progressService, AppDbContext db)
        {
            _progressService = progressService;
            _db = db;
        }
        public async Task<object?> GetProfile(Guid userId)
        {
            var data = await _db.Users
                .Where(u => u.Id == userId)
                .Select(u => new
                {
                    User = new
                    {
                        u.Id,
                        u.UserName,
                        u.Email,
                        u.RegisteredAt
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
                return null;

            var achievementsRaw = await _db.Achievements
                .GroupJoin(
                    _db.UserAchievements.Where(ua => ua.UserId == userId),
                    a => a.Id,
                    ua => ua.AchievementId,
                    (a, ua) => new { a, ua = ua.FirstOrDefault() }
                )
                .Select(x => new
                {
                    x.a.Code,
                    x.a.Rarity,
                    Progress = x.ua != null ? x.ua.Progress : 0,
                    IsUnlocked = x.ua != null && x.ua.IsUnlocked,
                    UnlockedAt = x.ua != null ? x.ua.UnlockedAt : (DateTime?)null
                })
                .ToListAsync();

            var achievements = achievementsRaw
                .Select(x =>
                {
                    var level = _progressService.GetCurrentProgressLevel(x.Code, x.Progress);

                    return new
                    {
                        x.Code,
                        x.Progress,
                        Rarity = (int)(level?.Rarity ?? x.Rarity),
                        x.IsUnlocked,
                        UnlockedAt = x.UnlockedAt != null
                            ? new DateTimeOffset(x.UnlockedAt.Value).ToUniversalTime()
                            : (DateTimeOffset?)null
                    };
                })
                .ToList();

            return new
            {
                data.User,
                data.Stats,
                data.Geography,
                data.GameModes,
                Pvp = data.PvP,
                Achievements = achievements
            };
        }
        public async Task UpdateProfile(Guid userId, UpdateProfileRequest request)
        {
            var user = await _db.Users.FindAsync(userId);
            if (user == null) return;

            if (!string.IsNullOrEmpty(request.Username))
                user.UserName = request.Username;

            //if (!string.IsNullOrEmpty(request.AvatarUrl))
            //    user.AvatarUrl = request.AvatarUrl;

            await _db.SaveChangesAsync();
        }
    }
}
