using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.API.HubClients;
using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using System.Collections.Concurrent;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using GeoQuiz_backend.Application.Services.PvP;

namespace GeoQuiz_backend.API.Hubs
{
    [Authorize]
    public class PvPHub : Hub<IPvPHubClient>
    {
        private readonly ISignalRNotificationService _notificationService;
        private readonly IMatchmakingService _matchmaking;
        private readonly IDraftService _draftService;
        private readonly IPvPGameSessionService _gameSessionService;   
        private readonly ILogger<PvPHub> _logger;
        private readonly AppDbContext _db;

        private readonly IServiceScopeFactory _serviceScopeFactory;

        private static readonly ConcurrentDictionary<Guid, string> _userConnections = new();
        private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentMatch = new();
        private class GameTimer
        {
            public DateTime StartTime { get; set; }
            public CancellationTokenSource Cts { get; set; } = new();
        }
        public PvPHub(
                    ISignalRNotificationService notificationService,
                    IMatchmakingService matchmaking,
                    IDraftService draftService,
                    IPvPGameSessionService gameService,
                    ILogger<PvPHub> logger,
                    AppDbContext db,
                    IServiceScopeFactory serviceScopeFactory)
        {
            _notificationService = notificationService;
            _matchmaking = matchmaking;
            _draftService = draftService;
            _gameSessionService = gameService;
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

                    _logger.LogInformation("User {UserId} moved to group match_{MatchId} with new connection", userId, matchId);
                }
            }

            _userConnections[userId] = connectionId;
            _logger.LogInformation("User {UserId} connected with connection {ConnectionId}", userId, connectionId);

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
                    DraftService.CancelDraftTimer(matchId);
                    PvPGameSessionService.CancelMatchTimer(matchId);
                    _logger.LogInformation("User {UserId} removed from group match_{MatchId}", userId, matchId);

                    using (var scope = _serviceScopeFactory.CreateScope())
                    {
                        var scopedResultService = scope.ServiceProvider.GetRequiredService<IPvPResultService>();
                        var scopedDb = scope.ServiceProvider.GetRequiredService<AppDbContext>();

                        await scopedResultService.FinalizeMatchAsync(matchId, GameFinishReason.OpponentDisconnected, userId);
                    }
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

                _userCurrentMatch[match.Player1Id] = match.Id;
                _userCurrentMatch[match.Player2Id] = match.Id;

                if (_userConnections.TryGetValue(match.Player1Id, out var conn1))
                {
                    await Groups.AddToGroupAsync(conn1, $"match_{match.Id}");
                    _logger.LogInformation("Added player1 {UserId} to group match_{MatchId} with connection {ConnectionId}", match.Player1Id, match.Id, conn1);
                }
                else
                {
                    _logger.LogWarning("Player1 {UserId} not connected when adding to group", match.Player1Id);
                }

                if (_userConnections.TryGetValue(match.Player2Id, out var conn2))
                {
                    await Groups.AddToGroupAsync(conn2, $"match_{match.Id}");
                    _logger.LogInformation("Added player2 {UserId} to group match_{MatchId} with connection {ConnectionId}", match.Player2Id, match.Id, conn2);
                }
                else
                {
                    _logger.LogWarning("Player2 {UserId} not connected when adding to group", match.Player2Id);
                }

                _draftService.StartDraftTimer(match.Id, 0);
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
        }
        
        public async Task LeaveMatch(Guid matchId)
        {
            var userId = GetUserId();
            _userCurrentMatch.TryRemove(userId, out _);
            _logger.LogInformation("User {UserId} leaving match {MatchId}", userId, matchId);


            var connectionId = Context.ConnectionId;
            await Groups.RemoveFromGroupAsync(connectionId, $"match_{matchId}");


        }
        public async Task BanMode(Guid matchId, GameMode mode, AppLanguage language, int expectedStep)
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} banning mode {Mode} in match {MatchId}",
                userId, mode, matchId);

            try
            {
                var draft = await _draftService.BanModeAsync(matchId, userId, mode, expectedStep);

                _logger.LogInformation("Draft updated for match {MatchId}. Remaining modes: {Modes}", matchId, string.Join(", ", draft.AvailableModes));
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in BanMode");
                throw new HubException(ex.Message);
            }
        }


        public async Task SubmitAnswer(SubmitAnswerRequest request) 
        {
            var userId = GetUserId();
            _logger.LogInformation("User {UserId} submitting answer for question {QuestionNumber} in match {MatchId}",
                userId, request.QuestionNumber, request.MatchId);

            try
            {
                var answerResult = await _gameSessionService.SubmitAnswerAsync(request.MatchId, userId, request);

                await _notificationService.NotifyQuestionResult(request.MatchId, answerResult);
                //if (yourAnswers.Count >= 10 && opponentAnswers.Count >= 10)
                //{
                //    await FinalizeGameAsync(request.MatchId, GameFinishReason.AllQuestionsAnswered);
                //}
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in SubmitAnswer");
                throw new HubException(ex.Message);
            }
        }
    }
}
