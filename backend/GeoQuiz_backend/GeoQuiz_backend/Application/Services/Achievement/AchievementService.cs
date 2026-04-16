using GeoQuiz_backend.Application.DTOs.User;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Services.Achievement;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;

namespace GeoQuiz_backend.Application.Services
{
    public class AchievementService : IAchievementService
    {
        private readonly AppDbContext _db;
        private readonly ILogger<AchievementService> _logger;

        private readonly IAchievementProgressService _progressService;
        private readonly ISignalRNotificationService _notificationService;
        private readonly Dictionary<string, Func<UserStats, int>> _statSelectors = new()
        {
            ["GAMES_PLAYED"] = s => s.TotalGamesPlayed,
            ["GAMES_WON"] = s => s.TotalGamesWon,
            ["FLAGS"] = s => s.FlagsCorrect,
            ["CAPITALS"] = s => s.CapitalsCorrect,
            ["OUTLINES"] = s => s.OutlinesCorrect,
            ["LANGUAGES"] = s => s.LanguagesCorrect,
            ["EUROPE"] = s => s.EuropeCorrect,
            ["ASIA"] = s => s.AsiaCorrect,
            ["AFRICA"] = s => s.AfricaCorrect,
            ["AMERICA"] = s => s.AmericaCorrect,
            ["OCEANIA"] = s => s.OceaniaCorrect,
            ["WIN_STREAK"] = s => s.MaxWinStreak,
            ["DAILY_LOGIN"] = s => s.DailyLoginStreak,
            ["PVP_GAMES_PLAYED"] = s => s.PvPGamesPlayed,
            ["PVP_GAMES_WON"] = s => s.PvPGamesWon,
            ["PVP_WIN_STREAK"] = s => s.CurrentPvPStreak,
            ["KOTH_GAMES_PLAYED"] = s => s.KothGamesPlayed,
            ["KOTH_GAMES_WON"] = s => s.KothGamesWon,
            ["KOTH_TOP3"] = s => s.KothTop3Finishes,
            ["TOTAL_CORRECT"] = s => s.TotalCorrectAnswers,
            ["LEVEL"] = s => s.Level
        };

        public AchievementService(AppDbContext db,
            ILogger<AchievementService> logger,
            ISignalRNotificationService notificationService,
            IAchievementProgressService progressService)
        {
            _db = db;
            _logger = logger;
            _notificationService = notificationService;
            _progressService = progressService;
        }
        public async Task CheckAndGrantAsync(Guid userId, UserStats oldStats, UserStats newStats, GameSession? session = null)
        {
            var unlockedAchievements = new List<AchievementDto>();

            await HandleProgressAchievements(userId, oldStats, newStats, unlockedAchievements);
            await HandleSingleAchievements(userId, oldStats, newStats, unlockedAchievements);
            await HandleCompositeAchievements(userId, newStats, unlockedAchievements);
            await HandleSessionBasedAchievements(userId, session, unlockedAchievements);

            if (unlockedAchievements.Any())
            {
                AchievementUnlockedMessage achievementUnlocked = new AchievementUnlockedMessage(unlockedAchievements);
                await _notificationService.NotifyAchievementUnlocked(userId, achievementUnlocked);
                _logger.LogError("achievement unlocked for {userId} at {date}", userId, DateTime.UtcNow.ToString());
            }
            //else
            //{
            //    var test = new List<AchievementDto>();
            //    test.Add(new AchievementDto
            //    {
            //        UserId = userId,
            //        Code = "DAILY_LOGIN",
            //        Progress = 25,
            //        Rarity = 4,
            //        IsUnlocked = true,
            //        UnlockedAt = DateTime.UtcNow,
            //    });
            //    AchievementUnlockedMessage achievementUnlocked = new AchievementUnlockedMessage(test);
            //    await _notificationService.NotifyAchievementUnlocked(userId, achievementUnlocked);
            //    _logger.LogError("TEST FAKE NOTIFY  for {userId} at {date}", userId, DateTime.UtcNow.ToString());
            //}
        }
        public async Task CheckAndGrantAsync(AppDbContext db, Guid userId, UserStats oldStats, UserStats newStats, GameSession? session = null)
        {
            var unlockedAchievements = new List<AchievementDto>();

            await HandleProgressAchievements(db, userId, oldStats, newStats, unlockedAchievements);
            await HandleSingleAchievements(db, userId, oldStats, newStats, unlockedAchievements);
            await HandleCompositeAchievements(db, userId, newStats, unlockedAchievements);
            await HandleSessionBasedAchievements(db, userId, session, unlockedAchievements);

            if (unlockedAchievements.Any())
            {
                var achievementUnlocked = new AchievementUnlockedMessage(unlockedAchievements);
                _logger.LogError("achievement unlocked for {userId} at {date}", userId, DateTime.UtcNow.ToString());
                await _notificationService.NotifyAchievementUnlocked(userId, achievementUnlocked);
            }
            //else
            //{
            //    var test = new List<AchievementDto>();
            //    test.Add(new AchievementDto
            //    {
            //        UserId = userId,
            //        Code = "DAILY_LOGIN",
            //        Progress = 25,
            //        Rarity = 4,
            //        IsUnlocked = true,
            //        UnlockedAt = DateTime.UtcNow,
            //    });
            //    AchievementUnlockedMessage achievementUnlocked = new AchievementUnlockedMessage(test);
            //    await _notificationService.NotifyAchievementUnlocked(userId, achievementUnlocked);
            //    _logger.LogError("TEST FAKE NOTIFY  for {userId} at {date}", userId, DateTime.UtcNow.ToString());
            //}
        }
        public async Task CheckAndGrantMissingAchievements(AppDbContext db, Guid userId, UserStats newStats)
        {
            var unlockedAchievements = new List<AchievementDto>();

            await HandleProgressAchievements(db, userId, newStats, unlockedAchievements);
            await HandleSingleAchievements(db, userId, newStats, unlockedAchievements);
            await HandleCompositeAchievements(db, userId, newStats, unlockedAchievements);

            if (unlockedAchievements.Any())
            {
                _logger.LogError("CheckAndGrantMissingAchievements  for {userId} at {date}", userId, DateTime.UtcNow.ToString());
                AchievementUnlockedMessage achievementUnlocked = new AchievementUnlockedMessage(unlockedAchievements);
                await _notificationService.NotifyAchievementUnlocked(userId, achievementUnlocked);
            }
        }
        private async Task HandleProgressAchievements(Guid userId, UserStats oldStats, UserStats newStats, List<AchievementDto> unlockedAchievements)
        {
            foreach (var (code, selector) in _statSelectors)
            {
                var oldVal = selector(oldStats);
                var newVal = selector(newStats);

                var levels = _progressService.GetNewlyReachedLevels(code, oldVal, newVal);

                foreach (var level in levels)
                {
                    await Unlock(userId, code, unlockedAchievements, level.Target, level.Title, level.Rarity);
                }
            }
        }
        private async Task HandleProgressAchievements(AppDbContext db, Guid userId, UserStats oldStats, UserStats newStats, List<AchievementDto> unlockedAchievements)
        {
            foreach (var (code, selector) in _statSelectors)
            {
                var oldVal = selector(oldStats);
                var newVal = selector(newStats);

                var levels = _progressService.GetNewlyReachedLevels(code, oldVal, newVal);

                foreach (var level in levels)
                {
                    await UnlockDirect(db, userId, code, unlockedAchievements, level.Target, level.Title, level.Rarity);
                }
            }
        }
        private async Task HandleProgressAchievements(AppDbContext db ,Guid userId, UserStats currentStats, List<AchievementDto> unlockedAchievements)
        {
            foreach (var (code, selector) in _statSelectors)
            {
                var currentVal = selector(currentStats);

                var lastLevel = _progressService.GetCurrentProgressLevel(code, currentVal);

                if (lastLevel != null) await UnlockDirect(db, userId, code, unlockedAchievements, lastLevel.Target, lastLevel.Title, lastLevel.Rarity);
            }
        }
        private async Task HandleSingleAchievements(Guid userId, UserStats oldStats, UserStats newStats, List<AchievementDto> unlockedAchievements)
        {
            if (oldStats.TotalGamesPlayed < 1 && newStats.TotalGamesPlayed >= 1)
                await Unlock(userId, "FIRST_GAME", unlockedAchievements);

            if (oldStats.TotalGamesWon < 1 && newStats.TotalGamesWon >= 1)
                await Unlock(userId, "FIRST_WIN", unlockedAchievements);

            if (oldStats.PvPGamesPlayed < 1 && newStats.PvPGamesPlayed >= 1)
                await Unlock(userId, "PVP_FIRST_GAME", unlockedAchievements);

            if (oldStats.PvPGamesWon < 1 && newStats.PvPGamesWon >= 1)
                await Unlock(userId, "PVP_FIRST_WIN", unlockedAchievements);

            if (oldStats.KothGamesPlayed < 1 && newStats.KothGamesPlayed >= 1)
                await Unlock(userId, "KOTH_FIRST_GAME", unlockedAchievements);

            if (oldStats.KothGamesWon < 1 && newStats.KothGamesWon >= 1)
                await Unlock(userId, "KOTH_FIRST_WIN", unlockedAchievements);
        }
        private async Task HandleSingleAchievements(AppDbContext db, Guid userId, UserStats oldStats, UserStats newStats, List<AchievementDto> unlockedAchievements)
        {
            if (oldStats.TotalGamesPlayed < 1 && newStats.TotalGamesPlayed >= 1)
                await UnlockDirect(db, userId, "FIRST_GAME", unlockedAchievements);

            if (oldStats.TotalGamesWon < 1 && newStats.TotalGamesWon >= 1)
                await UnlockDirect(db, userId, "FIRST_WIN", unlockedAchievements);

            if (oldStats.PvPGamesPlayed < 1 && newStats.PvPGamesPlayed >= 1)
                await UnlockDirect(db, userId, "PVP_FIRST_GAME", unlockedAchievements);

            if (oldStats.PvPGamesWon < 1 && newStats.PvPGamesWon >= 1)
                await UnlockDirect(db, userId, "PVP_FIRST_WIN", unlockedAchievements);

            if (oldStats.KothGamesPlayed < 1 && newStats.KothGamesPlayed >= 1)
                await UnlockDirect(db, userId, "KOTH_FIRST_GAME", unlockedAchievements);

            if (oldStats.KothGamesWon < 1 && newStats.KothGamesWon >= 1)
                await UnlockDirect(db, userId, "KOTH_FIRST_WIN", unlockedAchievements);
        }
        private async Task HandleSingleAchievements(AppDbContext db, Guid userId, UserStats currentStats, List<AchievementDto> unlockedAchievements)
        {
            if (currentStats.TotalGamesPlayed >= 1)
                await UnlockDirect(db, userId, "FIRST_GAME", unlockedAchievements);

            if (currentStats.TotalGamesWon >= 1)
                await UnlockDirect(db, userId, "FIRST_WIN", unlockedAchievements);

            if (currentStats.PvPGamesPlayed >= 1)
                await UnlockDirect(db, userId, "PVP_FIRST_GAME", unlockedAchievements);

            if (currentStats.PvPGamesWon >= 1)
                await UnlockDirect(db, userId, "PVP_FIRST_WIN", unlockedAchievements);

            if (currentStats.KothGamesPlayed >= 1)
                await UnlockDirect(db, userId, "KOTH_FIRST_GAME", unlockedAchievements);

            if (currentStats.KothGamesWon >= 1)
                await UnlockDirect(db, userId, "KOTH_FIRST_WIN", unlockedAchievements);
        }
        private async Task HandleCompositeAchievements(Guid userId, UserStats s, List<AchievementDto> unlockedAchievements)
        {
            if (s.EuropeCorrect >= 1000 &&
                s.AsiaCorrect >= 1000 &&
                s.AfricaCorrect >= 1000 &&
                s.AmericaCorrect >= 1000 &&
                s.OceaniaCorrect >= 1000)
            {
                await Unlock(userId, "WORLD_TRAVELER", unlockedAchievements , 1000);
            }

            if (s.FlagsCorrect >= 1000 &&
                s.CapitalsCorrect >= 1000 &&
                s.OutlinesCorrect >= 1000 &&
                s.LanguagesCorrect >= 1000)
            {
                await Unlock(userId, "ALL_ROUNDER", unlockedAchievements ,1000);
            }
        }
        private async Task HandleCompositeAchievements(AppDbContext db, Guid userId, UserStats s, List<AchievementDto> unlockedAchievements)
        {
            if (s.EuropeCorrect >= 1000 &&
                s.AsiaCorrect >= 1000 &&
                s.AfricaCorrect >= 1000 &&
                s.AmericaCorrect >= 1000 &&
                s.OceaniaCorrect >= 1000)
            {
                await UnlockDirect(db, userId, "WORLD_TRAVELER", unlockedAchievements, 1000);
            }

            if (s.FlagsCorrect >= 1000 &&
                s.CapitalsCorrect >= 1000 &&
                s.OutlinesCorrect >= 1000 &&
                s.LanguagesCorrect >= 1000)
            {
                await UnlockDirect(db, userId, "ALL_ROUNDER", unlockedAchievements, 1000);
            }
        }
        private async Task HandleSessionBasedAchievements(AppDbContext db, Guid userId, GameSession? session, List<AchievementDto> unlockedAchievements)
        {
            if (session == null) return;

            if (session.CorrectAnswers == session.TotalQuestions && session.TotalQuestions > 0)
            {
                await UnlockDirect(db, userId, "PERFECT_GAME", unlockedAchievements);
            }
            if (session.PvPMatch != null && session.CorrectAnswers == session.TotalQuestions && session.TotalQuestions > 0)
            {
                await UnlockDirect(db, userId, "PERFECT_GAME_PVP", unlockedAchievements);
            }
        }
        private async Task HandleSessionBasedAchievements(Guid userId, GameSession? session, List<AchievementDto> unlockedAchievements)
        {
            if (session == null) return;

            if (session.CorrectAnswers == session.TotalQuestions && session.TotalQuestions > 0)
            {
                await Unlock(userId, "PERFECT_GAME", unlockedAchievements);
            }
            if (session.PvPMatch != null && session.CorrectAnswers == session.TotalQuestions && session.TotalQuestions > 0)
            {
                await Unlock(userId, "PERFECT_GAME_PVP", unlockedAchievements);
            }
            
            // PVP_UNDERDOG
        }
        private async Task Unlock(Guid userId, string code, List<AchievementDto> unlockedAchievements,
            int progress = 1, string? title = null, AchievementRarity? rarity = null)
        {
            var achievement = await _db.Achievements.FirstOrDefaultAsync(a => a.Code == code);
            if (achievement == null) return;

            var existing = await _db.UserAchievements.FirstOrDefaultAsync(x => x.UserId == userId && x.AchievementId == achievement.Id);

            if (existing != null)
            {
                if (existing.Progress == progress) return;

                existing.Progress = progress;
            }

            else
            {
                existing = new UserAchievement
                {
                    UserId = userId,
                    AchievementId = achievement.Id,
                    Progress = progress,
                    IsUnlocked = true,
                    UnlockedAt = DateTime.UtcNow
                };
                _db.UserAchievements.Add(existing);
            }

            await _db.SaveChangesAsync();

            unlockedAchievements.Add(new AchievementDto
            {
                UserId = userId,
                Code = code,
                Progress = progress,
                Rarity = (int)(rarity ?? achievement.Rarity),
                IsUnlocked = true,
                UnlockedAt = DateTime.UtcNow
            });

        }
        public async Task UnlockDirect(AppDbContext db, Guid userId, string code, List<AchievementDto> unlockedAchievements,
            int progress = 1, string? title = null, AchievementRarity? rarity = null)
        {
            var achievement = await db.Achievements.FirstOrDefaultAsync(a => a.Code == code);
            if (achievement == null) return;

            var existing = await db.UserAchievements
                .FirstOrDefaultAsync(x => x.UserId == userId && x.AchievementId == achievement.Id);

            if (existing != null)
            {
                if (existing.Progress >= progress) return;

                existing.Progress = progress;
            }
            else
            {
                existing = new UserAchievement
                {
                    UserId = userId,
                    AchievementId = achievement.Id,
                    Progress = progress,
                    IsUnlocked = true,
                    UnlockedAt = DateTime.UtcNow
                };
                db.UserAchievements.Add(existing);
            }

            await db.SaveChangesAsync();

            unlockedAchievements.Add(new AchievementDto
            {
                UserId = userId,
                Code = code,
                Progress = progress,
                Rarity = (int)(rarity ?? achievement.Rarity),
                IsUnlocked = true,
                UnlockedAt = DateTime.UtcNow
            });
        }
    }
}
