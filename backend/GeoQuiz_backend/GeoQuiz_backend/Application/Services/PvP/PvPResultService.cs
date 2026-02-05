using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.DTOs.PvP;
using GeoQuiz_backend.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;
using static GeoQuiz_backend.Application.Achievements.AchievementsPayloads;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class PvPResultService : IPvPResultService
    {
        private readonly AppDbContext _db;
        private readonly IAchievementService _achievementService;

        public PvPResultService(AppDbContext db, IAchievementService achievementService)
        {
            _db = db;
            _achievementService = achievementService;
        }

        public async Task<PvPMatchResultDto> FinalizeMatchAsync(Guid matchId)
        {
            var match = await _db.PvPMatches
                .Include(m => m.Player1).ThenInclude(u => u.Stats)
                .Include(m => m.Player2).ThenInclude(u => u.Stats)
                .FirstAsync(m => m.Id == matchId);

            var answers = await _db.PvPAnswers
                .Where(a => a.MatchId == matchId)
                .ToListAsync();

            var p1Answers = answers.Where(a => a.UserId == match.Player1Id).ToList();
            var p2Answers = answers.Where(a => a.UserId == match.Player2Id).ToList();

            if (p1Answers.Count < 10 || p2Answers.Count < 10)
                throw new InvalidOperationException("Match not finished");

            var p1Score = p1Answers.Sum(a => a.ScoreGained);
            var p2Score = p2Answers.Sum(a => a.ScoreGained);

            var winnerId = p1Score >= p2Score
                ? match.Player1Id
                : match.Player2Id;

            match.WinnerId = winnerId;
            match.Status = PvPMatchStatus.Finished;
            match.FinishedAt = DateTime.UtcNow;

            GameMode gm = (GameMode)match.SelectedMode;

            var s1 = CreateSession(match.Player1Id, matchId, p1Answers, p1Score, gm);
            var s2 = CreateSession(match.Player2Id, matchId, p2Answers, p2Score, gm);

            _db.GameSessions.AddRange(s1, s2);

            await UpdateUserStats(match, match.Player1, s1, s2, match.Player2, winnerId);
            await UpdateUserStats(match, match.Player2, s2, s1, match.Player1, winnerId);

            //await _db.SaveChangesAsync(); в _achievementService.CheckAndGrantAsync обновляется

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

            stats.TotalGamesPlayed++;
            stats.PvPGamesPlayed++;

            if (winnerId == user.Id)
            {
                stats.TotalGamesWon++;
                stats.PvPGamesWon++;
                stats.CurrentPvPStreak++;
            }
            else
            {
                stats.CurrentPvPStreak = 0;
            }

            await _achievementService.CheckAndGrantAsync(
                new AchievementContext
                {
                    User = user,
                    Stats = stats,
                    Match = match,
                    Session = self,
                    Payload = new PvPResultData
                    {
                        UserLevel = user.Stats.Level,
                        OpponentLevel = opponentUser.Stats.Level,
                        UserWon = winnerId == user.Id
                    }
                }
            );
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
        private PlayerPvPStatsDto Map(GameSession s) => new()
        {
            UserId = s.UserId,
            TotalQuestions = s.TotalQuestions,
            CorrectAnswers = s.CorrectAnswers,
            Score = s.Score
        };
    }
}
