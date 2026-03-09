using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using Microsoft.EntityFrameworkCore;

namespace GeoQuiz_backend.Application.Services.KingOfTheHill
{
    public class KothGameService : IKothGameService
    {
        private readonly AppDbContext _db;
        private readonly IQuestionRepository _questionRepo;
        private readonly ICountryRepository _countryRepo;
        private readonly ISignalRNotificationService _notificationService;
        private readonly ILogger<KothGameService> _logger;

        private static readonly Dictionary<Guid, KothGameState> _activeGames = new();
        private static readonly object _gameLock = new();
        private static readonly Dictionary<Guid, CancellationTokenSource> _roundTimers = new();

        public KothGameService(
            AppDbContext db,
            IQuestionRepository questionRepo,
            ICountryRepository countryRepo,
            ISignalRNotificationService notificationService,
            ILogger<KothGameService> logger)
        {
            _db = db;
            _questionRepo = questionRepo;
            _countryRepo = countryRepo;
            _notificationService = notificationService;
            _logger = logger;
        }

        public async Task<KothMatch> StartMatchFromLobbyAsync(List<PlayerInfo> players)
        {
            var matchId = Guid.NewGuid();
            var totalPlayers = players.Count;
            var maxQuestions = 2 * (totalPlayers - 1);
            var seed = new Random().Next();

            var availableModes = Enum.GetValues<GameMode>().ToList();
            var randomMode = availableModes[new Random().Next(availableModes.Count)];

            _logger.LogInformation("Starting KOTH match {MatchId} with {PlayerCount} players, max questions: {MaxQuestions}, seed: {Seed}",
                matchId, totalPlayers, maxQuestions, seed);

            var questionSet = new QuestionSet
            {
                Id = Guid.NewGuid(),
                KothMatchId = matchId,
                Mode = randomMode,
                Language = AppLanguage.Ru,
                Seed = seed,
                CreatedAt = DateTime.UtcNow,
                QuestionIds = new List<string>()
            };

            var questions = await GenerateQuestionsAsync(maxQuestions, seed, randomMode);
            questionSet.QuestionIds = questions.Select(q => q.QuestionId).ToList();

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

            foreach (var player in players)
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
                ActivePlayerIds = players.Select(p => p.PlayerId).ToList(),
                Players = players.ToDictionary(
                    p => p.PlayerId,
                    p => new PlayerGameInfo
                    {
                        UserName = p.PlayerName,
                        Level = p.PlayerLevel,
                        IsActive = true
                    }),
                EliminatedPlayers = new List<Guid>(),
                EliminatedThisRound = new List<Guid>(),
                CurrentRound = 0,
                CurrentRoundType = RoundType.Classic,
                Questions = questions,
                PlayerAnswers = players.ToDictionary(p => p.PlayerId, p => new Dictionary<int, PlayerAnswer>()),
                PlayerScores = players.ToDictionary(p => p.PlayerId, p => 0),
                PlayerCorrectCount = players.ToDictionary(p => p.PlayerId, p => 0)
            };

            lock (_gameLock)
            {
                _activeGames[matchId] = gameState;
            }

            var matchStartedData = new MatchStartedData
            {
                MatchId = matchId,
                TotalPlayers = totalPlayers,
                TotalRounds = maxQuestions,
                FirstRoundStartTime = DateTime.UtcNow.AddSeconds(3),
                AllPlayers = players.Select(p => new PlayerInfo
                {
                    PlayerId = p.PlayerId,
                    PlayerName = p.PlayerName,
                    PlayerLevel = p.PlayerLevel
                }).ToList()
            };

            await _notificationService.NotifyMatchStarted(matchId, matchStartedData);

            _ = Task.Run(async () =>
            {
                await Task.Delay(3000);
                await StartNextRoundAsync(matchId);
            });

            return match;
        }

        public async Task<RoundStartedData?> StartNextRoundAsync(Guid matchId)
        {
            RoundStartedData roundStartedData = null!;
            Guid? gameStateForTimer = null;

            lock (_gameLock)
            {
                if (!_activeGames.TryGetValue(matchId, out var gameState))
                {
                    _logger.LogWarning("Game {MatchId} not found", matchId);
                    return null;
                }

                if (gameState.ActivePlayerIds.Count <= 1)
                {
                    _ = FinishMatchAsync(matchId);
                    return null;
                }

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
                    _logger.LogError("Not enough questions for match {MatchId}", matchId);
                    return null;
                }

                var question = gameState.Questions[gameState.CurrentRound - 1];
                gameState.RoundStartTime = DateTime.UtcNow;
                gameState.EliminatedThisRound.Clear();

                var questionData = MapToQuestionData(question);

                roundStartedData = new RoundStartedData
                {
                    RoundNumber = gameState.CurrentRound,
                    RoundType = gameState.CurrentRoundType,
                    Question = questionData,
                    RoundStartTime = gameState.RoundStartTime,
                    TimeLimitSeconds = 10
                };

                gameStateForTimer = matchId;
            }

            if (roundStartedData != null && gameStateForTimer.HasValue)
            {
                await _notificationService.NotifyRoundStarted(matchId, roundStartedData);

                var cts = new CancellationTokenSource();
                lock (_gameLock)
                {
                    _roundTimers[matchId] = cts;
                }

                _ = Task.Run(async () =>
                {
                    try
                    {
                        await Task.Delay(TimeSpan.FromSeconds(10), cts.Token);
                        await FinishRoundAsync(matchId);
                    }
                    catch (TaskCanceledException)
                    {
                        _logger.LogDebug("Round timer cancelled for match {MatchId}", matchId);
                    }
                }, cts.Token);
            }

            return roundStartedData;
        }

        public async Task<AnswerResultData> SubmitAnswerAsync(Guid matchId, Guid userId, SubmitAnswerRequest request)
        {
            AnswerResultData result = null!;
            bool shouldFinishRoundEarly = false;
            Guid matchToFinish = Guid.Empty;
            PlayerAnswer answer = null!;
            KothGameState gameState = null!;

            lock (_gameLock)
            {
                if (!_activeGames.TryGetValue(matchId, out gameState))
                    throw new Exception("Game not found");

                if (!gameState.ActivePlayerIds.Contains(userId))
                    throw new Exception("Player is eliminated");

                if (gameState.PlayerAnswers.ContainsKey(userId) &&
                    gameState.PlayerAnswers[userId].ContainsKey(request.RoundNumber))
                    throw new Exception("Already answered this round");

                if (request.RoundNumber > gameState.Questions.Count)
                    throw new Exception("Invalid round number");

                var question = gameState.Questions[request.RoundNumber - 1];
                var isCorrect = request.SelectedOptionIndex == question.CorrectOptionIndex;
                var scoreGained = isCorrect ? CalculateScore(request.TimeSpentMs) : 0;

                answer = new PlayerAnswer
                {
                    QuestionId = request.QuestionId,
                    IsCorrect = isCorrect,
                    TimeSpentMs = request.TimeSpentMs,
                    ScoreGained = scoreGained,
                    AnsweredAt = DateTime.UtcNow
                };

                gameState.PlayerAnswers[userId][request.RoundNumber] = answer;

                if (isCorrect)
                {
                    gameState.PlayerScores[userId] += scoreGained;
                    gameState.PlayerCorrectCount[userId]++;
                }

                result = new AnswerResultData
                {
                    IsCorrect = isCorrect,
                    ScoreGained = scoreGained,
                    TimeSpentMs = request.TimeSpentMs,
                    RemainingPlayers = gameState.ActivePlayerIds.Count,
                    CorrectOptionIndex = question.CorrectOptionIndex
                };

                var totalActivePlayers = gameState.ActivePlayerIds.Count;
                var answeredCount = gameState.PlayerAnswers.Count(kvp =>
                    kvp.Value.ContainsKey(request.RoundNumber));

                if (answeredCount >= totalActivePlayers)
                {
                    shouldFinishRoundEarly = true;
                    matchToFinish = matchId;
                }
            }

            await SaveAnswerToDbAsync(matchId, userId, request.RoundNumber, answer);

            await _notificationService.NotifyAnswerResult(userId, result); 

            if (shouldFinishRoundEarly && matchToFinish != Guid.Empty)
            {
                if (_roundTimers.TryGetValue(matchId, out var cts))
                {
                    cts.Cancel();
                    lock (_gameLock)
                    {
                        _roundTimers.Remove(matchId);
                    }

                    _ = Task.Run(() => FinishRoundAsync(matchId));
                }
            }

            return result;
        }

        public async Task<RoundFinishedData> FinishRoundAsync(Guid matchId)
        {
            RoundFinishedData roundFinishedData = null!;
            bool isMatchFinished = false;
            Guid matchToProcess = Guid.Empty;
            List<Guid> eliminatedPlayers = null!;
            List<PlayerEliminatedData> eliminatedData = null!;

            lock (_gameLock)
            {
                if (!_activeGames.TryGetValue(matchId, out var gameState))
                    throw new Exception("Game not found");

                var currentRound = gameState.CurrentRound;
                var eliminatedThisRound = new List<Guid>();
                var playerResults = new Dictionary<Guid, PlayerRoundResult>();

                foreach (var playerId in gameState.ActivePlayerIds.ToList())
                {
                    var hasAnswered = gameState.PlayerAnswers.ContainsKey(playerId) &&
                                      gameState.PlayerAnswers[playerId].ContainsKey(currentRound);

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

                    if (correctAnswers.Count > 0)
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

                eliminatedThisRound = eliminatedThisRound.Distinct().ToList();

                foreach (var playerId in eliminatedThisRound)
                {
                    gameState.ActivePlayerIds.Remove(playerId);
                    gameState.EliminatedPlayers.Add(playerId);
                    gameState.EliminatedThisRound.Add(playerId);
                    gameState.Players[playerId].IsActive = false;
                    gameState.Players[playerId].EliminatedAtRound = currentRound;
                }

                roundFinishedData = new RoundFinishedData
                {
                    RoundNumber = currentRound,
                    RoundType = gameState.CurrentRoundType,
                    EliminatedPlayerIds = eliminatedThisRound,
                    Results = playerResults.Values.ToList(),
                    RemainingPlayers = gameState.ActivePlayerIds.Count,
                    IsMatchFinished = gameState.ActivePlayerIds.Count <= 1
                };

                isMatchFinished = roundFinishedData.IsMatchFinished;
                eliminatedPlayers = eliminatedThisRound;
                matchToProcess = matchId;

                eliminatedData = new List<PlayerEliminatedData>();
                foreach (var playerId in eliminatedPlayers)
                {
                    eliminatedData.Add(new PlayerEliminatedData
                    {
                        PlayerId = playerId,
                        RoundsSurvived = roundFinishedData.RoundNumber,
                        Place = roundFinishedData.RemainingPlayers + 1,
                        CorrectAnswers = gameState.PlayerCorrectCount[playerId],
                        TotalScore = gameState.PlayerScores[playerId]
                    });
                }

            }

            if (roundFinishedData != null)
            {
                await _notificationService.NotifyRoundFinished(matchId, roundFinishedData);

                if (eliminatedData != null)
                {
                    foreach (var data in eliminatedData)
                    {
                        await _notificationService.NotifyPlayerEliminated(data.PlayerId, data);
                    }
                }
            }

            if (_roundTimers.TryGetValue(matchId, out var cts))
            {
                cts.Cancel();
                lock (_gameLock)
                {
                    _roundTimers.Remove(matchId);
                }
            }

            if (isMatchFinished)
            {
                _ = Task.Run(() => FinishMatchAsync(matchId));
            }
            else if (matchToProcess != Guid.Empty)
            {
                _ = Task.Run(async () =>
                {
                    await Task.Delay(2000);
                    await StartNextRoundAsync(matchId);
                });
            }

            return roundFinishedData;
        }


        public async Task<MatchFinishedData> FinishMatchAsync(Guid matchId)
        {
            MatchFinishedData matchFinishedData = null!;
            List<(Guid UserId, MatchFinishedData Data)> notifications = new();
            KothMatch dbMatch = null!;
            Dictionary<Guid, PlayerFinalStanding> standings = new();
            GameMode mode = GameMode.Capital;

            lock (_gameLock)
            {
                if (!_activeGames.TryGetValue(matchId, out var gameState))
                    throw new Exception("Game not found");

                dbMatch = gameState.Match; 
                mode = dbMatch.SelectedMode;

                Guid? winnerId = gameState.ActivePlayerIds.Count == 1?
                    gameState.ActivePlayerIds.First() : null;

                var finalStandings = new List<PlayerFinalStanding>();

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
                    .Select((id, index) => new { id, place = gameState.EliminatedPlayers.Count - index + 2 }) //temp
                    .OrderBy(x => x.place);

                foreach (var item in eliminatedPlayers)
                {
                    var standing = new PlayerFinalStanding
                    {
                        PlayerId = item.id,
                        PlayerName = gameState.Players[item.id].UserName,
                        Place = item.place,
                        CorrectAnswers = gameState.PlayerCorrectCount.ContainsKey(item.id)
                            ? gameState.PlayerCorrectCount[item.id] : 0,
                        TotalScore = gameState.PlayerScores.ContainsKey(item.id)
                            ? gameState.PlayerScores[item.id] : 0,
                        RoundsSurvived = gameState.Players[item.id].EliminatedAtRound
                    };
                    finalStandings.Add(standing);
                    standings[item.id] = standing;
                }

                matchFinishedData = new MatchFinishedData
                {
                    MatchId = matchId,
                    WinnerId = winnerId ?? Guid.Empty,
                    FinalStandings = finalStandings
                };

                foreach (var player in finalStandings)
                {
                    notifications.Add((player.PlayerId, matchFinishedData));
                }

                _activeGames.Remove(matchId);
            }

            if (dbMatch != null)
            {
                dbMatch.Status = KothMatchStatus.Finished;
                dbMatch.FinishedAt = DateTime.UtcNow;
                dbMatch.WinnerId = matchFinishedData.WinnerId != Guid.Empty ? matchFinishedData.WinnerId : null;

                foreach (var player in dbMatch.Players)
                {
                    if (standings.TryGetValue(player.UserId, out var standing))
                    {
                        player.Place = standing.Place;
                        player.IsActive = standing.Place == 1; 
                        player.RoundEliminated = standing.RoundsSurvived;
                    }
                }

                await _db.SaveChangesAsync();
            }

            await CreateGameSessionsAsync(matchId, standings, mode);

            foreach (var (userId, data) in notifications)
            {
                await _notificationService.NotifyMatchFinished(userId, data);
            }

            return matchFinishedData;
        }
        public Task<KothGameState?> GetGameStateAsync(Guid matchId)
        {
            lock (_gameLock)
            {
                _activeGames.TryGetValue(matchId, out var gameState);
                return Task.FromResult(gameState);
            }
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
        private async Task SaveAnswerToDbAsync(Guid matchId, Guid userId, int roundNumber, PlayerAnswer answer)
        {
            var kothAnswer = new KothAnswer
            {
                Id = Guid.NewGuid(),
                MatchId = matchId,
                UserId = userId,
                QuestionId = answer.QuestionId,
                RoundNumber = roundNumber,
                IsCorrect = answer.IsCorrect,
                TimeSpentMs = answer.TimeSpentMs,
                ScoreGained = answer.ScoreGained,
                AnsweredAt = answer.AnsweredAt
            };

            _db.KothAnswers.Add(kothAnswer);
            await _db.SaveChangesAsync();
        }
        private async Task CreateGameSessionsAsync(Guid matchId, Dictionary<Guid, PlayerFinalStanding> standings, GameMode mode)
        {
            foreach (var (userId, standing) in standings)
            {
                var answers = await _db.KothAnswers
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

                _db.GameSessions.Add(gameSession);

                await UpdateUserStatsAsync(userId, gameSession, standing.Place == 1);
            }

            await _db.SaveChangesAsync();
        }

        private async Task UpdateUserStatsAsync(Guid userId, GameSession session, bool isWinner)
        {
            var user = await _db.Users
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
            }

            stats.Experience += session.Score;

            while (stats.Experience >= GetXpToNextLevel(stats.Level))
            {
                stats.Experience -= GetXpToNextLevel(stats.Level);
                stats.Level++;
            }

            await _db.SaveChangesAsync();
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
