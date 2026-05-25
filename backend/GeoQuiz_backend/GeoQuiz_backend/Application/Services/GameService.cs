using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Application.DTOs.Game;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using static GeoQuiz_backend.Application.Payloads.AchievementsPayloads;
using GeoQuiz_backend.Application.Services.Achievement;

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
                User = user,
                Mode = request.Mode,
                Type = GameType.Solo,
                TotalQuestions = request.TotalQuestions,
                CorrectAnswers = request.CorrectAnswers,
                Score = request.Score,
                IsWin = request.CorrectAnswers > 7,
                IsOnline = request.IsOnline,
                PlayedAt = playedAt ?? DateTime.UtcNow
            };

            _db.GameSessions.Add(session);

            var oldStats = user.Stats.Clone();
            UpdateStats(user.Stats, request);
            var newStats = user.Stats;

            await _achievementService.CheckAndGrantAsync(userId, oldStats, newStats, session);

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

        public async Task<GameHistoryResponse> GetGameHistory(Guid userId, int page, int pageSize = 10)
        {
            var anySessions = await _db.GameSessions.AnyAsync();
            Console.WriteLine($"Any sessions: {anySessions}");

            var firstSession = await _db.GameSessions.FirstOrDefaultAsync();
            if (firstSession != null)
            {
                Console.WriteLine($"First session Type: {firstSession.Type}, IsWin: {firstSession.IsWin}");
            }

            var query = _db.GameSessions.Where(g => g.UserId == userId);

            var result = await query
                .OrderByDescending(g => g.PlayedAt)
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .Select(g => MapToDto(g))
                .ToListAsync();

            var totalCount = await query.CountAsync();
            var totalPages = totalCount / pageSize;
            var totalWins = await query.Where(r => r.IsWin).CountAsync();
            var serverTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

            var gameHistory = new GameHistoryResponse
            {
                Matches = result,
                TotalCount = totalCount,
                TotalPages = totalPages,
                TotalWins = totalWins,
                ServerTime = serverTime,
            };
            return gameHistory;
        }

        private static GameSessionDto MapToDto(GameSession gameSession)
        {
            var session = new GameSessionDto
            {
                MatchId = gameSession.Id,
                GameType = (int)gameSession.Type,
                PlayedAt = new DateTimeOffset(gameSession.PlayedAt).ToUnixTimeMilliseconds(),
                TotalScore = gameSession.Score,
                CorrectAnswers = gameSession.CorrectAnswers,
                TotalQuestions = gameSession.TotalQuestions,
                GameMode = (int)gameSession.Mode,
                IsWin = gameSession.IsWin,
                Place = gameSession.Place,
                RoundsSurvived = gameSession.RoundsSurvived,
                ExperienceGained = GetExperienceGained(gameSession)
            };
            return session;
        }

        public async Task UpdateGameSessionsFields()
        {
            var gameSessions = await _db.GameSessions
                .Include(gs => gs.PvPMatch)
                .ToListAsync();

            foreach (var session in gameSessions)
            {
                if (session.PvPMatchId != null)
                {
                    session.Type = GameType.PvP; 
                }
                else if (session.KothMatchId != null)
                {
                    session.Type = GameType.KoTH; 
                }
                else
                {
                    session.Type = GameType.Solo; 
                }

                switch (session.Type)
                {
                    case GameType.Solo: 
                        session.IsWin = session.CorrectAnswers >= 8;
                        break;

                    case GameType.PvP: 
                        if (session.PvPMatch != null)
                        {
                            session.IsWin = session.PvPMatch.WinnerId == session.UserId;
                        }
                        else
                        {
                            session.IsWin = false;
                        }
                        break;

                    case GameType.KoTH: 
                        session.IsWin = session.Place == 1;
                        break;
                }
            }

            await _db.SaveChangesAsync();
        }
        private static int GetExperienceGained(GameSession gameSession)
        {
            if (gameSession.Type == GameType.Solo || gameSession.Type == GameType.PvP)
            {
                if (gameSession.IsWin) return gameSession.Score;
                else return 0;
            }
            else
            {
                if (gameSession.Place != null && gameSession.Place <= 3) return gameSession.Score * 3;
                else return 0;
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
                stats.Score += r.Score;
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

            while (stats.Experience >= GetXpToNextLevel(stats.Level))
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
