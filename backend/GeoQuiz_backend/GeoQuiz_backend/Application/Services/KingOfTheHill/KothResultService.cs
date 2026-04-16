using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Services.PvP;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;

namespace GeoQuiz_backend.Application.Services.KingOfTheHill
{
    public class KothResultService : IKothResultService
    {
        private readonly IServiceScopeFactory _serviceScopeFactory;
        private readonly ILogger<KothResultService> _logger;
        private readonly IAchievementService _achievementService;
        private readonly ISignalRNotificationService _notificationService;
        public KothResultService(
            IServiceScopeFactory serviceScopeFactory,
            ILogger<KothResultService> logger,
            IAchievementService achievementService,
            ISignalRNotificationService notificationService)
        {
            _serviceScopeFactory = serviceScopeFactory;
            _logger = logger;
            _achievementService = achievementService;
            _notificationService = notificationService;
        }
        public async Task FinalizeMatchAsync(KothGameState gameState)
        {
            var matchId = gameState.MatchId;
            _logger.LogError("Finalizing match {MatchId}", matchId);

            using var scope = _serviceScopeFactory.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();

            await UpdateMatchAsync(db, gameState);
            _logger.LogError("Update kothmatch {MatchId}", matchId);
            var gameSessions = await CreateGameSessionsAsync(db, gameState);
            _logger.LogError("Update gamesession {MatchId}", matchId);

            var result = CreateMatchFinishedData(gameState);
            await _notificationService.NotifyMatchFinished(matchId, result);
            _logger.LogError("Ended match {MatchId}", matchId);

            await UpdateUserStatsAsync(db, gameState, gameSessions);
            _logger.LogError("Update stats {MatchId}", matchId);
            //return CreateMatchFinishedData(gameState);
        }
        private async Task UpdateMatchAsync(AppDbContext db, KothGameState gameState)
        {
            var match = await db.KothMatches
                .Include(m => m.Players)
                .FirstOrDefaultAsync(m => m.Id == gameState.MatchId);

            if (match == null)
            {
                _logger.LogWarning("Match {MatchId} not found in database", gameState.MatchId);
                return;
            }

            var realPlayerIds = gameState.Players.Where(p => !p.Value.IsBot).Select(p => p.Key).ToList();

            Guid? winnerId = gameState.ActivePlayerIds.Count == 1
                ? (realPlayerIds.Contains(gameState.ActivePlayerIds.First()) ? gameState.ActivePlayerIds.First() : null )
                : null;

            match.Status = KothMatchStatus.Finished;
            match.FinishedAt = DateTime.UtcNow;
            match.WinnerId = winnerId;

            foreach (var player in match.Players)
            {
                if (gameState.PlayerPlaces.TryGetValue(player.UserId, out var place))
                {
                    player.Place = place;
                    player.IsActive = place == 1;
                    player.RoundEliminated = gameState.Players[player.UserId].EliminatedAtRound;
                }
            }

            await db.SaveChangesAsync();
            _logger.LogInformation("Match {MatchId} updated in database. Winner: {WinnerId}",
                gameState.MatchId, winnerId);
        }
        private async Task<List<GameSession>> CreateGameSessionsAsync(AppDbContext db, KothGameState gameState)
        {
            var gameSessions = new List<GameSession>();
            var realPlayers = gameState.Players
                .Where(p => !p.Value.IsBot)
                .Select(p => p.Key)
                .ToList();

            foreach (var userId in realPlayers)
            {
                var answers = await db.KothAnswers
                    .Where(a => a.MatchId == gameState.MatchId && a.UserId == userId)
                    .ToListAsync();

                var place = gameState.PlayerPlaces.GetValueOrDefault(userId, gameState.ActivePlayerIds.Count + 1);
                var roundsSurvived = gameState.Players[userId].EliminatedAtRound;

                var gameSession = new GameSession
                {
                    Id = Guid.NewGuid(),
                    UserId = userId,
                    KothMatchId = gameState.MatchId,
                    Mode = gameState.Match.SelectedMode,
                    TotalQuestions = answers.Count,
                    CorrectAnswers = answers.Count(a => a.IsCorrect),
                    Score = answers.Sum(a => a.ScoreGained),
                    IsOnline = true,
                    PlayedAt = DateTime.UtcNow,
                    Place = place,
                    RoundsSurvived = roundsSurvived
                };

                gameSessions.Add(gameSession);
            }

            if (gameSessions.Any())
            {
                db.GameSessions.AddRange(gameSessions);
                await db.SaveChangesAsync();
                _logger.LogDebug("Created {SessionCount} game sessions for match {MatchId}",
                    gameSessions.Count, gameState.MatchId);
            }

            return gameSessions;
        }
        private async Task UpdateUserStatsAsync(AppDbContext db, KothGameState gameState, List<GameSession> gameSessions)
        {
            foreach (var session in gameSessions)
            {
                var user = await db.Users
                    .Include(u => u.Stats)
                    .FirstOrDefaultAsync(u => u.Id == session.UserId);

                if (user?.Stats == null) continue;

                var stats = user.Stats;
                var oldStats = stats.Clone();
                var isWinner = session.Place == 1;
                var isTop3 = session.Place <= 3;

                stats.TotalGamesPlayed++;
                stats.KothGamesPlayed++;
                stats.TotalCorrectAnswers += session.CorrectAnswers;

                if (isWinner)
                {
                    stats.TotalGamesWon++;
                    stats.KothGamesWon++;
                    stats.CurrentWinStreak++;
                }
                else
                {
                    stats.CurrentWinStreak = 0;
                }
                stats.MaxWinStreak = Math.Max(stats.MaxWinStreak, stats.CurrentWinStreak);

                if (isTop3)
                {
                    stats.KothTop3Finishes++;

                    var experienceGain = session.Score * 3;
                    stats.Experience += experienceGain;

                    while (stats.Experience >= GetXpToNextLevel(stats.Level))
                    {
                        stats.Experience -= GetXpToNextLevel(stats.Level);
                        stats.Level++;
                    }
                }

                switch (session.Mode)
                {
                    case GameMode.Flag:
                        stats.FlagsCorrect += session.CorrectAnswers;
                        break;
                    case GameMode.Capital:
                        stats.CapitalsCorrect += session.CorrectAnswers;
                        break;
                    case GameMode.Outline:
                        stats.OutlinesCorrect += session.CorrectAnswers;
                        break;
                    case GameMode.Language:
                        stats.LanguagesCorrect += session.CorrectAnswers;
                        break;
                }

                var newStats = stats;
                await CheckAchievementsAsync(user.Id, oldStats, newStats, session);
            }

            await db.SaveChangesAsync();
            _logger.LogInformation("User stats updated for match {MatchId}", gameState.MatchId);
        }
        private async Task CheckAchievementsAsync(Guid userId, UserStats oldStats, UserStats newStats, GameSession session)
        {
            using var scope = _serviceScopeFactory.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
            //var notification = scope.ServiceProvider.GetRequiredService<SignalRNotificationService>();
            await _achievementService.CheckAndGrantAsync(db, userId, oldStats, newStats, session);
        }

        private MatchFinishedData CreateMatchFinishedData(KothGameState gameState)
        {
            var finalStandings = new List<PlayerFinalStanding>();

            if (gameState.ActivePlayerIds.Count == 1)
            {
                var winnerId = gameState.ActivePlayerIds.First();
                var winnerInfo = gameState.Players[winnerId];

                finalStandings.Add(new PlayerFinalStanding
                {
                    PlayerId = winnerId,
                    PlayerName = winnerInfo.UserName,
                    Place = 1,
                    CorrectAnswers = gameState.PlayerCorrectCount.GetValueOrDefault(winnerId),
                    TotalScore = gameState.PlayerScores.GetValueOrDefault(winnerId),
                    RoundsSurvived = gameState.CurrentRound
                });
            }

            var eliminatedWithPlaces = gameState.PlayerPlaces
                .Where(p => !gameState.ActivePlayerIds.Contains(p.Key))
                .Select(p => new
                {
                    PlayerId = p.Key,
                    Place = p.Value
                })
                .OrderBy(x => x.Place);

            foreach (var item in eliminatedWithPlaces)
            {
                finalStandings.Add(new PlayerFinalStanding
                {
                    PlayerId = item.PlayerId,
                    PlayerName = gameState.Players[item.PlayerId].UserName,
                    Place = item.Place,
                    CorrectAnswers = gameState.PlayerCorrectCount.GetValueOrDefault(item.PlayerId),
                    TotalScore = gameState.PlayerScores.GetValueOrDefault(item.PlayerId),
                    RoundsSurvived = gameState.Players[item.PlayerId].EliminatedAtRound
                });
            }

            return new MatchFinishedData
            {
                MatchId = gameState.MatchId,
                WinnerId = gameState.ActivePlayerIds.Count == 1 ? gameState.ActivePlayerIds.First() : Guid.Empty,
                FinalStandings = finalStandings
            };
        }
        private int GetXpToNextLevel(int level) => level * 100;
    }
}
