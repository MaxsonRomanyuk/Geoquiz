using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using static GeoQuiz_backend.Application.Payloads.AchievementsPayloads;
using GeoQuiz_backend.Application.Services.Achievement;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class PvPResultService : IPvPResultService
    {
        private readonly AppDbContext _db;
        private readonly IAchievementService _achievementService;
        private readonly ISignalRNotificationService _notificationService;

        public PvPResultService(AppDbContext db,
            IAchievementService achievementService,
            ISignalRNotificationService notificationService)
        {
            _db = db;
            _achievementService = achievementService;
            _notificationService = notificationService;
        }

        public async Task<PvPMatchResultDto> FinalizeMatchAsync(Guid matchId, GameFinishReason reason, Guid? userId)
        {
            var match = await _db.PvPMatches
                .Include(m => m.Player1).ThenInclude(u => u.Stats)
                .Include(m => m.Player2).ThenInclude(u => u.Stats)
                .Include(m => m.QuestionSet)
                .FirstAsync(m => m.Id == matchId);

            if (match.Status == PvPMatchStatus.Finished)
                return new PvPMatchResultDto();

            if (reason == GameFinishReason.OpponentDisconnected && match.Status == PvPMatchStatus.Drafting && userId != null)
            {
                var disconnectedPlayer = userId.Value;
                var playerInDraft = match.Player1Id == disconnectedPlayer ? match.Player2Id : match.Player1Id;
                await _notificationService.NotifyOpponentDisconnected(playerInDraft, new DisconnectData
                {
                    MatchId = matchId,
                    Reason = DisconnectReason.ConnectionLost,
                    YouWin = true,
                    DisconnectedUserId = disconnectedPlayer,
                    DisconnectedAtQuestion = 0,
                    YourCurrentScore = 0,
                    OpponentCurrentScore = 0
                });

                _db.PvPMatches.Remove(match);
                await _db.SaveChangesAsync();

                return new PvPMatchResultDto();
            }

            var answers = await _db.PvPAnswers
                .Where(a => a.MatchId == matchId)
                .ToListAsync();

            var p1Answers = answers.Where(a => a.UserId == match.Player1Id).ToList();
            var p2Answers = answers.Where(a => a.UserId == match.Player2Id).ToList();

            var p1Score = p1Answers.Sum(a => a.ScoreGained);
            var p2Score = p2Answers.Sum(a => a.ScoreGained);


            Guid winnerId;
            if (reason == GameFinishReason.OpponentDisconnected) {
                winnerId = match.Player1Id == userId
                ? match.Player2Id
                : match.Player1Id;
            }
            else
            {
                winnerId = p1Score >= p2Score
                ? match.Player1Id
                : match.Player2Id;
            }
            match.WinnerId = winnerId;
            match.Status = PvPMatchStatus.Finished;
            match.FinishedAt = DateTime.UtcNow;

            GameMode gameMode;
            if (match.SelectedMode.HasValue)
            {
                gameMode = match.SelectedMode.Value;
            }
            else if (match.QuestionSet != null)
            {
                gameMode = match.QuestionSet.Mode;
            }
            else
            {
                throw new InvalidOperationException($"Match {matchId} has no game mode");
            }

            var p1Stats = new PlayerFinalStats
            {
                UserId = match.Player1Id,
                FinalScore = p1Score,
                CorrectAnswers = p1Answers.Count(a => a.IsCorrect),
                TotalQuestionsAnswered = p1Answers.Count,
                AverageAnswerTimeMs = p1Answers.Any()
                        ? (int)p1Answers.Average(a => a.TimeSpentMs)
                        : 0
            };

            var p2Stats = new PlayerFinalStats
            {
                UserId = match.Player2Id,
                FinalScore = p2Score,
                CorrectAnswers = p2Answers.Count(a => a.IsCorrect),
                TotalQuestionsAnswered = p2Answers.Count,
                AverageAnswerTimeMs = p2Answers.Any()
                    ? (int)p2Answers.Average(a => a.TimeSpentMs)
                    : 0
            };
            await _notificationService.NotifyGameFinished(match.Player1Id, new GameFinishedData
            {
                MatchId = matchId,
                WinnerId = winnerId,
                FinishReason = reason,
                YourStats = p1Stats,
                OpponentStats = p2Stats,
                ExperienceGained = winnerId == match.Player1Id ? p1Score : 0,
            });

            await _notificationService.NotifyGameFinished(match.Player2Id, new GameFinishedData
            {
                MatchId = matchId,
                WinnerId = winnerId,
                FinishReason = reason,
                YourStats = p2Stats,
                OpponentStats = p1Stats,
                ExperienceGained = winnerId == match.Player2Id ? p2Score : 0,
            });

            var s1 = CreateSession(match.Player1Id, matchId, p1Answers, p1Score, gameMode);
            var s2 = CreateSession(match.Player2Id, matchId, p2Answers, p2Score, gameMode);

            _db.GameSessions.AddRange(s1, s2);
            await _db.SaveChangesAsync();

            await UpdateUserStats(match, match.Player1, s1, s2, match.Player2, winnerId);
            await UpdateUserStats(match, match.Player2, s2, s1, match.Player1, winnerId);

            return new PvPMatchResultDto
            {
                MatchId = matchId,
                Player1Id = match.Player1Id,
                Player2Id = match.Player2Id,
                WinnerId = winnerId,
                Player1Score = p1Score,
                Player2Score = p2Score
            };
        }

        private async Task UpdateUserStats(PvPMatch match, User user, GameSession self, GameSession opponentSession, User opponentUser, Guid? winnerId)
        {
            var stats = user.Stats;
            var oldStats = stats.Clone();

            var correctAnswers = self.CorrectAnswers;

            stats.TotalGamesPlayed++;
            stats.PvPGamesPlayed++;
            stats.TotalCorrectAnswers += correctAnswers;

            if (winnerId == user.Id)
            {
                stats.TotalGamesWon++;
                stats.PvPGamesWon++;
                stats.CurrentPvPStreak++;
                stats.Score += self.Score;
                AddExperience(stats, self.Score);
            }
            else
            {
                stats.CurrentPvPStreak = 0;
            }

            stats.MaxWinStreak = Math.Max(stats.MaxWinStreak, stats.CurrentWinStreak);

            if (correctAnswers > 0)
            {
                switch (match.SelectedMode)
                {
                    case GameMode.Flag: stats.FlagsCorrect += correctAnswers; break;
                    case GameMode.Capital: stats.CapitalsCorrect += correctAnswers; break;
                    case GameMode.Outline: stats.OutlinesCorrect += correctAnswers; break;
                    case GameMode.Language: stats.LanguagesCorrect += correctAnswers; break;
                }

                var answers = match.Answers.Where(a => a.UserId.Equals(user.Id)).ToList();
                var questionSet = match.QuestionSet;

                if (questionSet != null)
                {
                    var regions = questionSet.Regions;
                    var questionIds = questionSet.QuestionIds;
                    var idRegion = questionIds
                        .Zip(regions, (id, region) => new { id, region })
                        .ToDictionary(x => x.id, x => x.region);
                    foreach (var answer in answers)
                    {
                        if (answer.IsCorrect)
                        {
                            if (idRegion.TryGetValue(answer.QuestionId, out var region))
                            {
                                switch (region)
                                {
                                    case Region.Europe: stats.EuropeCorrect++; break;
                                    case Region.Asia: stats.AsiaCorrect++; break;
                                    case Region.Africa: stats.AfricaCorrect++; break;
                                    case Region.America: stats.AmericaCorrect++; break;
                                    case Region.Oceania: stats.OceaniaCorrect++; break;
                                }
                            }
                        }
                    }
                }
            }
            var newStats = stats;
            await _db.SaveChangesAsync();

            await _achievementService.CheckAndGrantAsync(user.Id, oldStats, newStats, self);
               
        }
        private GameSession CreateSession(Guid userId, Guid matchId, List<PvPAnswer> answers, int score, GameMode gameMode)
        {
            return new GameSession
            {
                Id = Guid.NewGuid(),
                UserId = userId,
                PvPMatchId = matchId,
                Mode = gameMode,
                TotalQuestions = answers.Count,
                CorrectAnswers = answers.Count(a => a.IsCorrect),
                Score = score,
                IsOnline = true,
                PlayedAt = DateTime.UtcNow
            };
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
        private PlayerPvPStatsDto Map(GameSession s) => new()
        {
            UserId = s.UserId,
            TotalQuestions = s.TotalQuestions,
            CorrectAnswers = s.CorrectAnswers,
            Score = s.Score
        };
    }
}
