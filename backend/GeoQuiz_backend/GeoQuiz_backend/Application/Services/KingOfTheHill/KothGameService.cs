using Amazon.Runtime.Internal;
using GeoQuiz_backend.API.HubClients;
using GeoQuiz_backend.API.Hubs;
using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Services.Bots;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.Infrastructure.Factories;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using System.Collections.Concurrent;
using static System.Runtime.InteropServices.JavaScript.JSType;

namespace GeoQuiz_backend.Application.Services.KingOfTheHill
{
    public class KothGameService : IKothGameService
    {
        private readonly AppDbContext _db;
        private readonly IQuestionRepository _questionRepo;
        private readonly ICountryRepository _countryRepo;
        private readonly ISignalRNotificationService _notificationService;
        private readonly IServiceScopeFactory _serviceScopeFactory;
        private readonly ILogger<KothGameService> _logger;

        private static readonly ConcurrentDictionary<Guid, KothGameState> _activeGames = new();
        private static readonly ConcurrentDictionary<Guid, CancellationTokenSource> _roundTimers = new();

        public KothGameService(
            AppDbContext db,
            IQuestionRepository questionRepo,
            ICountryRepository countryRepo,
            ISignalRNotificationService notificationService,
            IServiceScopeFactory serviceScopeFactory,
            ILogger<KothGameService> logger)
        {
            _db = db;
            _questionRepo = questionRepo;
            _countryRepo = countryRepo;
            _notificationService = notificationService;
            _serviceScopeFactory = serviceScopeFactory;
            _logger = logger;
        }

        public async Task<KothMatch> StartMatchFromLobbyAsync(List<PlayerInfo> realPlayers, Guid lobbyId)
        {
            var matchId = Guid.NewGuid();
            var totalPlayers = realPlayers.Count;
            int botsNeeded = Math.Max(0, 32 - totalPlayers);
            
            
            var seed = new Random().Next();
            var availableModes = Enum.GetValues<GameMode>().ToList();
            var randomMode = availableModes[new Random().Next(availableModes.Count)];

            var allPlayers = new List<PlayerInfo>(realPlayers);
            if (botsNeeded > 0)
            {
                var bots = BotFactory.CreateBots(botsNeeded);
                allPlayers.AddRange(bots);

                _logger.LogInformation("Added {BotCount} bots to match. Total players: {Total}", botsNeeded, allPlayers.Count);
            }

            var maxQuestions = 2 * (allPlayers.Count - 1);
            _logger.LogInformation("Starting KOTH match {MatchId} with {PlayerCount} players, max questions: {MaxQuestions}, seed: {Seed}",
                matchId, totalPlayers, maxQuestions, seed);

            var questions = await GenerateQuestionsAsync(maxQuestions, seed, randomMode);

            var questionSet = new QuestionSet
            {
                Id = Guid.NewGuid(),
                KothMatchId = matchId,
                Mode = randomMode,
                Language = AppLanguage.Ru,
                Seed = seed,
                CreatedAt = DateTime.UtcNow,
                QuestionIds = questions.Select(q => q.QuestionId).ToList()
            };

            var match = new KothMatch
            {
                Id = matchId,
                Status = KothMatchStatus.InGame,
                SelectedMode = randomMode,
                CurrentRound = 0,
                CurrentRoundType = RoundType.Classic,
                CreatedAt = DateTime.UtcNow,
                StartedAt = DateTime.UtcNow,
                QuestionSet = questionSet
            };

            foreach (var player in realPlayers)
            {
                match.Players.Add(new KothPlayer
                {
                    Id = Guid.NewGuid(),
                    UserId = player.PlayerId,
                    MatchId = matchId,
                    JoinedAt = DateTime.UtcNow,
                    IsActive = true
                });
            }

            _db.KothMatches.Add(match);
            _db.QuestionSets.Add(questionSet);
            await _db.SaveChangesAsync();

            var gameState = new KothGameState
            {
                MatchId = matchId,
                Match = match, 
                ActivePlayerIds = allPlayers.Select(p => p.PlayerId).ToList(),
                Players = allPlayers.ToDictionary(
                    p => p.PlayerId,
                    p => new PlayerGameInfo
                    {
                        UserName = p.PlayerName,
                        Level = p.PlayerLevel,
                        IsActive = true,
                        IsBot = p.IsBot
                    }),
                EliminatedPlayers = new List<Guid>(),
                EliminatedThisRound = new List<Guid>(),
                CurrentRound = 0,
                CurrentRoundType = RoundType.Classic,
                Questions = questions,
                PlayerAnswers = allPlayers.ToDictionary(p => p.PlayerId, p => new Dictionary<int, PlayerAnswer>()),
                PlayerScores = allPlayers.ToDictionary(p => p.PlayerId, p => 0),
                PlayerCorrectCount = allPlayers.ToDictionary(p => p.PlayerId, p => 0),
                PlayerPlaces = new Dictionary<Guid, int>(),
                IsRoundStarting = false,
                IsRoundFinishing = false,
                IsRoundFinished = false,
                IsMatchFinishing = false,
                AnsweredPlayers = new HashSet<Guid>()
            };

            if (!_activeGames.TryAdd(matchId, gameState))
            {
                _logger.LogError("Failed to add game {MatchId} to active games", matchId);
                throw new Exception("Game already exists");
            }

            var matchStartedData = new MatchStartedData
            {
                MatchId = matchId,
                TotalPlayers = allPlayers.Count,
                TotalRounds = maxQuestions,
                FirstRoundStartTime = DateTime.UtcNow.AddSeconds(3),
                AllPlayers = allPlayers.Select(p => new PlayerInfo
                {
                    PlayerId = p.PlayerId,
                    PlayerName = p.PlayerName,
                    PlayerLevel = p.PlayerLevel,
                    IsBot = p.IsBot,
                }).ToList()
            };

            await _notificationService.NotifyMatchStarted(lobbyId, matchStartedData);

            _ = Task.Run(async () =>
            {
                await Task.Delay(250);
                await StartNextRoundAsync(matchId);
            });

            return match;
        }

        public async Task LeaveMatchAsync(Guid userId, Guid matchId)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
                throw new Exception("Game not found");

            bool shouldFinishRoundEarly = false;
            bool shouldFinishMatch = false;

            lock (gameState)
            {
                if (!gameState.ActivePlayerIds.Contains(userId))
                {
                    _logger.LogWarning("User {UserId} already not active in match {MatchId}", userId, matchId);
                    return;
                }
                gameState.ActivePlayerIds.Remove(userId);
                if (!gameState.EliminatedPlayers.Contains(userId))
                {
                    gameState.EliminatedPlayers.Add(userId);
                }
                if (!gameState.EliminatedThisRound.Contains(userId))
                {
                    gameState.EliminatedThisRound.Add(userId);
                }
                gameState.Players[userId].IsActive = false;
                gameState.Players[userId].EliminatedAtRound = gameState.CurrentRound;
                gameState.PlayerPlaces[userId] = gameState.ActivePlayerIds.Count + 1;


                if (gameState.ActivePlayerIds.Count <= 1)
                {
                    shouldFinishMatch = true;
                }
                else
                {
                    var eliminatedAnswered = 0;
                    foreach (var playerId in gameState.EliminatedThisRound)
                    {
                        var hasAnswered = gameState.PlayerAnswers.ContainsKey(playerId) && gameState.PlayerAnswers[playerId].ContainsKey(gameState.CurrentRound);
                        if (hasAnswered) eliminatedAnswered++;
                    }
                    shouldFinishRoundEarly = gameState.AnsweredPlayers.Count >= gameState.ActivePlayerIds.Count + eliminatedAnswered;
                }
            }

            if (shouldFinishMatch)
            {
                _logger.LogInformation("Only one player left in match {MatchId}, finishing", matchId);

                if (_roundTimers.TryRemove(matchId, out var cts))
                {
                    cts.Cancel();
                    cts.Dispose();
                }
                _ = Task.Run(() => FinishMatchAsync(matchId));
                return;
            }
            if (shouldFinishRoundEarly)
            {
                _logger.LogInformation("All players answered after user left, finishing round early for match {MatchId}", matchId);
                var data = new PlayerEliminatedData
                {
                    PlayerId = userId,
                    RoundsSurvived = gameState.CurrentRound,
                    Place = gameState.PlayerPlaces[userId],
                    CorrectAnswers = gameState.PlayerCorrectCount[userId],
                    TotalScore = gameState.PlayerScores[userId],
                    IsManuallyDisabled = true
                };

                await _notificationService.NotifyPlayerEliminated(userId, data);

                if (_roundTimers.TryRemove(matchId, out var cts))
                {
                    cts.Cancel();
                    cts.Dispose();
                }

                _ = Task.Run(() => FinishRoundAsync(matchId));
            }
        }

        public async Task<RoundStartedData?> StartNextRoundAsync(Guid matchId)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
            {
                _logger.LogWarning("Game {MatchId} not found", matchId);
                return null;
            }

            RoundStartedData roundStartedData;
            CancellationTokenSource cts;

            lock (gameState)
            {
                if (gameState.IsRoundStarting || gameState.IsRoundFinished || gameState.IsMatchFinishing)
                {
                    _logger.LogInformation("Match {MatchId} cannot start round: Starting={IsRoundStarting}, Finished={IsRoundFinished}, Finishing={IsMatchFinishing}",
                        matchId, gameState.IsRoundStarting, gameState.IsRoundFinished, gameState.IsMatchFinishing);
                    return null;
                }

                if (gameState.ActivePlayerIds.Count <= 1)
                {
                    _ = FinishMatchAsync(matchId);
                    return null;
                }

                gameState.IsRoundStarting = true;

                gameState.CurrentRound++;
                if (gameState.CurrentRound == 1)
                {
                    gameState.CurrentRoundType = RoundType.Classic;
                }
                else
                {
                    var eliminatedInPreviousRound = gameState.EliminatedThisRound.Count;
                    gameState.CurrentRoundType = eliminatedInPreviousRound == 0
                        ? RoundType.Speed
                        : RoundType.Classic;
                }

                if (gameState.CurrentRound > gameState.Questions.Count)
                {
                    gameState.IsRoundStarting = false;
                    return null;
                }

                var question = gameState.Questions[gameState.CurrentRound - 1];
                gameState.RoundStartTime = DateTime.UtcNow;
                gameState.EliminatedThisRound.Clear();
                gameState.AnsweredPlayers.Clear();

                roundStartedData = new RoundStartedData
                {
                    RoundNumber = gameState.CurrentRound,
                    RoundType = gameState.CurrentRoundType == RoundType.Classic ? 1 : 2,
                    Question = MapToQuestionData(question),
                    RoundStartTime = gameState.RoundStartTime,
                    TimeLimitSeconds = 10
                };

                cts = new CancellationTokenSource();
                _roundTimers[matchId] = cts;
            }

            try
            {
                await _notificationService.NotifyRoundStarted(matchId, roundStartedData);
                await ProcessRoundAnswers(matchId);

                _ = Task.Run(async () =>
                {
                    try
                    {
                        await Task.Delay(TimeSpan.FromSeconds(10), cts.Token);

                        bool shouldFinish;
                        lock (gameState)
                        {
                            shouldFinish = !gameState.IsRoundFinished && !gameState.IsMatchFinishing;
                        }

                        if (shouldFinish)
                        {
                            await FinishRoundAsync(matchId);
                        }
                    }
                    catch (TaskCanceledException)
                    {
                        _logger.LogDebug("Round timer cancelled for match {MatchId}", matchId);
                    }
                }, cts.Token);
            }
            finally
            {
                lock (gameState)
                {
                    gameState.IsRoundStarting = false;
                }
            }

            return roundStartedData;
        }

        public async Task<AnswerResultData> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
                throw new Exception("Game not found");

            lock (gameState)
            {
                if (!gameState.ActivePlayerIds.Contains(userId))
                    throw new Exception("Player is eliminated");

                if (gameState.IsRoundFinished)
                    throw new Exception("Round already finished");

                if (gameState.AnsweredPlayers.Contains(userId))
                    throw new Exception("Already answered this round");

                if (request.RoundNumber > gameState.Questions.Count)
                    throw new Exception("Invalid round number");

                gameState.AnsweredPlayers.Add(userId);
            }
            _logger.LogInformation("Answer submitting for  match {MatchId} with players, time Spents: {TimeSpentMs}", matchId, request.TimeSpentMs);
            var question = gameState.Questions[request.RoundNumber - 1];
            var isCorrect = request.SelectedOptionIndex == question.CorrectOptionIndex;
            var scoreGained = isCorrect ? CalculateScore(request.TimeSpentMs) : 0;

            var answer = new PlayerAnswer
            {
                QuestionId = request.QuestionId,
                IsCorrect = isCorrect,
                TimeSpentMs = request.TimeSpentMs,
                ScoreGained = scoreGained,
                AnsweredAt = DateTime.UtcNow
            };

            lock (gameState)
            {
                gameState.PlayerAnswers[userId][request.RoundNumber] = answer;

                if (isCorrect)
                {
                    gameState.PlayerScores[userId] = gameState.PlayerScores.GetValueOrDefault(userId) + scoreGained;
                    gameState.PlayerCorrectCount[userId] = gameState.PlayerCorrectCount.GetValueOrDefault(userId) + 1;
                }
            }

            var result = new AnswerResultData
            {
                IsCorrect = isCorrect,
                ScoreGained = scoreGained,
                TimeSpentMs = request.TimeSpentMs,
                RemainingPlayers = gameState.ActivePlayerIds.Count,
                CorrectOptionIndex = question.CorrectOptionIndex
            };

            var kothAnswer = new KothAnswer
            {
                Id = Guid.NewGuid(),
                MatchId = matchId,
                UserId = userId,
                QuestionId = answer.QuestionId,
                RoundNumber = request.RoundNumber,
                IsCorrect = answer.IsCorrect,
                TimeSpentMs = answer.TimeSpentMs,
                ScoreGained = answer.ScoreGained,
                AnsweredAt = answer.AnsweredAt
            };

            lock (gameState)
            {
                gameState.PendingAnswers.Add(kothAnswer);
            }

            bool allAnswered = false;
            lock (gameState)
            {
                var eliminatedAnswered = 0;
                foreach (var playerId in gameState.EliminatedThisRound)
                {
                    var hasAnswered = gameState.PlayerAnswers.ContainsKey(playerId) && gameState.PlayerAnswers[playerId].ContainsKey(gameState.CurrentRound);
                    if (hasAnswered) eliminatedAnswered++;
                }
                allAnswered = gameState.AnsweredPlayers.Count >= gameState.ActivePlayerIds.Count + eliminatedAnswered;
            }

            if (allAnswered)
            {
                if (_roundTimers.TryRemove(matchId, out var cts))
                {
                    cts.Cancel();
                    cts.Dispose();
                }
                
                _ = Task.Run(() => FinishRoundAsync(matchId));
            }

            return result;
        }
        public async Task ProcessRoundAnswers(Guid matchId)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
                throw new Exception("Game not found");

            lock (gameState)
            {
                var activeBots = gameState.ActivePlayerIds
                    .Where(id => gameState.Players[id].IsBot)
                    .ToList();

                foreach (var botId in activeBots)
                {
                    if (!gameState.AnsweredPlayers.Contains(botId))
                    {
                        var bot = gameState.Players[botId];
                        var question = gameState.Questions[gameState.CurrentRound - 1];

                        var botAnswer = BotAnswerService.GenerateAnswer(new PlayerInfo
                        {
                            PlayerId = botId,
                            PlayerName = bot.UserName,
                            PlayerLevel = bot.Level,
                            IsBot = bot.IsBot,
                        }, question);

                        var answer = new PlayerAnswer
                        {
                            QuestionId = question.QuestionId,
                            IsCorrect = botAnswer.IsCorrect,
                            TimeSpentMs = botAnswer.ResponseTimeMs,
                            ScoreGained = botAnswer.IsCorrect ? CalculateScore(botAnswer.ResponseTimeMs) : 0,
                            AnsweredAt = DateTime.UtcNow
                        };

                        gameState.PlayerAnswers[botId][gameState.CurrentRound] = answer;
                        gameState.AnsweredPlayers.Add(botId);

                        if (botAnswer.IsCorrect)
                        {
                            gameState.PlayerScores[botId] += answer.ScoreGained;
                            gameState.PlayerCorrectCount[botId]++;
                        }

                        _logger.LogDebug("Bot {BotId} answered {Result} in {Time}ms",
                            botId, botAnswer.IsCorrect ? "correctly" : "incorrectly", botAnswer.ResponseTimeMs);
                    }
                }
            }
        }
        public async Task<RoundFinishedData> FinishRoundAsync(Guid matchId)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
                throw new Exception("Game not found");

            lock (gameState)
            {
                if (gameState.IsRoundFinishing)
                {
                    _logger.LogWarning("Round already finishing for match {MatchId}", matchId);
                    return null!;
                }

                if (gameState.IsRoundFinished)
                {
                    _logger.LogWarning("Round already finished for match {MatchId}", matchId);
                    return null!;
                }

                if (gameState.IsMatchFinishing)
                {
                    _logger.LogWarning("Match is finishing for {MatchId}", matchId);
                    return null!;
                }

                gameState.IsRoundFinishing = true;
            }

            try
            {
                if (_roundTimers.TryRemove(matchId, out var cts))
                {
                    cts.Cancel();
                    cts.Dispose();
                }

                Dictionary<Guid, PlayerRoundResult> playerResults;
                List<Guid> eliminatedThisRound;
                List<KothAnswer> pendingAnswers = new List<KothAnswer>();

                lock (gameState)
                {
                    var currentRound = gameState.CurrentRound;
                    playerResults = new Dictionary<Guid, PlayerRoundResult>();
                    eliminatedThisRound = new List<Guid>();

                    foreach (var playerId in gameState.ActivePlayerIds.ToList())
                    {
                        var hasAnswered = gameState.PlayerAnswers.ContainsKey(playerId) && gameState.PlayerAnswers[playerId].ContainsKey(currentRound);

                        PlayerRoundResult result;

                        if (!hasAnswered)
                        {
                            result = new PlayerRoundResult
                            {
                                PlayerId = playerId,
                                HasAnswered = false,
                                IsCorrect = false,
                                TimeSpentMs = 0,
                                ScoreGained = 0
                            };
                            eliminatedThisRound.Add(playerId);
                        }
                        else
                        {
                            var answer = gameState.PlayerAnswers[playerId][currentRound];
                            result = new PlayerRoundResult
                            {
                                PlayerId = playerId,
                                HasAnswered = true,
                                IsCorrect = answer.IsCorrect,
                                TimeSpentMs = answer.TimeSpentMs,
                                ScoreGained = answer.ScoreGained
                            };

                            if (!answer.IsCorrect)
                            {
                                eliminatedThisRound.Add(playerId);
                            }
                        }

                        playerResults[playerId] = result;
                    }

                    if (gameState.CurrentRoundType == RoundType.Speed)
                    {
                        var correctAnswers = playerResults
                            .Where(kvp => kvp.Value.IsCorrect)
                            .Select(kvp => kvp.Key)
                            .ToList();

                        if (correctAnswers.Count > 1)
                        {
                            var slowest = correctAnswers
                                .OrderByDescending(id => playerResults[id].TimeSpentMs)
                                .First();

                            if (!eliminatedThisRound.Contains(slowest))
                            {
                                eliminatedThisRound.Add(slowest);
                            }
                        }
                    }
                    foreach (var playerId in eliminatedThisRound)
                    {
                        gameState.ActivePlayerIds.Remove(playerId);
                        gameState.EliminatedPlayers.Add(playerId);
                        gameState.EliminatedThisRound.Add(playerId);
                        gameState.Players[playerId].IsActive = false;
                        gameState.Players[playerId].EliminatedAtRound = currentRound;

                    }

                    var eliminatedWithStats = eliminatedThisRound
                        .Select(id => new
                        {
                            PlayerId = id,
                            Score = gameState.PlayerScores[id],
                            TimeSpent = gameState.PlayerAnswers.ContainsKey(id) &&
                                        gameState.PlayerAnswers[id].ContainsKey(currentRound)
                                        ? gameState.PlayerAnswers[id][currentRound].TimeSpentMs
                                        : int.MaxValue 
                        })
                        .OrderBy(x => x.Score)          
                        .ThenByDescending(x => x.TimeSpent) 
                        .ToList();

                    int currentPlace = gameState.ActivePlayerIds.Count + eliminatedWithStats.Count;
                    foreach (var item in eliminatedWithStats)
                    {
                        gameState.PlayerPlaces[item.PlayerId] = currentPlace;
                        currentPlace--;
                    }

                    pendingAnswers = gameState.PendingAnswers.ToList();
                    gameState.PendingAnswers.Clear();
                    gameState.IsRoundFinished = true;
                }

                if (pendingAnswers.Any())
                {
                    await Task.Run(async () =>
                    {
                        using (var scope = _serviceScopeFactory.CreateScope())
                        {
                            var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
                            dbContext.KothAnswers.AddRange(pendingAnswers);
                            await dbContext.SaveChangesAsync();
                            _logger.LogInformation("Saved {Count} answers for match {MatchId}", pendingAnswers.Count, matchId);
                        }
                    });
                }

                var roundFinishedData = new RoundFinishedData
                {
                    RoundNumber = gameState.CurrentRound,
                    RoundType = gameState.CurrentRoundType == RoundType.Classic ? 1 : 2,
                    CorrectOptionIndex = gameState.Questions[gameState.CurrentRound - 1].CorrectOptionIndex,
                    EliminatedPlayerIds = eliminatedThisRound,
                    Results = playerResults.Values.ToList(),
                    RemainingPlayers = gameState.ActivePlayerIds.Count,
                    IsMatchFinished = gameState.ActivePlayerIds.Count <= 1
                };

                await _notificationService.NotifyRoundFinished(matchId, roundFinishedData);
                _logger.LogInformation("Finish round {RoundNumber} for {matchId}", roundFinishedData.RoundNumber ,matchId);


                lock (gameState)
                {
                    gameState.IsRoundFinishing = false;
                    gameState.IsRoundFinished = false;
                }

                if (roundFinishedData.IsMatchFinished)
                {
                    _ = Task.Run(() => FinishMatchAsync(matchId));
                }
                else
                {
                    foreach (var playerId in eliminatedThisRound)
                    {
                        var data = new PlayerEliminatedData
                        {
                            PlayerId = playerId,
                            RoundsSurvived = roundFinishedData.RoundNumber,
                            Place = gameState.PlayerPlaces[playerId],
                            CorrectAnswers = gameState.PlayerCorrectCount[playerId],
                            TotalScore = gameState.PlayerScores[playerId],
                            IsManuallyDisabled = false
                        };

                        await _notificationService.NotifyPlayerEliminated(playerId, data);
                    }
                    _ = Task.Run(() => StartNextRoundAsync(matchId));
                }

                return roundFinishedData;
            }
            finally
            {
                //lock (gameState)
                //{
                //    gameState.IsRoundFinishing = false;
                //    gameState.IsRoundFinished = false;
                //}
            }
        }
        public async Task<MatchFinishedData> FinishMatchAsync(Guid matchId)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
                throw new Exception("Game not found");

            lock (gameState)
            {
                if (gameState.IsMatchFinishing)
                {
                    _logger.LogWarning("Match already finishing for {MatchId}", matchId);
                    return null!;
                }

                gameState.IsMatchFinishing = true;
            }

            try
            {
                _activeGames.TryRemove(matchId, out _);

                if (_roundTimers.TryRemove(matchId, out var cts))
                {
                    cts.Cancel();
                    cts.Dispose();
                }

                List<PlayerFinalStanding> finalStandings;
                Dictionary<Guid, PlayerFinalStanding> standings;
                Guid? winnerId;
                GameMode mode;

                lock (gameState)
                {
                    winnerId = gameState.ActivePlayerIds.Count == 1
                        ? gameState.ActivePlayerIds.First()
                        : null;

                    mode = gameState.Match.SelectedMode;
                    finalStandings = new List<PlayerFinalStanding>();
                    standings = new Dictionary<Guid, PlayerFinalStanding>();

                    if (winnerId.HasValue)
                    {
                        var standing = new PlayerFinalStanding
                        {
                            PlayerId = winnerId.Value,
                            PlayerName = gameState.Players[winnerId.Value].UserName,
                            Place = 1,
                            CorrectAnswers = gameState.PlayerCorrectCount[winnerId.Value],
                            TotalScore = gameState.PlayerScores[winnerId.Value],
                            RoundsSurvived = gameState.CurrentRound
                        };
                        finalStandings.Add(standing);
                        standings[winnerId.Value] = standing;
                    }

                    var eliminatedPlayers = gameState.EliminatedPlayers
                        .Select(id => new { id, place = gameState.PlayerPlaces[id] })
                        .OrderBy(x => x.place);

                    foreach (var item in eliminatedPlayers)
                    {
                        var standing = new PlayerFinalStanding
                        {
                            PlayerId = item.id,
                            PlayerName = gameState.Players[item.id].UserName,
                            Place = item.place,
                            CorrectAnswers = gameState.PlayerCorrectCount.GetValueOrDefault(item.id),
                            TotalScore = gameState.PlayerScores.GetValueOrDefault(item.id),
                            RoundsSurvived = gameState.Players[item.id].EliminatedAtRound
                        };
                        finalStandings.Add(standing);
                        standings[item.id] = standing;
                    }
                }

                var matchFinishedData = new MatchFinishedData
                {
                    MatchId = matchId,
                    WinnerId = winnerId ?? Guid.Empty,
                    FinalStandings = finalStandings
                };

                var realStandings = new Dictionary<Guid, PlayerFinalStanding>();

                foreach (var item in standings)
                {
                    lock (gameState)
                    {
                        if (gameState.Players.TryGetValue(item.Key, out var player) && !player.IsBot)
                        {
                            realStandings.Add(item.Key, item.Value);
                        }
                    }
                }


                using (var scope = _serviceScopeFactory.CreateScope())
                {
                    var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
                    await UpdateDatabaseWithResults(dbContext, matchId, realStandings, mode, matchFinishedData);
                    await CreateGameSessionsAsync(dbContext, matchId, realStandings, mode);
                    await dbContext.SaveChangesAsync();
                    _logger.LogInformation("Saved finish match for match {MatchId}", matchId);
                }
                
                //await _db.SaveChangesAsync();

                _logger.LogInformation("Finish match {matchId}", matchId);
                await _notificationService.NotifyMatchFinished(matchId, matchFinishedData);

                return matchFinishedData;
            }
            finally
            {
                lock (gameState)
                {
                    gameState.IsMatchFinishing = false;
                }
            }
        }

        public Task<KothGameState?> GetGameStateAsync(Guid matchId)
        {
            _activeGames.TryGetValue(matchId, out var gameState);
            return Task.FromResult(gameState);
        }

        private async Task<List<GameQuestion>> GenerateQuestionsAsync(int count, int seed, GameMode mode)
        {
            var questions = new List<GameQuestion>();
            var allQuestions = await _questionRepo.GetByTypeAsync(mode);
            var allCountries = await _countryRepo.GetAllAsync();

            var random = new Random(seed); 

            var selectedQuestions = allQuestions
                .OrderBy(x => random.Next())
                .Take(count)
                .ToList();

            foreach (var question in selectedQuestions)
            {
                var country = allCountries.First(c => c.Id == question.CountryId);

                var optionRandom = new Random(seed + question.Id.GetHashCode());

                var options = new List<GameOption>();
                var correctOption = new GameOption
                {
                    Index = 0,
                    Text = GetLocalizedTextForCountry(country, question.Type)
                };

                var wrongCountries = allCountries
                    .Where(c => c.Id != country.Id)
                    .OrderBy(x => optionRandom.Next())
                    .Take(3)
                    .ToList();

                options.Add(correctOption);
                foreach (var wrongCountry in wrongCountries)
                {
                    options.Add(new GameOption
                    {
                        Index = options.Count,
                        Text = GetLocalizedTextForCountry(wrongCountry, question.Type)
                    });
                }

                options = options.OrderBy(x => optionRandom.Next()).ToList();

                var correctIndex = options.FindIndex(o =>
                    o.Text.Ru == correctOption.Text.Ru &&
                    o.Text.En == correctOption.Text.En);

                questions.Add(new GameQuestion
                {
                    QuestionId = question.Id,
                    QuestionText = GetQuestionLocalizedText(question.Type, country),
                    Options = options,
                    CorrectOptionIndex = correctIndex,
                    ImageUrl = GetImageUrl(question.Type, country),
                    AudioUrl = GetAudioUrl(question.Type, country)
                });
            }

            return questions;
        }

        private QuestionData MapToQuestionData(GameQuestion question)
        {
            return new QuestionData
            {
                QuestionId = question.QuestionId,
                QuestionText = question.QuestionText,
                Options = question.Options.Select(o => new OptionData
                {
                    Index = o.Index,
                    Text = o.Text
                }).ToList(),
                ImageUrl = question.ImageUrl,
                AudioUrl = question.AudioUrl
            };
        }
        private async Task UpdateDatabaseWithResults(AppDbContext db ,Guid matchId, Dictionary<Guid, PlayerFinalStanding> standings, GameMode mode, MatchFinishedData matchFinishedData)
        {
            var dbMatch = await db.KothMatches
                .Include(m => m.Players)
                .FirstOrDefaultAsync(m => m.Id == matchId);

            if (dbMatch == null) return;

            dbMatch.Status = KothMatchStatus.Finished;
            dbMatch.FinishedAt = DateTime.UtcNow;
            dbMatch.WinnerId = standings.ContainsKey(matchFinishedData.WinnerId) ? matchFinishedData.WinnerId : null;

            foreach (var player in dbMatch.Players)
            {
                if (standings.TryGetValue(player.UserId, out var standing))
                {
                    player.Place = standing.Place;
                    player.IsActive = standing.Place == 1;
                    player.RoundEliminated = standing.RoundsSurvived;
                }
            }

            //await _db.SaveChangesAsync();
        }
        private async Task CreateGameSessionsAsync(AppDbContext db , Guid matchId, Dictionary<Guid, PlayerFinalStanding> standings, GameMode mode)
        {
            foreach (var (userId, standing) in standings)
            {
                var answers = await db.KothAnswers
                    .Where(a => a.MatchId == matchId && a.UserId == userId)
                    .ToListAsync();

                var gameSession = new GameSession
                {
                    Id = Guid.NewGuid(),
                    UserId = userId,
                    KothMatchId = matchId,
                    Mode = mode,
                    TotalQuestions = answers.Count,
                    CorrectAnswers = answers.Count(a => a.IsCorrect),
                    Score = answers.Sum(a => a.ScoreGained),
                    IsOnline = true,
                    PlayedAt = DateTime.UtcNow,
                    Place = standing.Place,
                    RoundsSurvived = standing.RoundsSurvived
                };

                db.GameSessions.Add(gameSession);

                await UpdateUserStatsAsync(db ,userId, gameSession, standing.Place == 1);
            }

            //await _db.SaveChangesAsync();
        }



        private async Task UpdateUserStatsAsync(AppDbContext db,Guid userId, GameSession session, bool isWinner)
        {
            var user = await db.Users
                .Include(u => u.Stats)
                .FirstOrDefaultAsync(u => u.Id == userId);

            if (user?.Stats == null) return;

            var stats = user.Stats;

            stats.TotalGamesPlayed++;
            stats.KothGamesPlayed++;

            if (isWinner)
            {
                stats.TotalGamesWon++;
                stats.KothGamesWon++;
            }

            if (session.Place <= 3)
            {
                stats.KothTop3Finishes++;
                stats.Experience += session.Score;

                if (stats.Experience >= GetXpToNextLevel(stats.Level))
                {
                    stats.Experience -= GetXpToNextLevel(stats.Level);
                    stats.Level++;
                }
            }


            //await _db.SaveChangesAsync();
        }

        private int GetXpToNextLevel(int level) => level * 100;
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

        private int CalculateScore(int timeSpentMs)
        {
            var maxScore = 10;
            var penalty = timeSpentMs / 1000;
            return Math.Max(1, maxScore - penalty);
        }
    }
}
