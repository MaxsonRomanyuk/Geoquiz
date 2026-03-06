using GeoQuiz.Backend.Application.DTOs.KingOfTheHill;
using GeoQuiz.Backend.Application.Interfaces;
using GeoQuiz.Backend.Domain.Entities;
using GeoQuiz.Backend.DTOs.KingOfTheHill;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using System.Collections.Concurrent;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz.Backend.API.Hubs;

[Authorize]
public class KothHub : Hub<IKothHubClient>
{
    private readonly IKothMatchmakingService _matchmaking;
    private readonly ISignalRNotificationService _notificationService;
    private readonly ILogger<KothHub> _logger;

    private static readonly ConcurrentDictionary<Guid, string> _userConnections = new();
    private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentLobby = new();
    private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentMatch = new();

    public KothHub(
        IKothMatchmakingService matchmaking,
        ISignalRNotificationService notificationService,
        ILogger<KothHub> logger)
    {
        _matchmaking = matchmaking;
        _notificationService = notificationService;
        _logger = logger;
    }

    public override async Task OnConnectedAsync()
    {
        var userId = GetUserId();
        var connectionId = Context.ConnectionId;

        _userConnections[userId] = connectionId;

        _logger.LogInformation(
            "User {UserId} connected to KothHub with connection {ConnectionId}",
            userId,
            connectionId
        );

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

            _logger.LogInformation(
                "User {UserId} disconnected from KothHub",
                userId
            );
        }

        await base.OnDisconnectedAsync(exception);
    }

    public async Task JoinLobby()
    {
        var userId = GetUserId();

        _logger.LogInformation(
            "User {UserId} joining KOTH lobby",
            userId
        );

        try
        {
            (KothLobby? lobby, string userName, int userLevel) =
                await _matchmaking.JoinLobbyAsync(userId);

            if (lobby != null)
            {
                var connectionId = Context.ConnectionId;

                await _notificationService.NotifyCurrentPlayerAboutLobby(
                    userId,
                    new LobbyInitialStateData
                    {
                        LobbyId = lobby.Id,
                        Players = lobby.PlayersLobby,
                        TotalPlayers = lobby.PlayersLobby.Count
                    }
                );

                await Groups.AddToGroupAsync(connectionId, $"lobby_{lobby.Id}");

                await _notificationService.NotifyPlayerJoinedToOthers(
                    lobby.Id,
                    new PlayerJoinedData
                    {
                        LobbyId = lobby.Id,
                        PlayerId = userId,
                        PlayerName = userName,
                        PlayerLevel = userLevel,
                        TotalPlayers = lobby.PlayersLobby.Count
                    },
                    connectionId
                );

                _userCurrentLobby[userId] = lobby.Id;

                _logger.LogInformation(
                    "User {UserId} added to lobby group {LobbyId}",
                    userId,
                    lobby.Id
                );
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

        _logger.LogInformation(
            "User {UserId} leaving KOTH lobby",
            userId
        );

        try
        {
            if (_userCurrentLobby.TryGetValue(userId, out var lobbyId))
            {
                await Groups.RemoveFromGroupAsync(
                    Context.ConnectionId,
                    $"lobby_{lobbyId}"
                );

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

    public async Task LeaveMatch(Guid matchId)
    {
        var userId = GetUserId();

        _logger.LogInformation(
            "User {UserId} leaving match {MatchId}",
            userId,
            matchId
        );

        await Groups.RemoveFromGroupAsync(
            Context.ConnectionId,
            $"match_{matchId}"
        );

        _userCurrentMatch.TryRemove(userId, out _);
    }

    private Guid GetUserId()
    {
        var userIdClaim =
            Context.User?.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? Context.User?.FindFirstValue(JwtRegisteredClaimNames.Sub);

        if (string.IsNullOrEmpty(userIdClaim) ||
            !Guid.TryParse(userIdClaim, out var userId))
        {
            throw new HubException("Invalid user identification");
        }

        return userId;
    }
}