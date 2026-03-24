using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Application.DTOs.Game;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using static GeoQuiz_backend.Application.Payloads.AchievementsPayloads;

namespace GeoQuiz_backend.Application.Services
{
    public class GameService : IGameService
    {
        private readonly AppDbContext _db;
        private readonly IAchievementService _achievementService;

        public GameService(AppDbContext db, IAchievementService achievementService)
        {
            _db = db;
            _achievementService = achievementService;
        }

        public async Task ProcessFinishedGameAsync(Guid userId, FinishGameRequest request, DateTime? playedAt = null, Guid ? sessionId = null )
        {
            var user = await _db.Users
                .Include(u => u.Stats)
                .FirstAsync(u => u.Id == userId);

            var session = new GameSession
            {
                Id = sessionId ?? Guid.NewGuid(),
                UserId = userId,
                Mode = request.Mode,
                TotalQuestions = request.TotalQuestions,
                CorrectAnswers = request.CorrectAnswers,
                Score = request.Score,
                IsOnline = request.IsOnline,
                PlayedAt = playedAt ?? DateTime.UtcNow
            };

            _db.GameSessions.Add(session);

            UpdateStats(user.Stats, request);

            await _achievementService.CheckAndGrantAsync(new AchievementContext
            {
                User = user,
                Stats = user.Stats,
                Session = session,
                Payload = new GameCompletedData
                {
                    CorrectAnswers = request.CorrectAnswers,
                    TimeSpent = request.TimeSpent,
                    GameMode = request.Mode.ToString()
                }
            });

            await _db.SaveChangesAsync();
        }

        public async Task SyncGamesAsync(Guid userId, List<SyncGameSessionRequest> games)
        {
            foreach (var game in games)
            {
                if (await _db.GameSessions.AnyAsync(g => g.Id == game.Id))
                    continue;

                await ProcessFinishedGameAsync(userId, new FinishGameRequest
                {
                    Mode = game.Mode,
                    TotalQuestions = game.TotalQuestions,
                    CorrectAnswers = game.CorrectAnswers,
                    EuropeCorrect = game. EuropeCorrect,
                    AsiaCorrect = game.AsiaCorrect,
                    AfricaCorrect = game.AfricaCorrect,
                    AmericaCorrect = game.AmericaCorrect,
                    OceaniaCorrect = game.OceaniaCorrect,
                    Score = game.Score,
                    IsOnline = false,
                    TimeSpent = game.TimeSpent
                },
                game.PlayedAt,
                game.Id
                );
            }
        }

        private static void UpdateStats(UserStats stats, FinishGameRequest r)
        {
            stats.TotalGamesPlayed++;
            stats.TotalCorrectAnswers += r.CorrectAnswers;

            if (r.CorrectAnswers >= 8)
            {
                stats.CurrentWinStreak++;
                stats.TotalGamesWon++;
                AddExperience(stats, r.Score);
            }
            else
                stats.CurrentWinStreak = 0;

            stats.MaxWinStreak = Math.Max(stats.MaxWinStreak, stats.CurrentWinStreak);

            stats.EuropeCorrect += r.EuropeCorrect;
            stats.AsiaCorrect += r.AsiaCorrect;
            stats.AfricaCorrect += r.AfricaCorrect;
            stats.AmericaCorrect += r.AmericaCorrect;
            stats.OceaniaCorrect += r.OceaniaCorrect;

            switch (r.Mode)
            {
                case GameMode.Flag: stats.FlagsCorrect += r.CorrectAnswers; break;
                case GameMode.Capital: stats.CapitalsCorrect += r.CorrectAnswers; break;
                case GameMode.Outline: stats.OutlinesCorrect += r.CorrectAnswers; break;
                case GameMode.Language: stats.LanguagesCorrect += r.CorrectAnswers; break;
            }
        }
        private static void AddExperience(UserStats stats, int gainedXp)
        {
            stats.Experience += gainedXp;

            if (stats.Experience >= GetXpToNextLevel(stats.Level))
            {
                stats.Experience -= GetXpToNextLevel(stats.Level);
                stats.Level++;
            }
        }
        private static int GetXpToNextLevel(int level)
        {
            return level * 100;
        }
    }
}
