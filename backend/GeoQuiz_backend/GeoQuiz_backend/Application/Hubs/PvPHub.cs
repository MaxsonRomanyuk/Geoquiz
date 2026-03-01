using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Services;
using GeoQuiz_backend.Application.Services.PvP;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.DTOs.PvP;
using GeoQuiz_backend.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz_backend.Application.Hubs
{
    [Authorize]
    public class PvPHub : Hub<IPvPHubClient>
    {
        private readonly ISignalRNotificationService _notificationService;
        private readonly IMatchmakingService _matchmaking;
        private readonly IDraftService _draftService;
        private readonly IQuestionSetService _questionSetService;
        private readonly IPvPGameSessionService _gameSessionService;          
        private readonly IPvPResultService _resultService;
        private readonly IQuestionRepository _questionRepository;      
        private readonly ICountryRepository _countryRepository;
        private readonly ILogger<PvPHub> _logger;
        private readonly AppDbContext _db;

        private readonly IServiceScopeFactory _serviceScopeFactory;

        private static readonly ConcurrentDictionary<Guid, string> _userConnections = new();
        private static readonly ConcurrentDictionary<Guid, CancellationTokenSource> _draftTimers = new();
        private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentMatch = new();
        private static readonly ConcurrentDictionary<Guid, GameTimer> _gameTimers = new();
        private class GameTimer
        {
            public DateTime StartTime { get; set; }
            public CancellationTokenSource Cts { get; set; } = new();
        }
        public PvPHub(
                    ISignalRNotificationService notificationService,
                    IMatchmakingService matchmaking,
                    IDraftService draftService,
                    IQuestionSetService questionSetService,
                    IPvPGameSessionService gameService,
                    IPvPResultService resultService,
                    IQuestionRepository questionRepository,
                    ICountryRepository countryRepository,
                    ILogger<PvPHub> logger,
                    AppDbContext db,
                    IServiceScopeFactory serviceScopeFactory)
        {
            _notificationService = notificationService;
            _matchmaking = matchmaking;
            _draftService = draftService;
            _questionSetService = questionSetService;
            _gameSessionService = gameService;
            _resultService = resultService;
            _questionRepository = questionRepository;
            _countryRepository = countryRepository;
            _logger = logger;
            _db = db;
            _serviceScopeFactory = serviceScopeFactory;
        }
        public override async Task OnConnectedAsync()
        {
            var userId = GetUserId();
            var connectionId = Context.ConnectionId;

            if (_userConnections.TryGetValue(userId, out var oldConnectionId))
            {
                _logger.LogInformation("User {UserId} reconnecting. Old connection: {OldConnection}, New connection: {NewConnection}",
                    userId, oldConnectionId, connectionId);

                if (_userCurrentMatch.TryGetValue(userId, out var matchId))
                {
                    await Groups.RemoveFromGroupAsync(oldConnectionId, $"match_{matchId}");
                    await Groups.AddToGroupAsync(connectionId, $"match_{matchId}");
                    _logger.LogInformation("User {UserId} moved to group match_{MatchId} with new connection",
                        userId, matchId);
                }
            }

            _userConnections[userId] = connectionId;
            _logger.LogInformation("User {UserId} connected with connection {ConnectionId}",
                userId, connectionId);

            await base.OnConnectedAsync();
        }

        public override async Task OnDisconnectedAsync(Exception? exception)
        {
            var userId = GetUserId();

            if (_userConnections.TryRemove(userId, out var connectionId))
            {
                if (_userCurrentMatch.TryRemove(userId, out var matchId))
                {
                    await Groups.RemoveFromGroupAsync(connectionId, $"match_{matchId}");
                    _logger.LogInformation("User {UserId} removed from group match_{MatchId}",
                        userId, matchId);

                    await FinalizeGameAsync(matchId, GameFinishReason.OpponentDisconnected, userId);
                }
            }

            await _matchmaking.LeaveQueueAsync(userId);

            _logger.LogInformation("User {UserId} disconnected", userId);
            await base.OnDisconnectedAsync(exception);
        }

        private Guid GetUserId()
        {
            var userIdClaim = Context.User?.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? Context.User?.FindFirstValue(JwtRegisteredClaimNames.Sub);

            if (string.IsNullOrEmpty(userIdClaim) || !Guid.TryParse(userIdClaim, out var userId))
                throw new HubException("Invalid user identification");

            return userId;
        }
        public async Task JoinQueue()
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} joining queue", userId);

            try
            {
                var match = await _matchmaking.JoinQueueAsync(userId);

                if (match == null)
                {
                    return;
                }
                await NotifyMatchFoundToBoth(match);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in JoinQueue for user {UserId}", userId);
                throw new HubException("Failed to join queue");
            }
        }
        public async Task LeaveQueue()
        {
            var userId = GetUserId();
            await _matchmaking.LeaveQueueAsync(userId);
            _logger.LogInformation("User {UserId} left queue", userId);
        }
        private async Task NotifyMatchFoundToBoth(PvPMatch match)
        {
            var player1 = await _db.Users
                .Include(u => u.Stats)
                .FirstAsync(u => u.Id == match.Player1Id);

            var player2 = await _db.Users
                .Include(u => u.Stats)
                .FirstAsync(u => u.Id == match.Player2Id);

            var player1Data = new MatchFoundWithDraftData
            {
                MatchId = match.Id,
                YourId = player1.Id,
                OpponentId = player2.Id,
                OpponentName = player2.UserName,
                OpponentLevel = player2.Stats.Level,
                OpponentIsPremium = player2.IsPremium,
                AvailableModes = match.Draft!.AvailableModes,
                BannedModes = match.Draft.BannedModes,
                CurrentTurnUserId = match.Draft.CurrentTurnUserId,
                TimePerTurnSeconds = 10,
                FirstTurnStartTime = DateTime.UtcNow.AddSeconds(1)
            };

            var player2Data = new MatchFoundWithDraftData
            {
                MatchId = match.Id,
                YourId = player2.Id,
                OpponentId = player1.Id,
                OpponentName = player1.UserName,
                OpponentLevel = player1.Stats.Level,
                OpponentIsPremium = player1.IsPremium,
                AvailableModes = match.Draft.AvailableModes,
                BannedModes = match.Draft.BannedModes,
                CurrentTurnUserId = match.Draft.CurrentTurnUserId,
                TimePerTurnSeconds = 10,
                FirstTurnStartTime = DateTime.UtcNow.AddSeconds(1)
            };

            await _notificationService.NotifyMatchFound(player1.Id, player1Data);
            await _notificationService.NotifyMatchFound(player2.Id, player2Data);

            _userCurrentMatch[player1.Id] = match.Id;
            _userCurrentMatch[player2.Id] = match.Id;

            if (_userConnections.TryGetValue(player1.Id, out var conn1))
            {
                await Groups.AddToGroupAsync(conn1, $"match_{match.Id}");
                _logger.LogInformation("Added player1 {UserId} to group match_{MatchId} with connection {ConnectionId}",
                    player1.Id, match.Id, conn1);
            } else
            {
                _logger.LogWarning("Player1 {UserId} not connected when adding to group", player1.Id);
            }

            if (_userConnections.TryGetValue(player2.Id, out var conn2))
            {
                await Groups.AddToGroupAsync(conn2, $"match_{match.Id}");
                _logger.LogInformation("Added player2 {UserId} to group match_{MatchId} with connection {ConnectionId}",
                    player2.Id, match.Id, conn2);
            } else
            {
                _logger.LogWarning("Player2 {UserId} not connected when adding to group", player2.Id);
            }

            StartDraftTimer(match.Id, match.Draft.CurrentTurnUserId, AppLanguage.Ru);
        }
        
        public async Task LeaveMatch(Guid matchId)
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} leaving match {MatchId}", userId, matchId);

            var connectionId = Context.ConnectionId;
            await Groups.RemoveFromGroupAsync(connectionId, $"match_{matchId}");

        }
        public async Task BanMode(Guid matchId, GameMode mode, AppLanguage language)
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} banning mode {Mode} in match {MatchId}",
                userId, mode, matchId);

            try
            {
                var draft = await _draftService.BanModeAsync(matchId, userId, mode);

                if (_draftTimers.TryRemove(matchId, out var oldCts))
                {
                    oldCts.Cancel();
                }

                var updateData = new DraftUpdateData
                {
                    MatchId = matchId,
                    BannedMode = mode,
                    BannedByUserId = userId,
                    RemainingModes = draft.AvailableModes,
                    NextTurnUserId = draft.CurrentTurnUserId,
                    Step = draft.Step,
                    IsDraftCompleted = draft.PvPMatch.Status == PvPMatchStatus.Ready
                };

                await _notificationService.NotifyDraftUpdated(matchId, updateData);

                _logger.LogInformation("Draft updated for match {MatchId}. Remaining modes: {Modes}", matchId, string.Join(", ", draft.AvailableModes));

                if (draft.PvPMatch.Status == PvPMatchStatus.Drafting)
                {
                    StartDraftTimer(matchId, draft.CurrentTurnUserId, language);
                }
                else if (draft.PvPMatch.Status == PvPMatchStatus.Ready)
                {
                    _logger.LogInformation("Draft completed for match {MatchId}, selected mode: {Mode}", matchId, draft.PvPMatch.SelectedMode);

                    var questionSet = await _questionSetService.CreateForMatchAsync(matchId, language);
                    await PrepareGameAsync(matchId, language);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in BanMode");
                throw new HubException(ex.Message);
            }
        }
        private void StartDraftTimer(Guid matchId, Guid currentTurnUserId, AppLanguage language)
        {
            var cts = new CancellationTokenSource();
            _draftTimers[matchId] = cts;

            Task.Run(async () =>
            {
                try
                {
                    await Task.Delay(TimeSpan.FromSeconds(10), cts.Token);

                    var draft = await _draftService.GetDraftAsync(matchId);

                    if (draft.CurrentTurnUserId == currentTurnUserId && draft.PvPMatch.Status == PvPMatchStatus.Drafting)
                    {
                        var modeToBan = draft.AvailableModes.First();
                        _logger.LogInformation("Timer expired for user {UserId} in match {MatchId}. Auto-banning {Mode}",
                        currentTurnUserId, matchId, modeToBan);
                        await BanMode(matchId, modeToBan, language);
                    }
                }
                catch (TaskCanceledException)
                {
                    _logger.LogDebug("Draft timer cancelled for match {MatchId}", matchId);
                }
                finally
                {
                    _draftTimers.TryRemove(matchId, out _);
                }
            }, cts.Token);
        }

        private async Task PrepareGameAsync(Guid matchId, AppLanguage language)
        {
            _logger.LogInformation("Preparing game for match {MatchId}", matchId);

            try
            {
                var match = await _db.PvPMatches
                    .Include(m => m.QuestionSet)
                    .FirstAsync(m => m.Id == matchId);

                if (match.QuestionSet == null)
                {
                    _logger.LogError("QuestionSet not found for match {MatchId}", matchId);
                    return;
                }

                var questions = await _questionSetService.GetQuestionsAsync(matchId);
                var allCountries = await _countryRepository.GetAllAsync();

                var questionDataList = new List<QuestionData>();

                foreach (var question in questions.OrderBy(q => match.QuestionSet.QuestionIds.IndexOf(q.Id)))
                {
                    var correctCountry = allCountries.First(c => c.Id == question.CountryId);

                    var rnd = new Random(match.QuestionSet.Seed + question.Id.GetHashCode());
                    var wrongCountries = allCountries
                        .Where(c => c.Id != correctCountry.Id)
                        .OrderBy(_ => rnd.Next())
                        .Take(3)
                        .ToList();

                    var allOptions = new List<Country> { correctCountry };
                    allOptions.AddRange(wrongCountries);
                    allOptions = allOptions.OrderBy(_ => rnd.Next()).ToList();

                    questionDataList.Add(new QuestionData
                    {
                        QuestionId = question.Id,
                        QuestionNumber = match.QuestionSet.QuestionIds.IndexOf(question.Id) + 1,
                        QuestionText = GetQuestionText(question.Type, correctCountry, language),
                        Options = allOptions.Select((c, idx) => new OptionData
                        {
                            Index = idx,
                            Text = match.SelectedMode == GameMode.Capital? GetCountryCapital(c, language) : GetCountryName(c, language)
                        }).ToList(),
                        ImageUrl = GetImageUrl(question.Type, correctCountry),
                        AudioUrl = GetAudioUrl(question.Type, correctCountry)
                    });
                }

                var startTime = DateTime.UtcNow.AddSeconds(2);
                _gameTimers[matchId] = new GameTimer { StartTime = startTime };

                await _notificationService.NotifyGameReady(matchId, new GameReadyData
                {
                    MatchId = matchId,
                    SelectedMode = match.SelectedMode!.Value,
                    Language = language,
                    TotalQuestions = questionDataList.Count,
                    TotalGameTimeSeconds = 60,
                    GameStartTime = startTime,
                    Questions = questionDataList
                });

                _logger.LogInformation("GameReady sent for match {MatchId}", matchId);

                _ = Task.Run(() => MonitorGameTimeAsync(matchId));
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error preparing game for match {MatchId}", matchId);
            }
        }

        public async Task SubmitAnswer(SubmitAnswerRequest request)
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} submitting answer for question {QuestionNumber} in match {MatchId}",
                userId, request.QuestionNumber, request.MatchId);

            try
            {
                var submitResponse = await _gameSessionService.SubmitAnswerAsync(request.MatchId, userId, request);

                var match = await _db.PvPMatches
                    .Include(m => m.QuestionSet)
                    .FirstAsync(m => m.Id == request.MatchId);

                var opponentId = match.Player1Id == userId
                    ? match.Player2Id
                    : match.Player1Id;

                var allAnswers = await _db.PvPAnswers
                    .Where(a => a.MatchId == request.MatchId)
                    .ToListAsync();

                var yourAnswers = allAnswers.Where(a => a.UserId == userId).ToList();
                var opponentAnswers = allAnswers.Where(a => a.UserId == opponentId).ToList();

                var yourAnswer = yourAnswers.First(a => a.QuestionId == request.QuestionId);
                var opponentAnswer = opponentAnswers.FirstOrDefault(a => a.QuestionId == request.QuestionId);

                var correctIndex = await CalculateCorrectOptionIndexAsync(request.QuestionId, match.QuestionSet!.Seed);

                var result = new QuestionResultData
                {
                    MatchId = request.MatchId,
                    QuestionNumber = request.QuestionNumber,
                    CorrectOptionIndex = correctIndex,

                    YourResult = new PlayerRoundResult
                    {
                        HasAnswered = true,
                        IsCorrect = yourAnswer.IsCorrect,
                        TimeSpentMs = yourAnswer.TimeSpentMs,
                        ScoreGained = yourAnswer.ScoreGained
                    },
                    OpponentResult = new PlayerRoundResult
                    {
                        HasAnswered = opponentAnswer != null,
                        IsCorrect = opponentAnswer?.IsCorrect ?? false,
                        TimeSpentMs = opponentAnswer?.TimeSpentMs ?? 0,
                        ScoreGained = opponentAnswer?.ScoreGained ?? 0
                    },
                    YourTotalScore = yourAnswers.Sum(a => a.ScoreGained),
                    OpponentTotalScore = opponentAnswers.Sum(a => a.ScoreGained),
                    YourCorrectCount = yourAnswers.Count(a => a.IsCorrect),
                    OpponentCorrectCount = opponentAnswers.Count(a => a.IsCorrect),
                    RemainingTimeSeconds = await GetRemainingTimeAsync(request.MatchId),
                    IsLastQuestion = request.QuestionNumber >= 10
                };

                await Clients.Caller.QuestionResult(result);

                if (yourAnswers.Count >= 10 && opponentAnswers.Count >= 10)
                {
                    await FinalizeGameAsync(request.MatchId, GameFinishReason.AllQuestionsAnswered);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in SubmitAnswer");
                throw new HubException(ex.Message);
            }
        }
        private async Task<int> CalculateCorrectOptionIndexAsync(string questionId, int seed)
        {
            var question = await _questionRepository.GetByIdAsync(questionId);
            var allCountries = await _countryRepository.GetAllAsync();
            var correctCountry = allCountries.First(c => c.Id == question.CountryId);

            var rnd = new Random(seed + questionId.GetHashCode());
            var wrongCountries = allCountries
                .Where(c => c.Id != correctCountry.Id)
                .OrderBy(_ => rnd.Next())
                .Take(3)
                .ToList();

            var options = new List<Country> { correctCountry };
            options.AddRange(wrongCountries);
            options = options.OrderBy(_ => rnd.Next()).ToList();

            return options.IndexOf(correctCountry);
        }

        private async Task<int> GetRemainingTimeAsync(Guid matchId)
        {
            if (_gameTimers.TryGetValue(matchId, out var timer))
            {
                var elapsed = (int)(DateTime.UtcNow - timer.StartTime).TotalSeconds;
                return Math.Max(0, 60 - elapsed);
            }
            return 60;
        }

        private async Task MonitorGameTimeAsync(Guid matchId)
        {
            if (!_gameTimers.TryGetValue(matchId, out var timer))
                return;

            try
            {
                while (!timer.Cts.Token.IsCancellationRequested)
                {
                    await Task.Delay(1000, timer.Cts.Token);

                    var remaining = await GetRemainingTimeAsync(matchId);

                    await _notificationService.NotifyTimerUpdate(matchId, new TimerUpdateData
                    {
                        MatchId = matchId,
                        RemainingTimeSeconds = remaining,
                        ServerTime = DateTime.UtcNow
                    });

                    if (remaining <= 0)
                    {
                        using (var scope = _serviceScopeFactory.CreateScope())
                        {
                            var scopedResultService = scope.ServiceProvider.GetRequiredService<IPvPResultService>();
                            var scopedDb = scope.ServiceProvider.GetRequiredService<AppDbContext>();

                            await FinalizeGameAsync(matchId, GameFinishReason.TimeOut, scopedResultService, scopedDb);
                            int a = 2;
                        }
                        break;
                    }
                }
            }
            catch (TaskCanceledException)
            {
                _logger.LogDebug("Game timer cancelled for match {MatchId}", matchId);
            }
        }
        private async Task FinalizeGameAsync(Guid matchId, GameFinishReason reason, Guid? userId = null)
        {
            await FinalizeGameAsync(matchId, reason, _resultService, _db, userId);
        }
        private async Task FinalizeGameAsync(Guid matchId, GameFinishReason reason, IPvPResultService resultService, AppDbContext db, Guid? userId = null)
        {
            _logger.LogInformation("Finalizing match {MatchId} with reason {Reason}", matchId, reason);

            try
            {
                if (_gameTimers.TryRemove(matchId, out var timer))
                {
                    timer.Cts.Cancel();
                }

                var match = await db.PvPMatches
                    .Include(m => m.Player1)
                    .Include(m => m.Player2)
                    .FirstAsync(m => m.Id == matchId);

                if (reason == GameFinishReason.OpponentDisconnected && match.Status == PvPMatchStatus.Drafting)
                {
                    var disconnectedPlayer = GetUserId();
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
                    _logger.LogInformation("Player {DisconnectedPlayer} disconnected during draft, notified {PlayerInDraft}",
                        disconnectedPlayer, playerInDraft);

                    _userCurrentMatch.TryRemove(match.Player1Id, out _);
                    _userCurrentMatch.TryRemove(match.Player2Id, out _);
                    _draftTimers.TryRemove(matchId, out _);

                    return;
                }

                var result = await resultService.FinalizeMatchAsync(matchId, reason, userId ?? Guid.Empty);

                var answers = await db.PvPAnswers
                    .Where(a => a.MatchId == matchId)
                    .ToListAsync();

                var p1Answers = answers.Where(a => a.UserId == match.Player1Id).ToList();
                var p2Answers = answers.Where(a => a.UserId == match.Player2Id).ToList();

                var p1Stats = new PlayerFinalStats
                {
                    UserId = match.Player1Id,
                    FinalScore = p1Answers.Sum(a => a.ScoreGained),
                    CorrectAnswers = p1Answers.Count(a => a.IsCorrect),
                    TotalQuestionsAnswered = p1Answers.Count,
                    AverageAnswerTimeMs = p1Answers.Any()
                        ? (int)p1Answers.Average(a => a.TimeSpentMs)
                        : 0
                };

                var p2Stats = new PlayerFinalStats
                {
                    UserId = match.Player2Id,
                    FinalScore = p2Answers.Sum(a => a.ScoreGained),
                    CorrectAnswers = p2Answers.Count(a => a.IsCorrect),
                    TotalQuestionsAnswered = p2Answers.Count,
                    AverageAnswerTimeMs = p2Answers.Any()
                        ? (int)p2Answers.Average(a => a.TimeSpentMs)
                        : 0
                };

                await _notificationService.NotifyGameFinished(match.Player1Id, new GameFinishedData
                {
                    MatchId = matchId,
                    WinnerId = result.WinnerId,
                    FinishReason = reason,
                    YourStats = p1Stats,
                    OpponentStats = p2Stats,
                    ExperienceGained = result.WinnerId == match.Player1Id ? p1Answers.Sum(a => a.ScoreGained) : 0,
                });

                await _notificationService.NotifyGameFinished(match.Player2Id, new GameFinishedData
                {
                    MatchId = matchId,
                    WinnerId = result.WinnerId,
                    FinishReason = reason,
                    YourStats = p2Stats,
                    OpponentStats = p1Stats,
                    ExperienceGained = result.WinnerId == match.Player2Id ? p2Answers.Sum(a => a.ScoreGained) : 0,
                });

                _userCurrentMatch.TryRemove(match.Player1Id, out _);
                _userCurrentMatch.TryRemove(match.Player2Id, out _);
                _draftTimers.TryRemove(matchId, out _);

                _logger.LogInformation("Game finished for match {MatchId}, winner: {WinnerId}",
                    matchId, result.WinnerId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error finalizing match {MatchId}", matchId);
            }
        }
        private string GetQuestionText(GameMode mode, Country country, AppLanguage language)
        {
            return mode switch
            {
                GameMode.Capital => language == AppLanguage.Ru
                    ? $"Столица страны {country.Name.Ru}?"
                    : $"Capital of {country.Name.En}?",

                GameMode.Flag => language == AppLanguage.Ru
                    ? "Флаг какой страны?"
                    : "Which country's flag?",

                GameMode.Outline => language == AppLanguage.Ru
                    ? "Контур какой страны?"
                    : "Which country's outline?",

                GameMode.Language => language == AppLanguage.Ru
                    ? $"Официальный язык {country.Name.Ru}?"
                    : $"Official language of {country.Name.En}?",

                _ => language == AppLanguage.Ru ? "Вопрос" : "Question"
            };
        }
        private string GetCountryCapital(Country country, AppLanguage language)
        {
            return language == AppLanguage.Ru ? country.Capital.Ru : country.Capital.En;
        }
        private string GetCountryName(Country country, AppLanguage language)
        {
            return language == AppLanguage.Ru ? country.Name.Ru : country.Name.En;
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
    }
}
