using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Services.Bots;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.Infrastructure.Factories;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using System.Collections.Concurrent;

namespace GeoQuiz_backend.Application.Services.KingOfTheHill
{
    public class KothGameService : IKothGameService
    {
        private static readonly ConcurrentDictionary<Guid, KothGameState> _activeGames = new();
        private static readonly ConcurrentDictionary<Guid, CancellationTokenSource> _roundTimers = new();

        private readonly AppDbContext _db;
        private readonly IQuestionRepository _questionRepo;
        private readonly ICountryRepository _countryRepo;
        private readonly ISignalRNotificationService _notificationService;
        private readonly IServiceScopeFactory _serviceScopeFactory;
        private readonly IKothRoundService _roundService;
        private readonly IKothResultService _resultService;
        private readonly ILogger<KothGameService> _logger;

        public KothGameService(
            AppDbContext db,
            IQuestionRepository questionRepo,
            ICountryRepository countryRepo,
            ISignalRNotificationService notificationService,
            IServiceScopeFactory serviceScopeFactory,
            IKothRoundService roundService,
            IKothResultService resultService,
            ILogger<KothGameService> logger)
        {
            _db = db;
            _questionRepo = questionRepo;
            _countryRepo = countryRepo;
            _notificationService = notificationService;
            _serviceScopeFactory = serviceScopeFactory;
            _roundService = roundService;
            _resultService = resultService;
            _logger = logger;
        }

        public async Task StartMatchFromLobbyAsync(List<PlayerInfo> realPlayers, Guid lobbyId)
        {
            var matchId = Guid.NewGuid();
            var gameState = await CreateGameStateAsync(matchId, realPlayers);

            _activeGames[matchId] = gameState;

            var matchStartedData = new MatchStartedData
            {
                MatchId = matchId,
                TotalPlayers = gameState.ActivePlayerIds.Count,
                TotalRounds = gameState.Questions.Count,
                FirstRoundStartTime = DateTime.UtcNow.AddSeconds(3),
                AllPlayers = gameState.ActivePlayerIds.Select(id => new PlayerInfo
                {
                    PlayerId = id,
                    PlayerName = gameState.Players[id].UserName,
                    PlayerLevel = gameState.Players[id].Level,
                    IsBot = gameState.Players[id].IsBot
                }).ToList()
            };

            await _notificationService.NotifyMatchStarted(lobbyId, matchStartedData);

            _ = Task.Run(async () =>
            {
                await Task.Delay(250);
                await StartNextRoundAsync(matchId);
            });
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
                gameState.EliminatedPlayers.Add(userId);
                gameState.EliminatedThisRound.Add(userId);
                gameState.Players[userId].IsActive = false;
                gameState.Players[userId].EliminatedAtRound = gameState.CurrentRound;
                gameState.PlayerPlaces[userId] = gameState.ActivePlayerIds.Count + 1;


                shouldFinishMatch = gameState.ActivePlayerIds.Count <= 1;
                shouldFinishRoundEarly = !shouldFinishMatch && _roundService.IsRoundComplete(gameState);
            }

            if (shouldFinishMatch)
            {
                _logger.LogInformation("Only one player left in match {MatchId}, finishing", matchId);

                if (_roundTimers.TryRemove(matchId, out var cts))
                {
                    cts.Cancel();
                    cts.Dispose();
                }

                await FinishMatchAsync(matchId); 
                return;
            }
            if (shouldFinishRoundEarly)
            {
                _logger.LogInformation("All players answered after user left, finishing round early for match {MatchId}", matchId);
                await _notificationService.NotifyPlayerEliminated(userId, new PlayerEliminatedData
                {
                    PlayerId = userId,
                    RoundsSurvived = gameState.CurrentRound,
                    Place = gameState.PlayerPlaces[userId],
                    CorrectAnswers = gameState.PlayerCorrectCount.GetValueOrDefault(userId),
                    TotalScore = gameState.PlayerScores.GetValueOrDefault(userId),
                    IsManuallyDisabled = true
                });

                if (_roundTimers.TryRemove(matchId, out var cts))
                {
                    cts.Cancel();
                    cts.Dispose();
                }

                await FinishRoundAsync(matchId); 
            }
        }
        public async Task StartNextRoundAsync(Guid matchId)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
            {
                _logger.LogWarning("Game {MatchId} not found when starting next round", matchId);
                return;
            }

            if (gameState.ActivePlayerIds.Count <= 1)
            {
                _logger.LogInformation("No active players left in match {MatchId}, finishing", matchId);
                await FinishMatchAsync(matchId);
                return;
            }

            var roundData = _roundService.StartRound(gameState);
            

            if (roundData == null)
            {
                _logger.LogWarning("Failed to start round {RoundNumber} for match {MatchId}",
                    gameState.CurrentRound + 1, matchId);
                await FinishMatchAsync(matchId);
                return;
            }

            _logger.LogInformation("Starting round {RoundNumber} (type: {RoundType}) for match {MatchId} with {ActivePlayers} active players",
                roundData.RoundNumber, roundData.RoundType == 1 ? "Classic" : "Speed", matchId, gameState.ActivePlayerIds.Count);

            await _notificationService.NotifyRoundStarted(matchId, roundData);
            _ = Task.Run(async () => _roundService.ProcessBotAnswers(gameState, matchId));

            var cts = new CancellationTokenSource();
            _roundTimers[matchId] = cts;

            try
            {
                await Task.Delay(TimeSpan.FromSeconds(10), cts.Token);

                if (!cts.Token.IsCancellationRequested)
                {
                    _logger.LogInformation("Round {RoundNumber} timer expired for match {MatchId}",
                        gameState.CurrentRound, matchId);
                    await FinishRoundAsync(matchId);
                }
            }
            catch (TaskCanceledException)
            {
                _logger.LogDebug("Round {RoundNumber} timer cancelled for match {MatchId}",
                    gameState.CurrentRound, matchId);
            }
            finally
            {
                _roundTimers.TryRemove(matchId, out _);
                cts.Dispose();
            }
        }
        
        public async Task<AnswerResultData> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
                throw new Exception("Game not found");

            lock (gameState)
            {
                if (!gameState.ActivePlayerIds.Contains(userId))
                    throw new Exception("Player eliminated");

                if (gameState.IsRoundFinished)
                    throw new Exception("Round already finished");

                if (gameState.AnsweredPlayers.Contains(userId))
                    throw new Exception("Already answered");

                gameState.AnsweredPlayers.Add(userId);
            }

            var result = _roundService.ProcessAnswer(gameState, userId, request);

            lock (gameState)
            {
                gameState.PendingAnswers.Add(new KothAnswer
                {
                    Id = Guid.NewGuid(),
                    MatchId = matchId,
                    UserId = userId,
                    QuestionId = request.QuestionId,
                    RoundNumber = request.RoundNumber,
                    IsCorrect = result.IsCorrect,
                    TimeSpentMs = request.TimeSpentMs,
                    ScoreGained = result.ScoreGained,
                    AnsweredAt = DateTime.UtcNow
                });
            }

            if (_roundService.IsRoundComplete(gameState))
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
        public async Task FinishRoundAsync(Guid matchId)
        {
            if (!_activeGames.TryGetValue(matchId, out var gameState))
            {
                _logger.LogWarning("Game {MatchId} not found when finishing round", matchId);
                return;
            }

            _logger.LogInformation("Finishing round {RoundNumber} for match {MatchId}",
                gameState.CurrentRound, matchId);

            var eliminated = _roundService.FinishRound(gameState);

            _logger.LogInformation("Round {RoundNumber} eliminated {EliminatedCount} players",
                gameState.CurrentRound, eliminated.Count);

            foreach (var playerId in eliminated)
            {
                lock (gameState)
                {
                    if (gameState.ActivePlayerIds.Contains(playerId))
                    {
                        gameState.ActivePlayerIds.Remove(playerId);
                    }
                    if (!gameState.EliminatedPlayers.Contains(playerId))
                    {
                        gameState.EliminatedPlayers.Add(playerId);
                    }
                    if (!gameState.EliminatedThisRound.Contains(playerId))
                    {
                        gameState.EliminatedThisRound.Add(playerId);
                    }
                    gameState.Players[playerId].IsActive = false;
                    gameState.Players[playerId].EliminatedAtRound = gameState.CurrentRound;
                }
            }

            _roundService.AssignPlaces(gameState, eliminated);

            await SavePendingAnswersAsync(gameState);

            var roundFinishedData = new RoundFinishedData
            {
                RoundNumber = gameState.CurrentRound,
                RoundType = gameState.CurrentRoundType == RoundType.Classic ? 1 : 2,
                CorrectOptionIndex = gameState.Questions[gameState.CurrentRound - 1].CorrectOptionIndex,
                EliminatedPlayerIds = eliminated,
                Results = GetPlayerResults(gameState),
                RemainingPlayers = gameState.ActivePlayerIds.Count,
                IsMatchFinished = gameState.ActivePlayerIds.Count <= 1
            };

            await _notificationService.NotifyRoundFinished(matchId, roundFinishedData);
            _logger.LogInformation("Round {RoundNumber} finished for match {MatchId}, {Remaining} players remaining",
                gameState.CurrentRound, matchId, gameState.ActivePlayerIds.Count);

            foreach (var playerId in eliminated)
            {
                var eliminatedData = new PlayerEliminatedData
                {
                    PlayerId = playerId,
                    RoundsSurvived = gameState.CurrentRound,
                    Place = gameState.PlayerPlaces.GetValueOrDefault(playerId, gameState.ActivePlayerIds.Count + 1),
                    CorrectAnswers = gameState.PlayerCorrectCount.GetValueOrDefault(playerId),
                    TotalScore = gameState.PlayerScores.GetValueOrDefault(playerId),
                    IsManuallyDisabled = false
                };

                await _notificationService.NotifyPlayerEliminated(playerId, eliminatedData);
                _logger.LogDebug("Player {PlayerId} eliminated in round {RoundNumber}, place: {Place}",
                    playerId, gameState.CurrentRound, eliminatedData.Place);
            }

            if (gameState.ActivePlayerIds.Count <= 1)
            {
                _logger.LogInformation("Match {MatchId} finished after round {RoundNumber}",
                    matchId, gameState.CurrentRound);
                await FinishMatchAsync(matchId);
            }
            else
            {
                lock(gameState)
                {
                    gameState.IsRoundFinished = false;
                }
                await StartNextRoundAsync(matchId);
            }
        }
        private async Task FinishMatchAsync(Guid matchId)
        {
            if (!_activeGames.TryRemove(matchId, out var gameState))
            {
                _logger.LogWarning("Game {MatchId} not found when finishing match", matchId);
                return;
            }

            _logger.LogInformation("Finishing match {MatchId} after {RoundCount} rounds",
                matchId, gameState.CurrentRound);

            if (_roundTimers.TryRemove(matchId, out var cts))
            {
                cts.Cancel();
                cts.Dispose();
            }

            await SavePendingAnswersAsync(gameState);

            var result = await _resultService.FinalizeMatchAsync(gameState);

            await _notificationService.NotifyMatchFinished(matchId, result);
            _logger.LogInformation("Match {MatchId} finished. Winner: {WinnerId}", matchId, result.WinnerId);
        }

        public Task<KothGameState?> GetGameStateAsync(Guid matchId)
        {
            _activeGames.TryGetValue(matchId, out var gameState);
            return Task.FromResult(gameState);
        }

        
        private async Task<KothGameState> CreateGameStateAsync(Guid matchId, List<PlayerInfo> realPlayers)
        {
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
                _logger.LogInformation("Added {BotCount} bots to match {MatchId}", botsNeeded, matchId);
            }

            var maxQuestions = 2 * (allPlayers.Count - 1);
            _logger.LogInformation("Creating game state for match {MatchId}: {PlayerCount} players, {MaxQuestions} max questions, seed: {Seed}",
                matchId, allPlayers.Count, maxQuestions, seed);

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
                        IsBot = p.IsBot,
                        EliminatedAtRound = 0
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
                PendingAnswers = new List<KothAnswer>(),
                IsRoundStarting = false,
                IsRoundFinishing = false,
                IsRoundFinished = false,
                IsMatchFinishing = false,
                AnsweredPlayers = new HashSet<Guid>()
            };

            return gameState;
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
        private async Task SavePendingAnswersAsync(KothGameState gameState)
        {
            List<KothAnswer> answers;
            lock (gameState)
            {
                answers = gameState.PendingAnswers.ToList();
                gameState.PendingAnswers.Clear();
            }

            if (answers.Any())
            {
                using (var scoped = _serviceScopeFactory.CreateScope())
                {
                    var dbContext = scoped.ServiceProvider.GetRequiredService<AppDbContext>();
                    await dbContext.KothAnswers.AddRangeAsync(answers);
                    await dbContext.SaveChangesAsync();
                    _logger.LogInformation("Saved {AnswerCount} answers for match {MatchId}", answers.Count, gameState.MatchId);
                }
            }
        }

        private List<PlayerRoundResult> GetPlayerResults(KothGameState gameState)
        {
            var results = new List<PlayerRoundResult>();
            var currentRound = gameState.CurrentRound;

            foreach (var playerId in gameState.Players.Keys)
            {
                var hasAnswered = gameState.PlayerAnswers.ContainsKey(playerId) &&
                                 gameState.PlayerAnswers[playerId].ContainsKey(currentRound);

                if (!hasAnswered)
                {
                    results.Add(new PlayerRoundResult
                    {
                        PlayerId = playerId,
                        HasAnswered = false,
                        IsCorrect = false,
                        TimeSpentMs = 0,
                        ScoreGained = 0
                    });
                }
                else
                {
                    var answer = gameState.PlayerAnswers[playerId][currentRound];
                    results.Add(new PlayerRoundResult
                    {
                        PlayerId = playerId,
                        HasAnswered = true,
                        IsCorrect = answer.IsCorrect,
                        TimeSpentMs = answer.TimeSpentMs,
                        ScoreGained = answer.ScoreGained
                    });
                }
            }

            return results;
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
    }
}
