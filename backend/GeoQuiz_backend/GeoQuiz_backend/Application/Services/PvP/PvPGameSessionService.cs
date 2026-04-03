using Amazon.Runtime.Internal;
using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace GeoQuiz_backend.Application.Services.PvP
{
    public class PvPGameSessionService : IPvPGameSessionService
    {
        private readonly AppDbContext _db;
        private readonly IQuestionRepository _questionRepository;
        private readonly ICountryRepository _countryRepository;
        private readonly IQuestionSetService _questionSetService;
        private readonly IPvPResultService _resultService;
        private readonly ISignalRNotificationService _notificationService;

        private readonly IServiceScopeFactory _serviceScopeFactory;
        private readonly ILogger<PvPGameSessionService> _logger;

        private static readonly ConcurrentDictionary<Guid, CancellationTokenSource> _gameTimers = new();
        private static readonly object _matchLock = new();


        public PvPGameSessionService(AppDbContext db,
            IQuestionRepository questionRepository,
            ICountryRepository countryRepository,
            IPvPResultService resultService,
            IQuestionSetService questionSetService,
            ISignalRNotificationService notificationService,
            IServiceScopeFactory serviceScopeFactory,
            ILogger<PvPGameSessionService> logger)
        {
            _db = db;
            _questionRepository = questionRepository;
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
                var gameQuestion = await GenerateQuestionsAsync(matchId, (GameMode)match.SelectedMode);

                await _notificationService.NotifyGameReady(matchId, new GameReadyData
                {
                    MatchId = matchId,
                    SelectedMode = match.SelectedMode!.Value,
                    TotalQuestions = gameQuestion.Count,
                    TotalGameTimeSeconds = 60,
                    GameStartTime = DateTime.UtcNow,
                    Questions = gameQuestion
                });

                _ = Task.Run(() => MonitorGameTimeAsync(matchId));            }
            else
            {
                throw new Exception("Game mode not selected");
            }
            //await _db.SaveChangesAsync();  сохраняем в CreateForMatchAsync

        }
        private async Task<List<QuestionData>> GenerateQuestionsAsync(Guid matchId, GameMode mode)
        {
            var questions = new List<QuestionData>();
            var questionsSet = await _questionSetService.CreateForMatchAsync(matchId);
            var allCountries = await _countryRepository.GetAllAsync();

            var random = new Random(questionsSet.Seed);


            foreach (var questionId in questionsSet.QuestionIds)
            {
                var countryId = RemoveLastUnderscorePart(questionId);
                var country = allCountries.First(c => c.Id == countryId);

                var optionRandom = new Random(questionsSet.Seed + questionId.GetHashCode());

                var options = new List<OptionData>();
                var correctOption = new OptionData
                {
                    Index = 0,
                    Text = GetLocalizedTextForCountry(country, mode)
                };

                var wrongCountries = allCountries
                    .Where(c => c.Id != country.Id)
                    .OrderBy(x => optionRandom.Next())
                    .Take(3)
                    .ToList();

                options.Add(correctOption);
                foreach (var wrongCountry in wrongCountries)
                {
                    options.Add(new OptionData
                    {
                        Index = options.Count,
                        Text = GetLocalizedTextForCountry(wrongCountry, mode)
                    });
                }

                options = options.OrderBy(x => optionRandom.Next()).ToList();

                questions.Add(new QuestionData
                {
                    QuestionId = questionId,
                    QuestionText = GetQuestionLocalizedText(mode, country),
                    Options = options,
                    QuestionNumber = questions.Count+1,
                    ImageUrl = GetImageUrl(mode, country),
                    AudioUrl = GetAudioUrl(mode, country)
                });
            }

            return questions;
        }
        public static string RemoveLastUnderscorePart(string input)
        {
            if (string.IsNullOrEmpty(input)) return input;

            for (int i = input.Length - 1; i >= 0; i--)
            {
                if (input[i] == '_')
                {
                    return input.Substring(0, i);
                }
            }
            return input;
        }
        private async Task MonitorGameTimeAsync(Guid matchId)
        {
            const int COUNTDOWN_SECONDS = 60;

            var cts = new CancellationTokenSource();

            lock (_matchLock)
            {
                _gameTimers[matchId] = cts;
            }

            try
            {
                using (var scope = _serviceScopeFactory.CreateScope())
                {
                    var notificationService = scope.ServiceProvider.GetRequiredService<ISignalRNotificationService>();
                    await notificationService.NotifyTimerUpdate(matchId, new TimerUpdateData
                    {
                        ServerTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                        TimerEndsAt = DateTimeOffset.UtcNow.AddSeconds(COUNTDOWN_SECONDS).ToUnixTimeMilliseconds()
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
                }
                cts.Dispose();
            }
        }
        public static void CancelMatchTimer(Guid matchId)
        {
            if (_gameTimers.TryRemove(matchId, out var cts))
            {
                cts.Cancel();
                cts.Dispose();
            }
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

        public async Task<SubmitAnswerResponse> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request)
        {
            var alreadyAnswered = await _db.PvPAnswers.AnyAsync(a =>
                a.MatchId == matchId &&
                a.UserId == userId &&
                a.QuestionId == request.QuestionId);

            if (alreadyAnswered)
                throw new Exception("Already answered this question");

            var match = await _db.PvPMatches
                .Include(m => m.QuestionSet)
                .FirstAsync(m => m.Id == matchId);

            var questionSet = match.QuestionSet
                ?? throw new Exception("QuestionSet not generated");

            var question = await _questionRepository.GetByIdAsync(request.QuestionId);
            var countries = await _countryRepository.GetAllAsync();
            var correctCountry = countries.First(c => c.Id == question.CountryId);

            var index = questionSet.QuestionIds.IndexOf(request.QuestionId);
            var rnd = new Random(questionSet.Seed + request.QuestionId.GetHashCode());

            var wrong = countries.Where(c => c.Id != correctCountry.Id)
                .OrderBy(_ => rnd.Next()).Take(3).ToList();

            var options = new List<string> { correctCountry.Id };
            options.AddRange(wrong.Select(c => c.Id));
            options = options.OrderBy(_ => rnd.Next()).ToList();

            var correctIndex = options.IndexOf(correctCountry.Id);
            var isCorrect = request.SelectedIndex == correctIndex;

            var score = isCorrect ? CalculateScore(request.TimeSpentMs) : 0;

            var answer = new PvPAnswer
            {
                Id = Guid.NewGuid(),
                MatchId = matchId,
                UserId = userId,
                QuestionId = request.QuestionId,
                IsCorrect = isCorrect,
                TimeSpentMs = request.TimeSpentMs,
                ScoreGained = score,
                AnsweredAt = DateTime.UtcNow
            };

            var im = options.Count;
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
        private LocalizedText GetLocalizedTextForCountry(Country country, GameMode mode)
        {
            return mode switch
            {
                GameMode.Capital => country.Capital,
                GameMode.Flag => country.Name,
                GameMode.Outline => country.Name,
                GameMode.Language => country.Name,
                _ => country.Name
            };
        }

        private LocalizedText GetQuestionLocalizedText(GameMode mode, Country country)
        {
            return mode switch
            {
                GameMode.Capital => new LocalizedText
                {
                    Ru = $"Столица страны {country.Name.Ru}?",
                    En = $"Capital of {country.Name.En}?"
                },
                GameMode.Flag => new LocalizedText
                {
                    Ru = "Флаг какой страны?",
                    En = "Which country's flag?"
                },
                GameMode.Outline => new LocalizedText
                {
                    Ru = "Контур какой страны?",
                    En = "Which country's outline?"
                },
                GameMode.Language => new LocalizedText
                {
                    Ru = $"Официальный язык {country.Name.Ru}?",
                    En = $"Official language of {country.Name.En}?"
                },
                _ => new LocalizedText
                {
                    Ru = "Вопрос",
                    En = "Question"
                }
            };
        }

        private string? GetImageUrl(GameMode mode, Country country)
        {
            return mode == GameMode.Flag ? country.FlagImage
                : mode == GameMode.Outline ? country.OutlineImage
                : null;
        }

        private string? GetAudioUrl(GameMode mode, Country country)
        {
            return mode == GameMode.Language ? country.LanguageAudio : null;
        }
        private int CalculateScore(int timeMs)
        {
            var maxScore = 10;
            var penalty = timeMs / 1000;
            return Math.Max(1, maxScore - penalty);
        }
    }
}
