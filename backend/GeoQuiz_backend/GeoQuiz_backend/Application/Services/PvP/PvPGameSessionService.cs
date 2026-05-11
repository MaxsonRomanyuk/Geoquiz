using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Payloads.Questions;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using System.Collections.Concurrent;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class PvPGameSessionService : IPvPGameSessionService
    {
        private readonly AppDbContext _db;
        private readonly ICountryRepository _countryRepository;
        private readonly IQuestionSetService _questionSetService;
        private readonly IPvPResultService _resultService;
        private readonly ISignalRNotificationService _notificationService;

        private readonly IServiceScopeFactory _serviceScopeFactory;
        private readonly ILogger<PvPGameSessionService> _logger;

        private static readonly ConcurrentDictionary<Guid, List<GameQuestion>> _gameQuestions = new();
        private static readonly ConcurrentDictionary<Guid, CancellationTokenSource> _gameTimers = new();
        private static readonly ConcurrentDictionary<Guid, DateTime> _gameTimerEndsAt = new();
        private static readonly object _matchLock = new();


        public PvPGameSessionService(AppDbContext db,
            ICountryRepository countryRepository,
            IPvPResultService resultService,
            IQuestionSetService questionSetService,
            ISignalRNotificationService notificationService,
            IServiceScopeFactory serviceScopeFactory,
            ILogger<PvPGameSessionService> logger)
        {
            _db = db;
            _countryRepository = countryRepository;
            _resultService = resultService;
            _questionSetService = questionSetService;
            _notificationService = notificationService;
            _serviceScopeFactory = serviceScopeFactory;
            _logger = logger;
        }

        public async Task StartMatchAsync(Guid matchId)
        {
            var match = await _db.PvPMatches.FirstAsync(m => m.Id == matchId);

            if (match.Status != PvPMatchStatus.Ready)
                throw new Exception("Match not ready or has already started");

            match.Status = PvPMatchStatus.InGame;
            match.CreatedAt = DateTime.UtcNow;

            if (match.SelectedMode != null)
            {
                var questionsSet = await _questionSetService.CreateQuestionSetAsync(matchId, GameType.PvP, 10);
                var gameQuestion = match.SelectedMode == GameMode.Language ?
                    await _questionSetService.GenerateLanguageQuestionsAsync(questionsSet) :
                    await _questionSetService.GenerateQuestionsAsync(questionsSet);
                _gameQuestions[matchId] = gameQuestion; 
                var questionData = gameQuestion.Select(g => MapToQuestionData(g)).ToList();
                await _notificationService.NotifyGameReady(matchId, new GameReadyData
                {
                    MatchId = matchId,
                    SelectedMode = match.SelectedMode!.Value,
                    TotalQuestions = gameQuestion.Count,
                    TotalGameTimeSeconds = 60,
                    GameStartTime = DateTime.UtcNow,
                    Questions = questionData
                });

            }
            else
            {
                throw new Exception("Game mode not selected");
            }

            await _db.SaveChangesAsync(); 

        }
        public async Task MonitorGameTimeAsync(Guid matchId)
        {
            const int COUNTDOWN_SECONDS = 60;

            var cts = new CancellationTokenSource();
            var timerEndsAt = DateTime.UtcNow.AddSeconds(COUNTDOWN_SECONDS);

            lock (_matchLock)
            {
                _gameTimers[matchId] = cts;
                _gameTimerEndsAt[matchId] = timerEndsAt;
            }

            try
            {

                using (var scope = _serviceScopeFactory.CreateScope())
                {
                    var notificationService = scope.ServiceProvider.GetRequiredService<ISignalRNotificationService>();
                    await notificationService.NotifyTimerUpdate(matchId, new TimerUpdateData
                    {
                        ServerTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                        TimerEndsAt = new DateTimeOffset(timerEndsAt).ToUnixTimeMilliseconds()
                    });
                }

                await Task.Delay(TimeSpan.FromSeconds(COUNTDOWN_SECONDS), cts.Token);
                
                if (cts.Token.IsCancellationRequested) return;
                    
                using (var scope = _serviceScopeFactory.CreateScope())
                {
                    var scopedResultService = scope.ServiceProvider.GetRequiredService<IPvPResultService>();
                    var scopedDb = scope.ServiceProvider.GetRequiredService<AppDbContext>();

                    await scopedResultService.FinalizeMatchAsync(matchId, GameFinishReason.TimeOut);
                }
            }
            catch (TaskCanceledException)
            {
                return;
            }
            finally
            {
                lock (_matchLock)
                {
                    _gameTimers.TryRemove(matchId, out _);
                    _gameTimerEndsAt.TryRemove(matchId, out _);
                }
                cts.Dispose();
            }
        }
        public DateTime? GetGameTimerEndsAt(Guid matchId)
        {
            if (_gameTimerEndsAt.TryGetValue(matchId, out var endTime))
            {
                return endTime;
            }
            return null;
        }
        public static void CancelMatchTimer(Guid matchId)
        {
            if (_gameTimers.TryRemove(matchId, out var cts))
            {
                cts.Cancel();
                cts.Dispose();
            }
            _gameTimerEndsAt.TryRemove(matchId, out _);
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
            var yourCurrentScore = answers
                .Where(a => a.UserId == userId)
                .Sum(a => (int?)a.ScoreGained) ?? 0;

            var opponentId = match.Player1Id == userId
                ? match.Player2Id
                : match.Player1Id;

            var opponentAnswered = answers.Count(a => a.UserId == opponentId);
            var opponentCurrentScore = answers
                .Where(a => a.UserId == opponentId)
                .Sum(a => (int?)a.ScoreGained) ?? 0;

            if (match.QuestionSet == null || match.SelectedMode == null) return new PvPGameStateDto();

            //var gameQuestion = match.SelectedMode == GameMode.Language ?
            //        await _questionSetService.GenerateLanguageQuestionsAsync(10, match.QuestionSet.Seed) :
            //        await _questionSetService.GenerateQuestionsAsync(10, match.QuestionSet.Seed, (GameMode)match.SelectedMode);
            if (!_gameQuestions.TryGetValue(matchId, out var gameQuestionState))
            {
                gameQuestionState = match.SelectedMode == GameMode.Language ?
                    await _questionSetService.GenerateLanguageQuestionsAsync(match.QuestionSet) :
                    await _questionSetService.GenerateQuestionsAsync(match.QuestionSet);
            }
            var questionData = gameQuestionState.Select(g => MapToQuestionData(g)).ToList();

            var players = await _db.Users
                .Include(u => u.Stats)
                .Where(u => u.Id == match.Player1Id || u.Id == match.Player2Id)
                .ToListAsync();

            var player = players.Single(p => p.Id == userId);
            var opponent = players.Single(p => p.Id != userId);

            return new PvPGameStateDto
            {
                MatchId = matchId,
                Mode = match.QuestionSet.Mode,
                Questions = questionData,
                YourCurrentScore = yourCurrentScore,
                OpponentCurrentScore = opponentCurrentScore,
                YourAnswered = yourAnswered,
                OpponentAnswered = opponentAnswered,
                IsFinished = match.Status == PvPMatchStatus.Finished,
                Player = player,
                Opponent = opponent
            };
        }

        public async Task<SubmitAnswerResponse> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request)
        {
            var alreadyAnswered = await _db.PvPAnswers.AnyAsync(a =>
                a.MatchId == matchId &&
                a.UserId == userId &&
                a.QuestionId == request.CountryId);

            if (alreadyAnswered)
                throw new Exception("Already answered this question");

            var match = await _db.PvPMatches
                .Include(m => m.QuestionSet)
                .FirstAsync(m => m.Id == matchId);

            var questionSet = match.QuestionSet
                ?? throw new Exception("QuestionSet not generated");

            if (!_gameQuestions.TryGetValue(matchId, out var gameQuestionState))
            {
                gameQuestionState = match.SelectedMode == GameMode.Language ?
                    await _questionSetService.GenerateLanguageQuestionsAsync(questionSet) :
                    await _questionSetService.GenerateQuestionsAsync(questionSet);
            }
            //var question = await _questionRepository.GetByIdAsync(request.QuestionId);
            //var countries = await _countryRepository.GetAllAsync();
            //var correctCountry = countries.First(c => c.Id == request.CountryId);

            var index = questionSet.CountryIds.IndexOf(request.CountryId);
            var correctIndex = gameQuestionState[index].CorrectOptionIndex;

            var isCorrect = request.SelectedIndex == correctIndex;

            //var rnd = new Random(questionSet.Seed + request.CountryId.GetHashCode());

            //var wrong = countries
            //    .Where(c => c.Id != correctCountry.Id)
            //    .OrderBy(_ => rnd.Next())
            //    .Take(3)
            //    .ToList();

            //var options = new List<string> { correctCountry.Id };
            //options.AddRange(wrong.Select(c => c.Id));
            //options = options.OrderBy(_ => rnd.Next()).ToList();

            //var correctIndex = options.IndexOf(correctCountry.Id);
            //var isCorrect = request.SelectedIndex == correctIndex;

            var score = isCorrect ? CalculateScore(request.TimeSpentMs) : 0;

            var answer = new PvPAnswer
            {
                Id = Guid.NewGuid(),
                MatchId = matchId,
                UserId = userId,
                QuestionId = request.CountryId,
                IsCorrect = isCorrect,
                TimeSpentMs = request.TimeSpentMs,
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

            if (yourAnswers.Count() >= 10 && opponentAnswers.Count() >= 10)
            {
                lock (_matchLock)
                {
                    CancelMatchTimer(matchId);
                }
                _ = Task.Run(async () =>
                {
                    using (var scope = _serviceScopeFactory.CreateScope())
                    {
                        var resultService = scope.ServiceProvider.GetRequiredService<IPvPResultService>();

                        try
                        {
                            await resultService.FinalizeMatchAsync(matchId, GameFinishReason.AllQuestionsAnswered, null);
                        }
                        catch (Exception ex)
                        {
                            _logger.LogError(ex, "Error in background finalization for match {MatchId}", matchId);
                        }
                    }
                });
            }
            return new SubmitAnswerResponse
            {
                IsCorrect = isCorrect,
                CorrectOptionIndex = correctIndex,
                QuestionNumber = request.QuestionNumber,
                YourScore = yourAnswers.Sum(a => a.ScoreGained),
                OpponentScore = opponentAnswers.Sum(a => a.ScoreGained),
                YourAnswered = yourAnswers.Count(),
                OpponentAnswered = opponentAnswers.Count()
            };
        }
        public async Task ClearQuestionsForMatch(Guid matchId)
        {
            if (!_gameQuestions.TryRemove(matchId, out var gameQuestions))
            {
                _logger.LogWarning("Game {MatchId} not found when clear questions", matchId);
                return;
            }
        }
        private int CalculateScore(int timeMs)
        {
            var maxScore = 10;
            var penalty = timeMs / 1000;
            return Math.Max(1, maxScore - penalty);
        }
        private QuestionData MapToQuestionData(GameQuestion gameQuestion)
        {
            return new QuestionData
            {
                CountryId = gameQuestion.CountryId,
                QuestionText = gameQuestion.QuestionText,
                Options = gameQuestion.Options,
                ImageUrl = gameQuestion.ImageUrl,
                AudioUrl = gameQuestion.AudioUrl,
            };
        }
    }
}
