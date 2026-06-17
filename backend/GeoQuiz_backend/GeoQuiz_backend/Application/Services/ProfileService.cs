using GeoQuiz_backend.Application.DTOs.User;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.CodeAnalysis;
using Microsoft.EntityFrameworkCore;


namespace GeoQuiz_backend.Application.Services
{
    public class ProfileService : IProfileService
    {
        private readonly IAchievementProgressService _progressService;
        private readonly IAchievementService _achievementService;
        private readonly IServiceScopeFactory _serviceScopeFactory;
        private readonly AppDbContext _db;
        private readonly ILogger<ProfileService> _logger;
        public ProfileService(IAchievementProgressService progressService,
            AppDbContext db,
            IServiceScopeFactory serviceScopeFactory,
            IAchievementService achievementService,
            ILogger<ProfileService> logger)
        {
            _progressService = progressService;
            _db = db;
            _serviceScopeFactory = serviceScopeFactory;
            _achievementService = achievementService;
            _logger = logger;
        }
        public async Task<object?> GetProfile(Guid userId)
        {
            await LazyCheckNeeded(userId);

            var profile = await LoadProfile(userId);
            return profile;
        }
        public async Task<LeaderboardDto> GetLeaderboard(Guid userId, int pageSize = 100)
        {
            var userScore = await _db.Users
                .Where(u => u.Id == userId && u.Stats != null)
                .Select(u => u.Stats.Score)
                .FirstOrDefaultAsync();

            var userRank = await _db.Users
                .Where(u => u.Stats != null && u.Stats.Score > userScore)
                .CountAsync() + 1;

            var leaderboardEntries = await _db.Users
                .Where(u => u.Stats != null)
                .OrderByDescending(u => u.Stats.Score)
                .Take(pageSize)
                .Select(u => new LeaderboardEntry
                {
                    playerId = u.Id,
                    playerName = u.UserName,
                    level = u.Stats.Level,
                    totalScore = u.Stats.Score
                })
                .ToListAsync();

            for (int i = 0; i < leaderboardEntries.Count; i++)
            {
                leaderboardEntries[i].rank = i + 1;
            }
            var userEntry = leaderboardEntries.FirstOrDefault(e => e.playerId == userId);
            var displayRank = userEntry?.rank ?? userRank;

            return new LeaderboardDto
            {
                LeaderboardEntries = leaderboardEntries,
                yourRank = displayRank,
                yourScore = userScore
            };
        }
        public async Task UpdateProfile(Guid userId, UpdateProfileRequest request)
        {
            var user = await _db.Users.FindAsync(userId);
            if (user == null) return;

            if (!string.IsNullOrEmpty(request.Username))
                user.UserName = request.Username;

            await _db.SaveChangesAsync();
        }
        private async Task LazyCheckNeeded(Guid userId)
        {
            var lastSync = await _db.UserStats
                .Where(s => s.UserId == userId)
                .Select(s => s.LastAchievementSync)
                .FirstOrDefaultAsync();

            bool needSync = lastSync == DateTime.MinValue || DateTime.UtcNow - lastSync >= TimeSpan.FromDays(1);

            var userStats = await _db.UserStats
                .FirstOrDefaultAsync(s => s.UserId == userId);

            if (userStats != null)
            {
                var today = DateTime.UtcNow.Date;
                var lastLoginDate = userStats.LastLoginDate.Date;

                if (lastLoginDate != today)
                {
                    if (lastLoginDate == today.AddDays(-1))
                        userStats.DailyLoginStreak++;
                    else
                        userStats.DailyLoginStreak = 1;
                }
                userStats.LastLoginDate = DateTime.UtcNow;
                await _db.SaveChangesAsync();
            }

            if (needSync && userStats != null)
            {
                using var scope = _serviceScopeFactory.CreateScope();
                var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
                var stats = await db.UserStats.FirstAsync(s => s.UserId == userId);

                await _achievementService.CheckAndGrantMissingAchievements(db, userId, stats);

                stats.LastAchievementSync = DateTime.UtcNow;
                await db.SaveChangesAsync();
            }
        }
        private async Task<object?> LoadProfile(Guid userId)
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
                        u.Stats.Experience,
                        u.Stats.Score
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
                        UserId = userId,
                        x.Code,
                        x.Progress,
                        Rarity = (int)(level?.Rarity ?? x.Rarity),
                        x.IsUnlocked,
                        UnlockedAt = x.UnlockedAt != null ? x.UnlockedAt : (DateTime?)null
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
        
    }
}
