using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.API.HubClients;
using GeoQuiz_backend.Application.DTOs.KingOfTheHill;
using GeoQuiz_backend.Domain.Entities;
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

        private static readonly ConcurrentDictionary<Guid, string> _userConnections = new();
        private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentLobby = new();
        private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentMatch = new();

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

            _userConnections[userId] = connectionId;
            _logger.LogInformation("User {UserId} connected to KothHub with connection {ConnectionId}", userId, connectionId);

            await base.OnConnectedAsync();
        }
        

        public override async Task OnDisconnectedAsync(Exception? exception)
        {
            var userId = GetUserId();

            if (_userConnections.TryRemove(userId, out _))
            {
                if (_userCurrentLobby.TryGetValue(userId, out var lobbyId))
                {
                    await _matchmaking.LeaveLobbyAsync(userId);
                    _userCurrentLobby.TryRemove(userId, out _);
                }
                if (_userCurrentMatch.TryGetValue(userId, out var matchId))
                {
                    _userCurrentMatch.TryRemove(userId, out _);
                    await _gameService.LeaveMatchAsync(userId, matchId);
                }

                _logger.LogInformation("User {UserId} disconnected from KothHub", userId);
            }

            await base.OnDisconnectedAsync(exception);
        }

        public async Task JoinLobby()
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} joining KOTH lobby", userId);

            try
            {
                (KothLobby? lobby, string userName, int userLevel) = await _matchmaking.JoinLobbyAsync(userId);

                if (lobby != null)
                {
                    var connectionId = Context.ConnectionId;

                    await _notificationService.NotifyCurrentPlayerAboutLobby(userId, new LobbyInitialStateData
                    {
                        LobbyId = lobby.Id,
                        Players = lobby.PlayersLobby,
                        TotalPlayers = lobby.PlayersLobby.Count
                    });

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

                await _matchmaking.LeaveLobbyAsync(userId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in LeaveLobby for user {UserId}", userId);
                throw new HubException("Failed to leave lobby");
            }
        }
        public async Task JoinMatch(Guid matchId)
        {
            var userId = GetUserId();
            if (!_userCurrentLobby.TryGetValue(userId, out var lobbyId))
            {
                _logger.LogWarning("User {UserId} tried to join match {MatchId} but not in any lobby", userId, matchId);
                return;
            }

            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"lobby_{lobbyId}");
            _userCurrentLobby.TryRemove(userId, out _);

            await Groups.AddToGroupAsync(Context.ConnectionId, $"match_{matchId}");
            _userCurrentMatch[userId] = matchId;

            _logger.LogInformation("User {UserId} moved from lobby {LobbyId} to match {MatchId}", userId, lobbyId, matchId);
        }
        public async Task LeaveMatch(Guid matchId)
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} leaving match {MatchId}", userId, matchId);

            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"match_{matchId}");

            _userCurrentMatch.TryRemove(userId, out _);
            await _gameService.LeaveMatchAsync(userId, matchId);

            _userCurrentLobby.TryRemove(userId, out _);
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
    }
}
