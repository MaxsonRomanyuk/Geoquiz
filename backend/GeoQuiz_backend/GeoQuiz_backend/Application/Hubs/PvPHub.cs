using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Services;
using GeoQuiz_backend.Application.Services.PvP;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.DTOs.PvP;
using GeoQuiz_backend.Infrastructure.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using System.Collections.Concurrent;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;

namespace GeoQuiz_backend.Application.Hubs
{
    [Authorize]
    public class PvPHub : Hub<IPvPHubClient>
    {
        private readonly ISignalRNotificationService _notificationService;
        private readonly IMatchmakingService _matchmaking;
        private readonly ILogger<PvPHub> _logger;
        private readonly AppDbContext _db;

        private static readonly ConcurrentDictionary<Guid, string> _userConnections = new();
        public PvPHub(ISignalRNotificationService notificationService,IMatchmakingService matchmaking,ILogger<PvPHub> logger, AppDbContext db)
        {
            _notificationService = notificationService;
            _matchmaking = matchmaking;
            _logger = logger;
            _db = db;
        }
        public override async Task OnConnectedAsync()
        {
            var userId = GetUserId();
            var connectionId = Context.ConnectionId;

            _userConnections[userId] = connectionId;
            _logger.LogInformation("User {UserId} connected with connection {ConnectionId}",
                userId, connectionId);

            await base.OnConnectedAsync();
        }

        public override async Task OnDisconnectedAsync(Exception? exception)
        {
            var userId = GetUserId();

            _userConnections.TryRemove(userId, out _);

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
                AvailableModes = match.Draft.AvailableModes,
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
                FirstTurnStartTime = DateTime.UtcNow.AddSeconds(2)
            };

            await _notificationService.NotifyMatchFound(player1.Id, player1Data);
            await _notificationService.NotifyMatchFound(player2.Id, player2Data);

            if (_userConnections.TryGetValue(player1.Id, out var conn1))
                await Groups.AddToGroupAsync(conn1, $"match_{match.Id}");

            if (_userConnections.TryGetValue(player2.Id, out var conn2))
                await Groups.AddToGroupAsync(conn2, $"match_{match.Id}");
        }
        public async Task LeaveQueue()
        {
            var userId = GetUserId();
            await _matchmaking.LeaveQueueAsync(userId);
            _logger.LogInformation("User {UserId} left queue", userId);
        }
    }
}
