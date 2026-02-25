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
using Microsoft.Extensions.Logging;
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
        private readonly IDraftService _draftService;
        private readonly ILogger<PvPHub> _logger;
        private readonly AppDbContext _db;

        private static readonly ConcurrentDictionary<Guid, string> _userConnections = new();
        private static readonly ConcurrentDictionary<Guid, CancellationTokenSource> _draftTimers = new();
        private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentMatch = new();
        public PvPHub(
                    ISignalRNotificationService notificationService,
                    IMatchmakingService matchmaking,
                    IDraftService draftService,
                    ILogger<PvPHub> logger,
                    AppDbContext db)
        {
            _notificationService = notificationService;
            _matchmaking = matchmaking;
            _draftService = draftService;
            _logger = logger;
            _db = db;
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
            }
            else
            {
                _logger.LogWarning("Player1 {UserId} not connected when adding to group", player1.Id);
            }

            if (_userConnections.TryGetValue(player2.Id, out var conn2))
            {
                await Groups.AddToGroupAsync(conn2, $"match_{match.Id}");
                _logger.LogInformation("Added player2 {UserId} to group match_{MatchId} with connection {ConnectionId}",
                    player2.Id, match.Id, conn2);
            }
            else
            {
                _logger.LogWarning("Player2 {UserId} not connected when adding to group", player2.Id);
            }

            StartDraftTimer(match.Id, match.Draft.CurrentTurnUserId, AppLanguage.Ru);
        }
        public async Task LeaveQueue()
        {
            var userId = GetUserId();
            await _matchmaking.LeaveQueueAsync(userId);
            _logger.LogInformation("User {UserId} left queue", userId);
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

                if (draft.PvPMatch.Status == PvPMatchStatus.Drafting)
                {
                    var updateData = new DraftUpdateData
                    {
                        MatchId = matchId,
                        BannedMode = mode,
                        BannedByUserId = userId,
                        RemainingModes = draft.AvailableModes,
                        NextTurnUserId = draft.CurrentTurnUserId,
                        Step = draft.Step
                    };

                    await _notificationService.NotifyDraftUpdated(matchId, updateData);

                    _logger.LogInformation("Draft updated for match {MatchId}. Remaining modes: {Modes}", matchId, string.Join(", ", draft.AvailableModes));

                    StartDraftTimer(matchId, draft.CurrentTurnUserId, language);
                }
                else if (draft.PvPMatch.Status == PvPMatchStatus.Ready)
                {
                    _logger.LogInformation("Draft completed for match {MatchId}, selected mode: {Mode}",
                        matchId, draft.PvPMatch.SelectedMode);
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
                        _logger.LogWarning("Timer expired for user {UserId} in match {MatchId}. Auto-banning {Mode}",
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
    }
}
