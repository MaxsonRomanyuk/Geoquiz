using GeoQuiz_backend.API.HubClients;
using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Services.PvP;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Mongo;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using System.Collections.Concurrent;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz_backend.API.Hubs
{
    [Authorize]
    public class KothHub : Hub<IKothHubClient>
    {
        private readonly IKothMatchmakingService _matchmaking;
        private readonly IKothGameService _gameService;
        private readonly ISignalRNotificationService _notificationService;
        private readonly ILogger<KothHub> _logger;

        //private static readonly ConcurrentDictionary<Guid, string> _userConnections = new();
        private static readonly ConcurrentDictionary<Guid, UserKothSession> _userSessions = new();
        private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentLobby = new();
        private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentMatch = new();
        private static readonly ConcurrentDictionary<Guid, ConcurrentDictionary<Guid, bool>> _activeMatches = new();
        private static readonly ConcurrentDictionary<Guid, bool> _roundStarted = new();

        public KothHub(
            IKothMatchmakingService matchmaking,
            IKothGameService gameService,
            ISignalRNotificationService notificationService,
            ILogger<KothHub> logger)
        {
            _matchmaking = matchmaking;
            _gameService = gameService;
            _notificationService = notificationService;
            _logger = logger;
        }
        public override async Task OnConnectedAsync()
        {
            var userId = GetUserId();
            var connectionId = Context.ConnectionId;

            if (_userSessions.TryGetValue(userId, out var existingSession))
            {
                _logger.LogWarning("User {UserId} reconnecting. Old connection: {OldConnection}, New connection: {NewConnection}",
                    userId, existingSession.ConnectionId, connectionId);

                await HandleMultiDeviceConflict(userId, existingSession, connectionId);

                _userSessions[userId] = new UserKothSession
                {
                    ConnectionId = connectionId,
                    ConnectedAt = DateTime.UtcNow,
                    IsInLobby = false,
                    IsInMatch = existingSession.IsInMatch,
                    CurrentMatchId = existingSession.CurrentMatchId
                };
            }
            else
            {
                _userSessions[userId] = new UserKothSession
                {
                    ConnectionId = connectionId,
                    ConnectedAt = DateTime.UtcNow,
                    IsInLobby = false,
                    IsInMatch = false,
                    CurrentMatchId = null
                };
            }

            _logger.LogInformation("User {UserId} connected to KothHub with connection {ConnectionId}", userId, connectionId);

            await base.OnConnectedAsync();
        }
        

        public override async Task OnDisconnectedAsync(Exception? exception)
        {
            var userId = GetUserId();
            var connectionId = Context.ConnectionId;

            if (_userSessions.TryGetValue(userId, out var session))
            {
                if (session.ConnectionId == connectionId)
                {
                    _logger.LogInformation("User {UserId} disconnecting from session {session}", userId, session.ToString());
                    _userSessions.TryRemove(userId, out _);

                    if (session.IsInLobby)
                    {
                        _logger.LogInformation("User {UserId} removed from lobby due to disconnect", userId);
                        if (_userCurrentLobby.TryRemove(userId, out var lobbyId))
                        {
                            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"lobby_{lobbyId}");
                        }

                        await _matchmaking.LeaveLobbyAsync(userId);
                    }
                    if (session.IsInMatch && session.CurrentMatchId.HasValue)
                    {
                        _logger.LogInformation("User {UserId} removed from match due to disconnect", userId);
                        if (_userCurrentMatch.TryRemove(userId, out var matchId))
                        {
                            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"match_{matchId}");
                        }

                        await _gameService.LeaveMatchAsync(userId, matchId);
                    }
                }
                else
                {
                    _logger.LogInformation("User {UserId} disconnected from connection {ConnectionId} and trying to delete new connection {session}",
                        userId, connectionId, session.ConnectionId);
                }
            }

            _logger.LogInformation("User {UserId} disconnected from KothHub", userId);
            await base.OnDisconnectedAsync(exception);
        }

        public async Task JoinLobby()
        {
            var userId = GetUserId();
            var connectionId = Context.ConnectionId;
            if (_userSessions.TryGetValue(userId, out var session))
            {
                if (session.IsInLobby)
                {
                    _logger.LogWarning("User {UserId} already in lobby", userId);
                    // ищем новое лобби
                    return;
                }
                else if (session.IsInMatch && session.CurrentMatchId.HasValue)
                {
                    var matchId = session.CurrentMatchId.Value;
                    if (_activeMatches.TryGetValue(matchId, out _))
                    {
                        await SendCurrentMatchState(userId, matchId);
                        await Groups.AddToGroupAsync(session.ConnectionId, $"match_{matchId}");
                    }
                    _logger.LogWarning("User {UserId} already in match", userId);
                    return;
                }
                session.IsInLobby = true;
            }
            _logger.LogInformation("User {UserId} joining KOTH lobby", userId);

            try
            {
                (KothLobby? lobby, string userName, int userLevel) = await _matchmaking.JoinLobbyAsync(userId);

                if (lobby == null) return;

                //if (_userSessions.TryGetValue(userId, out var userSession))
                //{
                //    userSession.IsInLobby = true;
                //}
                
                //await _notificationService.NotifyCurrentPlayerAboutLobby(userId, new LobbyInitialStateData
                //{
                //    LobbyId = lobby.Id,
                //    Players = lobby.PlayersLobby,
                //    TotalPlayers = lobby.PlayersLobby.Count
                //});
                await Groups.AddToGroupAsync(connectionId, $"lobby_{lobby.Id}");

                await _notificationService.NotifyPlayerJoinedToOthers(lobby.Id, new PlayerJoinedData
                {
                    LobbyId = lobby.Id,
                    PlayerId = userId,
                    PlayerName = userName,
                    PlayerLevel = userLevel,
                    TotalPlayers = lobby.PlayersLobby.Count

                }, connectionId);
                _userCurrentLobby[userId] = lobby.Id;

                _logger.LogInformation("User {UserId} added to lobby group {LobbyId}", userId, lobby.Id);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in JoinLobby for user {UserId}", userId);
                throw new HubException("Failed to join lobby");
            }
        }

        public async Task LeaveLobby()
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} leaving KOTH lobby", userId);

            try
            {
                if (_userCurrentLobby.TryGetValue(userId, out var lobbyId))
                {
                    await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"lobby_{lobbyId}");
                    _userCurrentLobby.TryRemove(userId, out _);
                }
                if (_userSessions.TryGetValue(userId, out var session))
                {
                    session.IsInLobby = false;
                }
                await _matchmaking.LeaveLobbyAsync(userId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in LeaveLobby for user {UserId}", userId);
                throw new HubException("Failed to leave lobby");
            }
        }
        public async Task PlayerReadyForGame(Guid matchId)
        {
            var userId = GetUserId();
            if (!_activeMatches.TryGetValue(matchId, out var matchState))
            {
                _logger.LogWarning("Match {MatchId} not found in active matches for user {UserId}", matchId, userId);
                return;
            }

            matchState[userId] = true;
            _logger.LogWarning("Start next round for user {UserId} at {date}", userId, DateTime.UtcNow.ToString());

            if (_roundStarted.ContainsKey(matchId))
                return;

            var gameReady = matchState.Values.All(ready => ready);
            
            if (gameReady)
            {
                if (_roundStarted.TryAdd(matchId, true))
                {
                    _logger.LogInformation("All players ready for match {MatchId}, starting round", matchId);
                    _ = Task.Run(async () =>
                    {
                        await _gameService.StartNextRoundAsync(matchId);
                    });
                }
            }
        }
        public async Task JoinMatch(Guid matchId)
        {
            var userId = GetUserId();
            if (_userCurrentMatch.TryGetValue(userId, out _))
            {
                _logger.LogWarning("User {UserId} is already in the match {MatchId}", userId, matchId);
                return;
            }
            if (!_userCurrentLobby.TryGetValue(userId, out var lobbyId))
            {
                _logger.LogWarning("User {UserId} tried to join match {MatchId} but not in any lobby", userId, matchId);
                return;
            }

            if (_userSessions.TryGetValue(userId, out var session))
            {
                session.IsInLobby = false;
                session.IsInMatch = true;
                session.CurrentMatchId = matchId;
            }

            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"lobby_{lobbyId}");
            _userCurrentLobby.TryRemove(userId, out _);

            await Groups.AddToGroupAsync(Context.ConnectionId, $"match_{matchId}");
            _userCurrentMatch[userId] = matchId;

            if (!_activeMatches.ContainsKey(matchId))
            {
                _activeMatches[matchId] = new ConcurrentDictionary<Guid, bool>();
            }
            _activeMatches[matchId][userId] = false;

            _logger.LogInformation("User {UserId} moved from lobby {LobbyId} to match {MatchId}", userId, lobbyId, matchId);
        }

        public async Task LeaveMatch(Guid matchId)
        {
            var userId = GetUserId();

            if (_userSessions.TryGetValue(userId, out var session))
            {
                session.IsInMatch = false;
            }

            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"match_{matchId}");
            _userCurrentMatch.TryRemove(userId, out _);

            await _gameService.LeaveMatchAsync(userId, matchId);
            _logger.LogInformation("User {UserId} leaving match {MatchId}", userId, matchId);
            //_userCurrentLobby.TryRemove(userId, out _);
        }

        public async Task SubmitAnswer(SubmitAnswerRequest request)
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} submitting answer for round {RoundNumber} in match {MatchId}", userId, request.RoundNumber, request.MatchId);
            try
            {
                var answerResult = await _gameService.SubmitAnswerAsync(request.MatchId, userId, request);
                await _notificationService.NotifyAnswerResult(userId, answerResult);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error submitting answer for user {UserId}", userId);
                throw new HubException("Failed to submit answer");
            }
        }

        private Guid GetUserId()
        {
            var userIdClaim = Context.User?.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? Context.User?.FindFirstValue(JwtRegisteredClaimNames.Sub);

            if (string.IsNullOrEmpty(userIdClaim) || !Guid.TryParse(userIdClaim, out var userId))
                throw new HubException("Invalid user identification");

            return userId;
        }
        private async Task HandleMultiDeviceConflict(Guid userId, UserKothSession existingSession, string newConnectionId)
        {
            if (existingSession.IsInLobby)
            {
                await _matchmaking.LeaveLobbyAsync(userId);
                _userCurrentLobby.TryRemove(userId, out var lobbyId);
                {
                    await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"lobby_{lobbyId}");
                }
                _logger.LogInformation("User {UserId} removed from lobby due to new device connection", userId);

            }
            else if (existingSession.IsInMatch && existingSession.CurrentMatchId.HasValue)
            {
                var matchId = existingSession.CurrentMatchId.Value;
                _userCurrentMatch.TryRemove(userId, out _);
                {
                    await Groups.RemoveFromGroupAsync(existingSession.ConnectionId, $"match_{matchId}");
                }
                _logger.LogWarning("User {UserId} was in match {MatchId}", userId, matchId);
            }
            await _notificationService.NotifyForceKothDisconnect(existingSession.ConnectionId, new LocalizedText
            {
                Ru = "Вы были отключены, так как аккаунт использован на другом устройстве",
                En = "You have been disconnected because your account is in use on another device."
            });
        }
        private async Task SendCurrentMatchState(Guid userId, Guid matchId)
        {
            var gameState = await _gameService.GetGameStateAsync(matchId);
            if (gameState == null)
            {
                return;
            }
            var isEliminated = gameState.EliminatedPlayers.Contains(userId);

            var currentScore = gameState.PlayerScores[userId];
            var totalPlayers = gameState.Players.Count;
            var playersLeft = gameState.ActivePlayerIds.Count;

            var question = gameState.Questions[gameState.CurrentRound - 1];
            var timerEndsAt = _gameService.GetRoundTimerEndsAt(matchId) ?? DateTime.UtcNow.AddSeconds(10);
            var roundData = new RoundStartedData
            {
                RoundNumber = gameState.CurrentRound,
                RoundType = gameState.CurrentRoundType == RoundType.Classic ? 1 : 2,
                Question = MapToQuestionData(question),
                ServerTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                RoundEndAt = new DateTimeOffset(timerEndsAt).ToUnixTimeMilliseconds()
            };

            var resumeData = new MatchResumeData
            {
                MatchId = matchId,
                RoundStartedData = roundData,
                CurrentScore = currentScore,
                TotalPlayers = totalPlayers,
                PlayersLeft = playersLeft
            };

            await _notificationService.NotifyMatchResume(userId, resumeData);
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
    }
}
public class UserKothSession
{
    public string ConnectionId { get; set; }
    public DateTime ConnectedAt { get; set; }
    public bool IsInLobby { get; set; }
    public bool IsInMatch { get; set; }
    public Guid? CurrentMatchId { get; set; }
}