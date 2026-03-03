using GeoQuiz_backend.Application.Interfaces;
using GeoQuiz_backend.Domain.Entities;
using GeoQuiz_backend.DTOs.KingOfTheHill;
using GeoQuiz_backend.Infrastructure.Data;

namespace GeoQuiz_backend.Application.Services.KingOfTheHill
{
    public class KothMatchmakingService : IKothMatchmakingService
    {
        private readonly AppDbContext _db;
        private readonly ISignalRNotificationService _notificationService;
        private readonly ILogger<KothMatchmakingService> _logger;

        private static readonly Dictionary<Guid, KothLobby> _activeLobbies = new();
        private static readonly object _lobbyLock = new();
        private static readonly Dictionary<Guid, CancellationTokenSource> _lobbyTimers = new();

        public KothMatchmakingService(
            AppDbContext db,
            ISignalRNotificationService notificationService,
            ILogger<KothMatchmakingService> logger)
        {
            _db = db;
            _notificationService = notificationService;
            _logger = logger;
        }

        public async Task<KothLobby?> JoinLobbyAsync(Guid userId)
        {
            KothLobby lobby;

            lock (_lobbyLock)
            {
                var existingLobby = _activeLobbies.Values
                    .FirstOrDefault(l => l.PlayerIds.Contains(userId));

                if (existingLobby != null)
                {
                    _logger.LogWarning("User {UserId} already in lobby {LobbyId}", userId, existingLobby.Id);
                    return existingLobby;
                }

                lobby = _activeLobbies.Values.FirstOrDefault(l => l.PlayerIds.Count < 32);

                if (lobby == null)
                {
                    lobby = new KothLobby
                    {
                        Id = Guid.NewGuid(),
                        PlayerIds = new List<Guid> { userId },
                        CreatedAt = DateTime.UtcNow
                    };
                    _activeLobbies[lobby.Id] = lobby;

                    _logger.LogInformation("Created new lobby {LobbyId} with first player {UserId}", lobby.Id, userId);
                }
                else
                {
                    lobby.PlayerIds.Add(userId);
                    _logger.LogInformation("User {UserId} joined lobby {LobbyId}, now {Count}/32", userId, lobby.Id, lobby.PlayerIds.Count);
                }
            }

            await _notificationService.NotifyPlayerJoined(lobby.Id, new PlayerJoinedData
            {
                LobbyId = lobby.Id,
                PlayerId = userId,
                TotalPlayers = lobby.PlayerIds.Count
            });

            await CheckStartConditionsAsync(lobby.Id);

            return lobby;
        }

        public async Task LeaveLobbyAsync(Guid userId)
        {
            Guid? lobbyId = null;
            bool shouldCancelTimer = false;
            int remainingPlayers = 0;

            lock (_lobbyLock)
            {
                var lobby = _activeLobbies.Values
                    .FirstOrDefault(l => l.PlayerIds.Contains(userId));

                if (lobby == null)
                    return;

                lobbyId = lobby.Id;
                lobby.PlayerIds.Remove(userId);
                remainingPlayers = lobby.PlayerIds.Count;

                _logger.LogInformation("User {UserId} left lobby {LobbyId}, remaining {Count}", userId, lobby.Id, remainingPlayers);

                if (remainingPlayers == 0)
                {
                    _activeLobbies.Remove(lobby.Id);

                    if (_lobbyTimers.TryGetValue(lobby.Id, out var cts))
                    {
                        cts.Cancel();
                        _lobbyTimers.Remove(lobby.Id);
                    }

                    _logger.LogInformation("Lobby {LobbyId} removed (empty)", lobby.Id);
                    return;
                }

                if (remainingPlayers < 4 && _lobbyTimers.ContainsKey(lobby.Id))
                {
                    shouldCancelTimer = true;
                }
            }

            if (lobbyId.HasValue)
            {
                await _notificationService.NotifyPlayerLeft(lobbyId.Value, new PlayerLeftData
                {
                    LobbyId = lobbyId.Value,
                    PlayerId = userId,
                    TotalPlayers = remainingPlayers
                });
            }

            if (shouldCancelTimer && lobbyId.HasValue)
            {
                await CancelLobbyTimerAsync(lobbyId.Value);
            }
        }

        private async Task CheckStartConditionsAsync(Guid lobbyId)
        {
            bool shouldStartTimer = false;

            lock (_lobbyLock)
            {
                if (!_activeLobbies.TryGetValue(lobbyId, out var lobby))
                    return;

                if (lobby.PlayerIds.Count >= 32)
                {
                    _logger.LogInformation("Lobby {LobbyId} full (32 players), starting immediately", lobbyId);
                    // start
                    return;
                }

                if (lobby.PlayerIds.Count >= 4 && !_lobbyTimers.ContainsKey(lobbyId))
                {
                    shouldStartTimer = true;
                }
            }

            if (shouldStartTimer)
            {
                await StartLobbyTimerAsync(lobbyId);
            }
        }

        private async Task StartLobbyTimerAsync(Guid lobbyId)
        {
            const int COUNTDOWN_SECONDS = 15;

            var cts = new CancellationTokenSource();

            lock (_lobbyLock)
            {
                _lobbyTimers[lobbyId] = cts;
            }

            try
            {
                for (int i = COUNTDOWN_SECONDS; i > 0; i--)
                {
                    if (cts.Token.IsCancellationRequested)
                        return;

                    await _notificationService.NotifyLobbyCountdown(lobbyId, i);

                    await Task.Delay(1000, cts.Token);

                    bool stillValid;
                    lock (_lobbyLock)
                    {
                        if (!_activeLobbies.TryGetValue(lobbyId, out var lobby))
                            return;

                        stillValid = lobby.PlayerIds.Count >= 4;
                    }

                    if (!stillValid)
                    {
                        _logger.LogInformation("Lobby {LobbyId} countdown cancelled - less than 4 players", lobbyId);
                        await CancelLobbyTimerAsync(lobbyId);
                        return;
                    }
                }

                _logger.LogInformation("Lobby {LobbyId} countdown finished, starting game", lobbyId);

                lock (_lobbyLock)
                {
                    _lobbyTimers.Remove(lobbyId);
                }

                // start
            }
            catch (TaskCanceledException)
            {
                _logger.LogDebug("Lobby {LobbyId} timer cancelled", lobbyId);
            }
        }

        private async Task CancelLobbyTimerAsync(Guid lobbyId)
        {
            CancellationTokenSource? cts = null;

            lock (_lobbyLock)
            {
                if (_lobbyTimers.TryGetValue(lobbyId, out cts))
                {
                    _lobbyTimers.Remove(lobbyId);
                }
            }

            if (cts != null)
            {
                cts.Cancel();
                await _notificationService.NotifyLobbyCountdownCancelled(lobbyId);
            }
        }

        public Task<bool> IsInLobbyAsync(Guid userId)
        {
            lock (_lobbyLock)
            {
                var inLobby = _activeLobbies.Values
                    .Any(l => l.PlayerIds.Contains(userId));
                return Task.FromResult(inLobby);
            }
        }
    }
}
