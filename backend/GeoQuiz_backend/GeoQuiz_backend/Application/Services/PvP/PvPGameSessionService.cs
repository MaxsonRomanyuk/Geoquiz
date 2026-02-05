using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.DTOs.PvP;
using GeoQuiz_backend.Infrastructure.Data;
using Microsoft.EntityFrameworkCore;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class PvPGameSessionService : IPvPGameSessionService
    {
        private readonly AppDbContext _db;
        private readonly IQuestionRepository _questionRepository;
        private readonly ICountryRepository _countryRepository;
        private readonly IPvPResultService _resultService;

        public PvPGameSessionService(AppDbContext db,IQuestionRepository questionRepository,ICountryRepository countryRepository,IPvPResultService resultService)
        {
            _db = db;
            _questionRepository = questionRepository;
            _countryRepository = countryRepository;
            _resultService = resultService;
        }

        public async Task StartMatchAsync(Guid matchId)
        {
            var match = await _db.PvPMatches.FirstAsync(m => m.Id == matchId);

            if (match.Status != PvPMatchStatus.Ready)
                throw new Exception("Match not ready or has already started");

            match.Status = PvPMatchStatus.InGame;
            match.CreatedAt = DateTime.UtcNow;

            await _db.SaveChangesAsync();
        }

        public async Task<PvPGameStateDto> GetGameStateAsync(Guid matchId, Guid userId)
        {
            var match = await _db.PvPMatches
                .Include(m => m.QuestionSet)
                .FirstAsync(m => m.Id == matchId);

            var answers = await _db.PvPAnswers
                .Where(a => a.MatchId == matchId)
                .ToListAsync();

            var yourAnswered = answers.Count(a => a.UserId == userId);

            var opponentId = match.Player1Id == userId
                ? match.Player2Id
                : match.Player1Id;

            var opponentAnswered = answers.Count(a => a.UserId == opponentId);

            return new PvPGameStateDto
            {
                MatchId = matchId,
                Mode = match.QuestionSet.Mode,
                Language = match.QuestionSet.Language,
                QuestionIds = match.QuestionSet.QuestionIds,
                YourAnswered = yourAnswered,
                OpponentAnswered = opponentAnswered,
                IsFinished = yourAnswered >= 10 && opponentAnswered >= 10
            };
        }

        public async Task<SubmitAnswerResponse> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest dto)
        {
            var alreadyAnswered = await _db.PvPAnswers.AnyAsync(a =>
                a.MatchId == matchId &&
                a.UserId == userId &&
                a.QuestionId == dto.QuestionId);

            if (alreadyAnswered)
                throw new Exception("Already answered this question");

            var match = await _db.PvPMatches
                .Include(m => m.QuestionSet)
                .FirstAsync(m => m.Id == matchId);

            var questionSet = match.QuestionSet
                ?? throw new Exception("QuestionSet not generated");

            var question = await _questionRepository.GetByIdAsync(dto.QuestionId);
            var countries = await _countryRepository.GetAllAsync();
            var correctCountry = countries.First(c => c.Id == question.CountryId);

            var index = questionSet.QuestionIds.IndexOf(dto.QuestionId);
            var rnd = new Random(questionSet.Seed + index);

            var wrong = countries.Where(c => c.Id != correctCountry.Id)
                .OrderBy(_ => rnd.Next()).Take(3).ToList();

            var options = new List<string> { correctCountry.Id };
            options.AddRange(wrong.Select(c => c.Id));
            options = options.OrderBy(_ => rnd.Next()).ToList();

            var correctIndex = options.IndexOf(correctCountry.Id);
            var isCorrect = dto.SelectedIndex == correctIndex;

            var score = isCorrect ? CalculateScore(dto.TimeSpentMs) : 0;

            var answer = new PvPAnswer
            {
                Id = Guid.NewGuid(),
                MatchId = matchId,
                UserId = userId,
                QuestionId = dto.QuestionId,
                IsCorrect = isCorrect,
                TimeSpentMs = dto.TimeSpentMs,
                ScoreGained = score,
                AnsweredAt = DateTime.UtcNow
            };

            _db.PvPAnswers.Add(answer);
            await _db.SaveChangesAsync();

            var allAnswers = await _db.PvPAnswers
                .Where(a => a.MatchId == matchId)
                .ToListAsync();

            var yourAnswers = allAnswers.Where(a => a.UserId == userId);
            var opponentId = match.Player1Id == userId
                ? match.Player2Id
                : match.Player1Id;

            var opponentAnswers = allAnswers.Where(a => a.UserId == opponentId);

            return new SubmitAnswerResponse
            {
                IsCorrect = isCorrect,
                YourScore = yourAnswers.Sum(a => a.ScoreGained),
                OpponentScore = opponentAnswers.Sum(a => a.ScoreGained),
                YourAnswered = yourAnswers.Count(),
                OpponentAnswered = opponentAnswers.Count()
            };
        }

        public async Task FinishAsync(Guid matchId, Guid userId)
        {
            var match = await _db.PvPMatches.FirstAsync(m => m.Id == matchId);

            var answers = await _db.PvPAnswers
                .Where(a => a.MatchId == matchId)
                .ToListAsync();

            var p1 = answers.Count(a => a.UserId == match.Player1Id);
            var p2 = answers.Count(a => a.UserId == match.Player2Id);

            if (p1 >= 10 && p2 >= 10)
            {
                await _resultService.FinalizeMatchAsync(matchId);
            }
        }

        private int CalculateScore(int timeMs)
        {
            var maxScore = 100;
            var penalty = timeMs / 100;
            return Math.Max(10, maxScore - penalty);
        }
    }
}
