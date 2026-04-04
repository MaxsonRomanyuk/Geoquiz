using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Services.Bots;
using GeoQuiz_backend.Domain.Entities;

namespace GeoQuiz_backend.Application.Services.KingOfTheHill
{
    public class KothRoundService : IKothRoundService
    {
        private readonly ILogger<KothRoundService> _logger;

        public KothRoundService(ILogger<KothRoundService> logger)
        {
            _logger = logger;
        }
        public RoundStartedData? StartRound(KothGameState gameState)
        {
            lock (gameState)
            {
                if (gameState.IsRoundStarting || gameState.IsRoundFinished)
                    return null;

                gameState.IsRoundStarting = true;

                gameState.CurrentRound++;

                if (gameState.CurrentRound == 1)
                {
                    gameState.CurrentRoundType = RoundType.Classic;
                }
                else
                {
                    gameState.CurrentRoundType = gameState.EliminatedThisRound.Count == 0
                        ? RoundType.Speed
                        : RoundType.Classic;
                }

                gameState.RoundStartTime = DateTime.UtcNow;
                gameState.EliminatedThisRound.Clear();
                gameState.AnsweredPlayers.Clear();
                gameState.IsRoundStarting = false;
                gameState.IsRoundFinished = false;

                var question = gameState.Questions[gameState.CurrentRound - 1];

                return new RoundStartedData
                {
                    RoundNumber = gameState.CurrentRound,
                    RoundType = gameState.CurrentRoundType == RoundType.Classic ? 1 : 2,
                    Question = MapToQuestionData(question),
                    ServerTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    RoundEndAt = DateTimeOffset.UtcNow.AddSeconds(10).ToUnixTimeMilliseconds()
                };
            }
        }
        public AnswerResultData ProcessAnswer(KothGameState gameState, Guid userId, SubmitAnswerRequest request)
        {
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
                CorrectOptionIndex = question.CorrectOptionIndex,
            };

            return result;
        }
        public void ProcessBotAnswers(KothGameState gameState, Guid matchId)
        {
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

                        _logger.LogDebug("Bot {BotId} answered {Result} in {Time}ms", botId, botAnswer.IsCorrect ? "correctly" : "incorrectly", botAnswer.ResponseTimeMs);
                    }
                }
            }
        }
        public List<Guid> FinishRound(KothGameState gameState)
        {
            lock (gameState)
            {
                if (gameState.IsRoundFinishing || gameState.IsRoundFinished)
                    return new List<Guid>();

                gameState.IsRoundFinishing = true;
            }

            var eliminated = new List<Guid>();

            lock (gameState)
            {
                var currentRound = gameState.CurrentRound;

                foreach (var playerId in gameState.ActivePlayerIds)
                {
                    var hasAnswered = gameState.PlayerAnswers.ContainsKey(playerId) &&
                                     gameState.PlayerAnswers[playerId].ContainsKey(currentRound);

                    if (!hasAnswered)
                    {
                        eliminated.Add(playerId);
                        continue;
                    }

                    var answer = gameState.PlayerAnswers[playerId][currentRound];
                    if (!answer.IsCorrect)
                    {
                        eliminated.Add(playerId);
                    }
                }

                if (gameState.CurrentRoundType == RoundType.Speed)
                {
                    var correctAnswers = gameState.ActivePlayerIds
                        .Where(id => !eliminated.Contains(id))
                        .Select(id => new
                        {
                            PlayerId = id,
                            TimeSpent = gameState.PlayerAnswers[id][currentRound].TimeSpentMs
                        })
                        .OrderByDescending(x => x.TimeSpent)
                        .ToList();

                    if (correctAnswers.Count > 1)
                    {
                        var slowest = correctAnswers.First();
                        if (!eliminated.Contains(slowest.PlayerId))
                        {
                            eliminated.Add(slowest.PlayerId);
                        }
                    }
                }

                //gameState.EliminatedThisRound.AddRange(eliminated);
                gameState.IsRoundFinished = true;
                gameState.IsRoundFinishing = false;
            }

            return eliminated;
        }
        public void AssignPlaces(KothGameState gameState, List<Guid> eliminated)
        {
            lock (gameState)
            {
                var eliminatedWithStats = eliminated
                    .Select(id => new
                    {
                        PlayerId = id,
                        Score = gameState.PlayerScores.GetValueOrDefault(id),
                        TimeSpent = gameState.PlayerAnswers.ContainsKey(id) &&
                                    gameState.PlayerAnswers[id].ContainsKey(gameState.CurrentRound)
                                    ? gameState.PlayerAnswers[id][gameState.CurrentRound].TimeSpentMs
                                    : int.MaxValue
                    })
                    .OrderBy(x => x.Score)
                    .ThenByDescending(x => x.TimeSpent)
                    .ToList();

                int currentPlace = gameState.ActivePlayerIds.Count + eliminatedWithStats.Count;
                foreach (var item in eliminatedWithStats)
                {
                    if (!gameState.PlayerPlaces.ContainsKey(item.PlayerId))
                    {
                        gameState.PlayerPlaces[item.PlayerId] = currentPlace;
                    }
                    currentPlace--;
                }
            }
        }
        public bool IsRoundComplete(KothGameState gameState)
        {
            lock (gameState)
            {
                var eliminatedAnswered = 0;
                foreach (var playerId in gameState.EliminatedThisRound)
                {
                    var hasAnswered = gameState.PlayerAnswers.ContainsKey(playerId) && gameState.PlayerAnswers[playerId].ContainsKey(gameState.CurrentRound);
                    if (hasAnswered) eliminatedAnswered++;
                }
                var allAnswered = gameState.AnsweredPlayers.Count >= gameState.ActivePlayerIds.Count + eliminatedAnswered;
                return allAnswered;
            }
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
        private int CalculateScore(int timeSpentMs)
        {
            var maxScore = 10;
            var penalty = timeSpentMs / 1000;
            return Math.Max(1, maxScore - penalty);
        }
    }

}
