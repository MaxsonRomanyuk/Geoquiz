using GeoQuiz_backend.API.HubClients;
using GeoQuiz_backend.Application.DTOs.PvP;
using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Application.Payloads.Koth;
using GeoQuiz_backend.Application.Services.PvP;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.Domain.Enums;
using GeoQuiz_backend.Domain.Mongo;
using GeoQuiz_backend.Infrastructure.Persistence.MySQL;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using MongoDB.Driver.Core.Connections;
using System.Collections.Concurrent;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using static Microsoft.EntityFrameworkCore.DbLoggerCategory.Database;

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

        private static readonly ConcurrentDictionary<Guid, UserPvPSession> _userSessions = new();
        private static readonly ConcurrentDictionary<Guid, Guid> _userCurrentMatch = new();
        private static readonly ConcurrentDictionary<Guid, PvPMatchState> _activeMatches = new();

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

            if (_userSessions.TryGetValue(userId, out var existingSession))
            {
                _logger.LogWarning("User {UserId} reconnecting. Old connection: {OldConnection}, New connection: {NewConnection}",
                    userId, existingSession.ConnectionId, connectionId);

                await HandleMultiDeviceConflict(userId, existingSession, connectionId);

                _userSessions[userId] = new UserPvPSession
                {
                    ConnectionId = connectionId,
                    ConnectedAt = DateTime.UtcNow,
                    IsInQueue = false,
                    IsInDraft = existingSession.IsInDraft,
                    IsInMatch = existingSession.IsInMatch,
                    CurrentMatchId = existingSession.CurrentMatchId
                };
            }
            else
            {
                _userSessions[userId] = new UserPvPSession
                {
                    ConnectionId = connectionId,
                    ConnectedAt = DateTime.UtcNow,
                    IsInQueue = false,
                    IsInDraft = false,
                    IsInMatch = false,
                    CurrentMatchId = null
                };
            }
            _logger.LogInformation("User {UserId} connected with connection {ConnectionId}", userId, connectionId);

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

                    if (session.IsInQueue)
                    {
                        await _matchmaking.LeaveQueueAsync(userId);
                        _logger.LogInformation("User {UserId} removed from queue due to disconnect", userId);
                    }

                    if (session.CurrentMatchId.HasValue && (session.IsInDraft || session.IsInMatch))
                    {
                        var matchId = session.CurrentMatchId.Value;
                        await ForceMatchEnd(userId, matchId, session.ConnectionId);
                        _activeMatches.TryRemove(matchId, out var _);
                    }
                    _userCurrentMatch.TryRemove(userId, out _);
                }
                else
                {
                    _logger.LogInformation("User {UserId} disconnected from connection {ConnectionId} and trying to delete new connection {session}",
                        userId, connectionId, session.ConnectionId);
                }
            }
            _logger.LogInformation("User {UserId} disconnected", userId);
            await base.OnDisconnectedAsync(exception);
        }
        public async Task JoinQueue()
        {
            var userId = GetUserId();
            if (_userSessions.TryGetValue(userId, out var session))
            {
                if (session.IsInQueue)
                {
                    _logger.LogWarning("User {UserId} already in queue", userId);
                    return;
                }
                else if (session.IsInDraft && session.CurrentMatchId.HasValue)
                {
                    var matchId = session.CurrentMatchId.Value;
                    if (_activeMatches.TryGetValue(matchId, out var matchState))
                    {
                        await SendCurrentDraftState(userId, matchId);
                        await Groups.AddToGroupAsync(session.ConnectionId, $"match_{matchId}");
                    }
                    _logger.LogWarning("User {UserId} already in draft", userId);
                    return;
                }
                else if (session.IsInMatch && session.CurrentMatchId.HasValue)
                {
                    var matchId = session.CurrentMatchId.Value;
                    if (_activeMatches.TryGetValue(matchId, out var matchState))
                    {
                        await SendCurrentMatchState(userId, matchId);
                        await Groups.AddToGroupAsync(session.ConnectionId, $"match_{matchId}");
                    }
                    _logger.LogWarning("User {UserId} already in match", userId);
                    return;
                }

                session.IsInQueue = true;
            }
            _logger.LogInformation("User {UserId} joining queue", userId);

            try
            {
                var match = await _matchmaking.JoinQueueAsync(userId);

                if (match == null)
                {
                    return;
                }

                UpdateUserSessionForDraft(match.Player1Id, match.Id, true);
                UpdateUserSessionForDraft(match.Player2Id, match.Id, true);

                await NotifyMatchFoundToBoth(match);

                _userCurrentMatch[match.Player1Id] = match.Id;
                _userCurrentMatch[match.Player2Id] = match.Id;
                _activeMatches[match.Id] = new PvPMatchState
                {
                    Match = match,
                    Player1ReadyForDraft = false,
                    Player2ReadyForDraft = false,
                    Player1ReadyForGame = false,
                    Player2ReadyForGame = false
                };

                if (_userSessions.TryGetValue(match.Player1Id, out var session1))
                {
                    await Groups.AddToGroupAsync(session1.ConnectionId, $"match_{match.Id}");
                    _logger.LogInformation("Added player1 {UserId} to group match_{MatchId} with connection {ConnectionId}", match.Player1Id, match.Id, session1.ConnectionId);
                }
                else
                {
                    _logger.LogWarning("Player1 {UserId} not connected when adding to group", match.Player1Id);
                }

                if (_userSessions.TryGetValue(match.Player2Id, out var session2))
                {
                    await Groups.AddToGroupAsync(session2.ConnectionId, $"match_{match.Id}");
                    _logger.LogInformation("Added player2 {UserId} to group match_{MatchId} with connection {ConnectionId}", match.Player2Id, match.Id, session2.ConnectionId);
                }
                else
                {
                    _logger.LogWarning("Player2 {UserId} not connected when adding to group", match.Player2Id);
                }

                //_draftService.StartDraftTimer(match.Id, 0, true);
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
            if (_userSessions.TryGetValue(userId, out var session))
            {
                session.IsInQueue = false;
            }
            await _matchmaking.LeaveQueueAsync(userId);
            _logger.LogInformation("User {UserId} left queue", userId);
        }
        public async Task PlayerReadyForDraft(Guid matchId)
        {
            var userId = GetUserId();
            if (!_activeMatches.TryGetValue(matchId, out var matchState))
            {
                _logger.LogWarning("Match {MatchId} not found in active matches for user {UserId}", matchId, userId);
                return;
            }

            if (matchState.Match.Player1Id == userId)
                matchState.Player1ReadyForDraft = true;
            else if (matchState.Match.Player2Id == userId)
                matchState.Player2ReadyForDraft = true;

            if (matchState.Player1ReadyForDraft && matchState.Player2ReadyForDraft)
            {
                _draftService.StartDraftTimer(matchId, 0);
            }
        }
        public async Task LeaveMatch(Guid matchId)
        {
            var userId = GetUserId();
            var connectionId = Context.ConnectionId;
            if (_userSessions.TryGetValue(userId, out var session))
            {
                session.IsInMatch = false;
            }
            await ForceMatchEnd(userId, matchId, connectionId);
            _logger.LogInformation("User {UserId} leaving match {MatchId}", userId, matchId);
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

        public async Task PlayerReadyForGame(Guid matchId)
        {
            var userId = GetUserId();
            if (!_activeMatches.TryGetValue(matchId, out var matchState))
            {
                _logger.LogWarning("Match {MatchId} not found in active matches for user {UserId}", matchId, userId);
                return;
            }

            if (_userSessions.TryGetValue(userId, out var session))
            {
                session.IsInDraft = false;
                session.IsInMatch = true;
            }
            if (matchState.Match.Player1Id == userId)
            {
                matchState.Player1ReadyForGame = true;
            }
            else if (matchState.Match.Player2Id == userId)
            {
                matchState.Player2ReadyForGame = true;
            }

            if (matchState.Player1ReadyForGame && matchState.Player2ReadyForGame)
            {
                _ = Task.Run(() => _gameSessionService.MonitorGameTimeAsync(matchId));
            }
        }

        public async Task SubmitAnswer(SubmitAnswerRequest request) 
        {
            var userId = GetUserId();
            _logger.LogError("User {UserId} submitting answer for question {QuestionNumber} in match {MatchId}",
                userId, request.QuestionNumber, request.MatchId);

            try
            {
                var answerResult = await _gameSessionService.SubmitAnswerAsync(request.MatchId, userId, request);

                await _notificationService.NotifyQuestionResult(request.MatchId, answerResult);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error in SubmitAnswer");
                throw new HubException(ex.Message);
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

        private void UpdateUserSessionForDraft(Guid userId, Guid matchId, bool isInDraft)
        {
            if (_userSessions.TryGetValue(userId, out var session))
            {
                session.IsInQueue = false;
                session.IsInDraft = isInDraft;
                session.IsInMatch = false;
                session.CurrentMatchId = matchId;
            }
        }
        private async Task HandleMultiDeviceConflict(Guid userId, UserPvPSession existingSession, string newConnectionId)
        {
            if (existingSession.IsInQueue)
            {
                await _matchmaking.LeaveQueueAsync(userId);
                _logger.LogInformation("User {UserId} removed from queue due to new device connection", userId);
            }
            else if (existingSession.IsInDraft && existingSession.CurrentMatchId.HasValue)
            {
                var matchId = existingSession.CurrentMatchId.Value;

                _logger.LogInformation("User {UserId} was in draft {MatchId}", userId, matchId);
                await Groups.RemoveFromGroupAsync(existingSession.ConnectionId, $"match_{matchId}");
                
            }
            else if (existingSession.IsInMatch && existingSession.CurrentMatchId.HasValue)
            {
                var matchId = existingSession.CurrentMatchId.Value;

                _logger.LogWarning("User {UserId} was in match {MatchId}", userId, matchId);
                await Groups.RemoveFromGroupAsync(existingSession.ConnectionId, $"match_{matchId}");
            }

            await _notificationService.NotifyForcePvPDisconnect(existingSession.ConnectionId, new LocalizedText
            {
                Ru = "Вы были отключены, так как аккаунт использован на другом устройстве",
                En = "You have been disconnected because your account is in use on another device."
            });
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
                OpponentScore = player2.Stats.Score,
                OpponentIsPremium = player2.IsPremium,
                AvailableModes = match.Draft!.AvailableModes,
                BannedModes = match.Draft.BannedModes,
                CurrentTurnUserId = match.Draft.CurrentTurnUserId,
                FirstTurnStartTime = DateTime.UtcNow.AddSeconds(1)
            };

            var player2Data = new MatchFoundWithDraftData
            {
                MatchId = match.Id,
                YourId = player2.Id,
                OpponentId = player1.Id,
                OpponentName = player1.UserName,
                OpponentLevel = player1.Stats.Level,
                OpponentScore = player1.Stats.Score,
                OpponentIsPremium = player1.IsPremium,
                AvailableModes = match.Draft.AvailableModes,
                BannedModes = match.Draft.BannedModes,
                CurrentTurnUserId = match.Draft.CurrentTurnUserId,
                FirstTurnStartTime = DateTime.UtcNow.AddSeconds(1)
            };

            await _notificationService.NotifyMatchFound(player1.Id, player1Data);
            await _notificationService.NotifyMatchFound(player2.Id, player2Data);
        }
        private async Task SendCurrentDraftState(Guid userId, Guid matchId)
        {
            var draft = await _draftService.GetDraftAsync(matchId);
            var currentTurnUserId = draft.CurrentTurnUserId;

            var players = await _db.Users
                .Include(u => u.Stats)
                .Where(u => u.Id == draft.PvPMatch.Player1Id || u.Id == draft.PvPMatch.Player2Id)
                .ToListAsync();

            var player = players.Single(p => p.Id == userId);
            var opponent = players.Single(p => p.Id != userId);
            var timerEndsAt = _draftService.GetDraftTimerEndsAt(matchId) ?? DateTime.UtcNow.AddSeconds(10);

            var resumeData = new MatchFoundWithDraftData
            {
                MatchId = matchId,
                YourId = player.Id,
                OpponentId = opponent.Id,
                OpponentName = opponent.UserName,
                OpponentLevel = opponent.Stats.Level,
                OpponentScore = opponent.Stats.Score,
                OpponentIsPremium = opponent.IsPremium,
                AvailableModes = draft.AvailableModes,
                BannedModes = draft.BannedModes,
                CurrentTurnUserId = draft.CurrentTurnUserId,
                TimerEndAt = new DateTimeOffset(timerEndsAt).ToUnixTimeMilliseconds(),
                ServerTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                FirstTurnStartTime = DateTime.UtcNow.AddSeconds(1)
            };

            await _notificationService.NotifyDraftResume(userId, resumeData);
        }
        private async Task SendCurrentMatchState(Guid userId, Guid matchId)
        {
            var matchData = await _gameSessionService.GetGameStateAsync(matchId, userId);

            var gameData = new GameReadyData
            {
                MatchId = matchId,
                SelectedMode = matchData.Mode,
                TotalQuestions = matchData.Questions.Count,
                TotalGameTimeSeconds = 60,
                Questions = matchData.Questions
            };

            var timerEndsAt = _gameSessionService.GetGameTimerEndsAt(matchId) ?? DateTime.UtcNow.AddSeconds(60);

            var resumeData = new GameResumeData
            {
                OpponentName = matchData.Opponent.UserName,
                OpponentTotalScore = matchData.Opponent.Stats.Score,
                YourTotalScore = matchData.Player.Stats.Score,
                OpponentCurrentScore = matchData.OpponentCurrentScore,
                YourCurrentScore = matchData.YourCurrentScore,
                CurrentQuestion = matchData.YourAnswered,
                TimerEndAt = new DateTimeOffset(timerEndsAt).ToUnixTimeMilliseconds(),
                ServerTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                GameData = gameData
            };

            //_logger.LogInformation("=== Sending GameResumeData ===");
            //_logger.LogInformation("OpponentName: {OpponentName}", resumeData.OpponentName);
            //_logger.LogInformation("OpponentTotalScore: {OpponentTotalScore}", resumeData.OpponentTotalScore);
            //_logger.LogInformation("YourTotalScore: {YourTotalScore}", resumeData.YourTotalScore);
            //_logger.LogInformation("OpponentCurrentScore: {OpponentCurrentScore}", resumeData.OpponentCurrentScore);
            //_logger.LogInformation("YourCurrentScore: {YourCurrentScore}", resumeData.YourCurrentScore);
            //_logger.LogInformation("CurrentQuestion: {CurrentQuestion}", resumeData.CurrentQuestion);
            //_logger.LogInformation("TimerEndAt: {TimerEndAt} ({TimerEndAtDateTime})",
            //    resumeData.TimerEndAt,
            //    DateTimeOffset.FromUnixTimeMilliseconds(resumeData.TimerEndAt).LocalDateTime);
            //_logger.LogInformation("ServerTime: {ServerTime} ({ServerTimeDateTime})",
            //    resumeData.ServerTime,
            //    DateTimeOffset.FromUnixTimeMilliseconds(resumeData.ServerTime).LocalDateTime);
            //_logger.LogInformation("MatchId: {MatchId}", resumeData.GameData?.MatchId);
            //_logger.LogInformation("TotalQuestions: {TotalQuestions}", resumeData.GameData?.TotalQuestions);
            //_logger.LogInformation("Questions count: {QuestionsCount}", resumeData.GameData?.Questions?.Count);
            //_logger.LogInformation("=============================");

            await _notificationService.NotifyGameResume(userId, resumeData);
        }
        private async Task ForceMatchEnd(Guid userId, Guid matchId, string oldConnectionId)
        {
            try
            {
                await Groups.RemoveFromGroupAsync(oldConnectionId, $"match_{matchId}");

                DraftService.CancelDraftTimer(matchId);
                PvPGameSessionService.CancelMatchTimer(matchId);

                using (var scope = _serviceScopeFactory.CreateScope())
                {
                    var scopedResultService = scope.ServiceProvider.GetRequiredService<IPvPResultService>();
                    await scopedResultService.FinalizeMatchAsync(matchId, GameFinishReason.OpponentDisconnected, userId);
                }

                _activeMatches.TryRemove(matchId, out _);
                _userCurrentMatch.TryRemove(userId, out _);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error forcing match end for user {UserId} in match {MatchId}", userId, matchId);
            }
        }
    }
    public class UserPvPSession
    {
        public string ConnectionId { get; set; }
        public DateTime ConnectedAt { get; set; }
        public bool IsInQueue { get; set; }
        public bool IsInDraft { get; set; }
        public bool IsInMatch { get; set; }
        public Guid? CurrentMatchId { get; set; }
    }
    public class PvPMatchState
    {
        public PvPMatch Match { get; set; } = null!;
        public bool Player1ReadyForDraft { get; set; }
        public bool Player2ReadyForDraft { get; set; }
        public bool Player1ReadyForGame { get; set; }
        public bool Player2ReadyForGame { get; set; }
    }
}
