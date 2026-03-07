using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using static GeoQuiz_backend.Application.Payloads.AchievementsPayloads;

namespace GeoQuiz_backend.Application.Services
{
    public class AchievementService : IAchievementService
    {
        private readonly AppDbContext _db;
        private readonly ILogger<AchievementService> _logger;

        private readonly Dictionary<string, AchievementRule> _rules;

        public AchievementService(AppDbContext db, ILogger<AchievementService> logger)
        {
            _db = db;
            _logger = logger;

            _rules = new()
            {
                ["FIRST_GAME"] = new(ctx => ctx.Stats.TotalGamesPlayed, new[] { 1 }),
                ["GAMES_PLAYED"] = new(ctx => ctx.Stats.TotalGamesPlayed, new[] { 10, 50, 100, 500, 1000 }),
                ["WINS"] = new(ctx => ctx.Stats.TotalGamesWon, new[] { 10, 50, 100, 500 }),
                ["WIN_STREAK"] = new(ctx => ctx.Stats.MaxWinStreak, new[] { 3, 5, 10, 20, 50 }),

                ["QUICK_ANSWER"] = new(ctx => ctx.Stats.TotalQuickAnswers, new[] { 10, 100, 500 }),
                ["LAST_SECOND"] = new(ctx => ctx.Stats.TotalLastSecondWins, new[] { 1, 10, 50 }),

                ["SPEED_RUN"] = new(ctx =>
                {
                    if (ctx.Payload is GameCompletedData g &&
                        g.TimeSpent <= 30 &&
                        g.CorrectAnswers == 10)
                        return 1;
                    return 0;
                }, new[] { 1, 10, 50 }),

                ["EUROPE_MASTER"] = new(ctx => ctx.Stats.EuropeCorrect, new[] { 10, 100, 250 }),
                ["ASIA_MASTER"] = new(ctx => ctx.Stats.AsiaCorrect, new[] { 10, 100, 250 }),
                ["AFRICA_MASTER"] = new(ctx => ctx.Stats.AfricaCorrect, new[] { 10, 100, 250 }),
                ["AMERICA_MASTER"] = new(ctx => ctx.Stats.AmericaCorrect, new[] { 10, 100, 250 }),
                ["OCEANIA_MASTER"] = new(ctx => ctx.Stats.OceaniaCorrect, new[] { 10, 100, 250 }),

                ["FLAG_MASTER"] = new(ctx => ctx.Stats.FlagsCorrect, new[] { 10, 100, 250 }),
                ["CAPITAL_MASTER"] = new(ctx => ctx.Stats.CapitalsCorrect, new[] { 10, 100, 250 }),
                ["OUTLINE_MASTER"] = new(ctx => ctx.Stats.OutlinesCorrect, new[] { 10, 100, 250 }),
                ["LANGUAGE_MASTER"] = new(ctx => ctx.Stats.LanguagesCorrect, new[] { 10, 100, 250 }),

                ["PERFECT_GAME"] = new(ctx =>
                {
                    if (ctx.Payload is GameCompletedData g && g.CorrectAnswers == 10)
                        return 1;
                    return 0;
                }, new[] { 1, 10, 50, 100 }),

                ["TACTICIAN"] = new(ctx =>
                {
                    if (ctx.Payload is GameCompletedData g &&
                        g.UsedAllHints &&
                        g.CorrectAnswers == 10)
                        return 1;
                    return 0;
                }, new[] { 1, 10, 25 }),

                ["PVP_PLAYER"] = new(ctx => ctx.Stats.PvPGamesPlayed, new[] { 10, 50, 200 }),
                ["PVP_WINS"] = new(ctx => ctx.Stats.PvPGamesWon, new[] { 10, 50, 200 }),
                ["PVP_STREAK"] = new(ctx => ctx.Stats.CurrentPvPStreak, new[] { 3, 5, 10, 20 }),

                ["UNDERDOG"] = new(ctx =>
                {
                    if (ctx.Payload is PvPResultData p &&
                        p.UserWon &&
                        p.OpponentLevel - p.UserLevel >= 5)
                        return 1;
                    return 0;
                }, new[] { 1, 5, 20 })
            };

        }

        public async Task CheckAndGrantAsync(AchievementContext ctx)
        {
            var userId = ctx.User.Id;

            var achievements = await _db.Achievements.ToListAsync();

            var userAchievements = await _db.UserAchievements
                .Where(x => x.UserId == userId)
                .ToDictionaryAsync(x => x.AchievementId);

            foreach (var achievement in achievements)
            {
                if (!_rules.TryGetValue(achievement.Code, out var rule))
                    continue;

                var (progress, unlockedNow) = rule.Evaluate(ctx);

                if (!userAchievements.TryGetValue(achievement.Id, out var ua))
                {
                    ua = new UserAchievement
                    {
                        UserId = userId,
                        AchievementId = achievement.Id,
                        Progress = progress,
                        IsUnlocked = unlockedNow,
                        UnlockedAt = unlockedNow ? DateTime.UtcNow : default
                    };

                    _db.UserAchievements.Add(ua);
                }
                else
                {
                    ua.Progress = Math.Max(ua.Progress, progress);

                    if (!ua.IsUnlocked && unlockedNow)
                    {
                        ua.IsUnlocked = true;
                        ua.UnlockedAt = DateTime.UtcNow;

                        _logger.LogInformation("Achievement unlocked: {Code} for user {UserId}", achievement.Code, userId);
                    }
                }
            }

            await _db.SaveChangesAsync();
        }
    }


    public class AchievementRule
    {
        public Func<AchievementContext, int> ProgressFunc { get; }
        public int[] Milestones { get; }

        public AchievementRule(Func<AchievementContext, int> progress, int[] milestones)
        {
            ProgressFunc = progress;
            Milestones = milestones.OrderBy(x => x).ToArray();
        }

        public (int progress, bool unlocked) Evaluate(AchievementContext ctx)
        {
            var value = ProgressFunc(ctx);
            var unlocked = Milestones.Any(m => value >= m);
            return (value, unlocked);
        }
    }
    public class AchievementContext
    {
        public User User { get; init; } = null!;
        public UserStats Stats { get; init; } = null!;

        public PvPMatch? Match { get; init; }
        public GameSession? Session { get; init; }

        public object? Payload { get; init; }
    }
}
